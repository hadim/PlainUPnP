package com.m3sv.plainupnp.upnp.cleanslate

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.m3sv.plainupnp.ContentCache
import com.m3sv.plainupnp.data.upnp.UpnpDevice
import com.m3sv.plainupnp.upnp.*
import com.m3sv.plainupnp.upnp.filters.CallableFilter
import com.m3sv.plainupnp.upnp.resourceproviders.LocalServiceResourceProvider
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.model.message.header.STAllHeader
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class UpnpServiceListener @Inject constructor(
    private val context: Context,
    private val mediaServer: MediaServer,
    private val contentCache: ContentCache
) {

    var upnpService: AndroidUpnpService? = null
        private set

    private val waitingListener: MutableList<RegistryListener> = mutableListOf()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mediaServer.start()
            upnpService = (service as AndroidUpnpService).also { upnpService ->
                upnpService.controlPoint.search()
                upnpService.registry
                    .addDevice(
                        LocalUpnpDevice(
                            LocalServiceResourceProvider(
                                context
                            ),
                            LocalService(
                                context,
                                getLocalIpAddress(context),
                                contentCache
                            )
                        )()
                    )
            }
            waitingListener.map { addListenerSafe(it) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            upnpService = null
            mediaServer.stop()
        }
    }

    fun bindService() {
        context.bindService(
            Intent(
                context,
                PlainUpnpAndroidService::class.java
            ),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun unbindService() {
        context.unbindService(serviceConnection)
    }

    fun getFilteredDeviceList(filter: CallableFilter): Collection<UpnpDevice> {
        val deviceList = ArrayList<UpnpDevice>()
        try {
            upnpService?.registry?.devices?.forEach {
                val device = CDevice(it)
                filter.device = device

                if (filter.call()) deviceList.add(device)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }

        return deviceList
    }

    fun addListener(registryListener: RegistryListener) {
        if (upnpService != null)
            addListenerSafe(registryListener)
        else
            waitingListener.add(registryListener)
    }

    private fun addListenerSafe(registryListener: RegistryListener) {
        upnpService?.registry?.run {
            // Get ready for future device advertisements
            addListener(CRegistryListener(registryListener))

            // Now add all devices to the list we already know about
            devices?.forEach {
                registryListener.deviceAdded(CDevice(it))
            }
        }
    }

    fun removeListener(registryListener: RegistryListener) {
        if (upnpService != null)
            removeListenerSafe(registryListener)
        else
            waitingListener.remove(registryListener)
    }

    private fun removeListenerSafe(registryListener: RegistryListener) {
        assert(upnpService != null)
        upnpService?.registry?.removeListener(CRegistryListener(registryListener))
    }

    fun clearListener() {
        waitingListener.clear()
    }

    fun refresh() {
        upnpService?.controlPoint?.search(STAllHeader())
    }
}