package org.openbase.bco.dal.control.layer.unit.app

import org.openbase.bco.dal.control.layer.unit.AbstractAuthorizedBaseUnitController
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactory
import org.openbase.bco.dal.lib.layer.service.UnitDataSourceFactory
import org.openbase.bco.dal.lib.layer.service.mock.OperationServiceFactoryMock
import org.openbase.bco.dal.lib.layer.unit.Unit
import org.openbase.bco.dal.lib.layer.unit.UnitController
import org.openbase.bco.dal.lib.layer.unit.app.AppController
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.NotAvailableException
import org.openbase.jul.iface.Activatable
import org.openbase.type.domotic.action.ActionParameterType
import org.openbase.type.domotic.unit.UnitConfigType
import org.openbase.type.domotic.unit.app.AppDataType.AppData
import java.util.*

/**
 * * @author [Tamino Huxohl](mailto:pleminoq@openbase.org)
 */
abstract class AbstractAppController(
    private val operationServiceFactory: OperationServiceFactory = OperationServiceFactoryMock.getInstance(),
    private val unitDataSourceFactory: UnitDataSourceFactory = object : UnitDataSourceFactory {
        @Throws(InstantiationException::class)
        override fun <UNIT : Unit<*>> newInstance(unit: UNIT): Activatable {
            throw org.openbase.jul.exception.InstantiationException(
                Unit::class.java,
                UnsupportedOperationException("Not supported yet.")
            )
        }
    },
) : AbstractAuthorizedBaseUnitController<AppData, AppData.Builder>(AppData.newBuilder()), AppController {

    @Throws(NotAvailableException::class)
    override fun getOperationServiceFactory(): OperationServiceFactory? = operationServiceFactory

    @Throws(NotAvailableException::class)
    override fun getUnitDataSourceFactory(): UnitDataSourceFactory? = unitDataSourceFactory

    @Throws(InterruptedException::class, CouldNotPerformException::class)
    override fun getActionParameterTemplate(config: UnitConfigType.UnitConfig): ActionParameterType.ActionParameter.Builder {
        val appClass = Registries.getClassRegistry(true).getAppClassById(config.appConfig.appClassId)
        return ActionParameterType.ActionParameter.newBuilder()
            .addAllCategory(appClass.categoryList)
            .setPriority(appClass.priority)
    }

    override fun isAutostartEnabled(config: UnitConfigType.UnitConfig?): Boolean =
        config?.appConfig?.autostart ?: false

    // Method can be overwritten in case this app introduces further units.
    override fun getHostedUnitControllerList(): List<UnitController<*, *>?> = emptyList()

    @Throws(NotAvailableException::class)
    override fun getHostedUnitController(id: String): UnitController<*, *> {
        // Method can be overwritten in case this app introduces further units.
        throw NotAvailableException("UnitController", id)
    }

    // Method can be overwritten in case this app introduces further units.
    override fun getHostedUnitConfigList(): List<UnitConfigType.UnitConfig> = emptyList()
}
