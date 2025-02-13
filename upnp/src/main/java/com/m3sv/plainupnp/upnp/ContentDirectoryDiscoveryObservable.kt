package com.m3sv.plainupnp.upnp

import com.m3sv.plainupnp.data.upnp.DeviceDisplay
import com.m3sv.plainupnp.data.upnp.DeviceType
import com.m3sv.plainupnp.data.upnp.UpnpDeviceEvent
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import javax.inject.Inject


class ContentDirectoryDiscoveryObservable @Inject constructor(private val controller: UpnpServiceController) :
    Observable<List<DeviceDisplay>>() {

    private val contentDirectories = LinkedHashSet<DeviceDisplay>()

    fun currentContentDirectories() = contentDirectories.toList()

    override fun subscribeActual(observer: Observer<in List<DeviceDisplay>>) {
        val deviceObserver = ContentDeviceObserver(controller.contentDirectoryDiscovery, observer)
        observer.onSubscribe(deviceObserver)
    }

    private fun handleEvent(event: UpnpDeviceEvent) {
        when (event) {
            is UpnpDeviceEvent.Added -> {
                contentDirectories += DeviceDisplay(
                    event.upnpDevice,
                    false,
                    DeviceType.CONTENT_DIRECTORY
                )
            }

            is UpnpDeviceEvent.Removed -> {
                val device = DeviceDisplay(
                    event.upnpDevice,
                    false,
                    DeviceType.CONTENT_DIRECTORY
                )

                if (contentDirectories.contains(device))
                    contentDirectories -= device
            }
        }

    }

    private inner class ContentDeviceObserver(
        private val contentDirectoryDiscovery: DeviceDiscovery,
        private val observer: Observer<in List<DeviceDisplay>>
    ) : Disposable, DeviceDiscoveryObserver {

        init {
            contentDirectoryDiscovery.addObserver(this)
        }

        override fun isDisposed(): Boolean = !contentDirectoryDiscovery.hasObserver(this)

        override fun dispose() {
            contentDirectoryDiscovery.removeObserver(this)
        }

        override fun addedDevice(event: UpnpDeviceEvent) {
            handleEvent(event)
            if (!isDisposed)
                observer.onNext(contentDirectories.toList())
        }

        override fun removedDevice(event: UpnpDeviceEvent) {
            handleEvent(event)

            if (!isDisposed)
                observer.onNext(contentDirectories.toList())
        }
    }
}