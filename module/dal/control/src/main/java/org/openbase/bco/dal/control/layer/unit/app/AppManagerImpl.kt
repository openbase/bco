package org.openbase.bco.dal.control.layer.unit.app

import org.openbase.bco.dal.control.layer.unit.UnitControllerRegistrySynchronizer
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactory
import org.openbase.bco.dal.lib.layer.service.UnitDataSourceFactory
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistryImpl
import org.openbase.bco.dal.lib.layer.unit.app.AppController
import org.openbase.bco.dal.lib.layer.unit.app.AppControllerFactory
import org.openbase.bco.dal.lib.layer.unit.app.AppManager
import org.openbase.bco.registry.remote.Registries
import org.openbase.bco.registry.remote.login.BCOLogin
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.InstantiationException
import org.openbase.jul.exception.NotAvailableException
import org.openbase.jul.exception.NotSupportedException
import org.openbase.jul.iface.Launchable
import org.openbase.jul.iface.VoidInitializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *
 * @author [Tamino Huxohl](mailto:pleminoq@openbase.org)
 */
class AppManagerImpl : AppManager, Launchable<Void>, VoidInitializable {
    private var factory: AppControllerFactory? = null
    private var appControllerRegistry: UnitControllerRegistry<AppController>
    private var appRegistrySynchronizer: UnitControllerRegistrySynchronizer<AppController>

    init {
        try {
            this.factory = AppControllerFactoryImpl.instance
            this.appControllerRegistry = UnitControllerRegistryImpl()
            this.appRegistrySynchronizer = UnitControllerRegistrySynchronizer(
                appControllerRegistry,
                Registries.getUnitRegistry().getAppUnitConfigRemoteRegistry(false),
                factory
            )
        } catch (ex: CouldNotPerformException) {
            throw InstantiationException(this, ex)
        }
    }

    override fun init() {
        // this has to stay, else do not implement VoidInitializable
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun activate() {
        BCOLogin.getSession().loginBCOUser()
        appControllerRegistry.activate()
        appRegistrySynchronizer.activate()
    }

    override fun isActive(): Boolean {
        return appRegistrySynchronizer.isActive &&
                appControllerRegistry.isActive
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun deactivate() {
        appRegistrySynchronizer.deactivate()
        appControllerRegistry.deactivate()
    }

    override fun shutdown() {
        appRegistrySynchronizer.shutdown()
        appControllerRegistry.shutdown()
    }

    @Throws(NotAvailableException::class)
    override fun getOperationServiceFactory(): OperationServiceFactory {
        throw NotAvailableException("OperationServiceFactory", NotSupportedException("OperationServiceFactory", this))
    }

    @Throws(NotAvailableException::class)
    override fun getUnitDataSourceFactory(): UnitDataSourceFactory {
        throw NotAvailableException("UnitDataSourceFactory", NotSupportedException("UnitDataSourceFactory", this))
    }

    override fun getAppControllerRegistry(): UnitControllerRegistry<AppController> {
        return appControllerRegistry
    }

    companion object {
        protected val LOGGER: Logger = LoggerFactory.getLogger(AppManagerImpl::class.java)
    }
}
