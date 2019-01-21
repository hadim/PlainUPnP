package com.m3sv.plainupnp.di

import android.app.Application
import android.content.Context
import com.m3sv.plainupnp.di.scope.ApplicationScope
import com.m3sv.plainupnp.upnp.DefaultUpnpManager
import dagger.Module
import dagger.Provides
import org.droidupnp.legacy.upnp.Factory


@Module
internal object AppModule {

    @Provides
    @JvmStatic
    fun provideContext(app: Application): Context = app.applicationContext

    @Provides
    @ApplicationScope
    @JvmStatic
    fun provideUPnPManager(context: Context, factory: Factory) =
        DefaultUpnpManager(context, factory.createUpnpServiceController(context), factory)
}