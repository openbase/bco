package org.openbase.bco.dal.remote.trigger

import com.google.protobuf.Message
import org.openbase.bco.dal.remote.layer.unit.AbstractUnitRemote
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.type.processing.TimestampProcessor
import org.openbase.jul.pattern.Observer
import org.openbase.jul.pattern.controller.Remote
import org.openbase.jul.pattern.provider.DataProvider
import org.openbase.jul.pattern.trigger.AbstractTrigger
import org.openbase.type.domotic.service.ServiceTemplateType
import org.openbase.type.domotic.state.ActivationStateType
import org.openbase.type.domotic.state.ConnectionStateType
import org.slf4j.LoggerFactory

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */ /**
 * @param <UR>  UnitRemote
 * @param <DT>  DataType
 * @param <STE> StateTypeEnum
 * @author [Timo Michalski](mailto:tmichalski@techfak.uni-bielefeld.de)
</STE></DT></UR> */
abstract class AbstractBCOTrigger<UR : AbstractUnitRemote<DT>, DT : Message, STE>(
    private val unitRemote: UR,
    private val targetState: STE,
    private val serviceType: ServiceTemplateType.ServiceTemplate.ServiceType
) : AbstractTrigger() {
    protected val LOGGER = LoggerFactory.getLogger(javaClass)
    private val dataObserver: Observer<DataProvider<DT>, DT>
    private val connectionObserver: Observer<Remote<*>, ConnectionStateType.ConnectionState.State>
    private var active = false

    init {
        dataObserver =
            Observer { source: DataProvider<DT>, data: DT -> verifyCondition(data, targetState, serviceType) }
        connectionObserver = Observer { source: Remote<*>?, data: ConnectionStateType.ConnectionState.State ->
            if (data == ConnectionStateType.ConnectionState.State.CONNECTED) {
                verifyCondition(unitRemote.data, targetState, serviceType)
            } else {
                notifyChange(
                    TimestampProcessor.updateTimestampWithCurrentTime(
                        ActivationStateType.ActivationState.newBuilder().setValue(
                            ActivationStateType.ActivationState.State.UNKNOWN
                        ).build()
                    )
                )
            }
        }
    }

    protected abstract fun verifyCondition(
        data: DT,
        targetState: STE,
        serviceType: ServiceTemplateType.ServiceTemplate.ServiceType?
    )

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun activate() {
        StateObservationService.registerTrigger(dataObserver, unitRemote, this)
        unitRemote.addConnectionStateObserver(connectionObserver)
        active = true
        if (unitRemote.isDataAvailable) {
            verifyCondition(unitRemote.data, targetState, serviceType)
        }
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun deactivate() {
        StateObservationService.removeTrigger(dataObserver, unitRemote, this)
        unitRemote.removeConnectionStateObserver(connectionObserver)
        active = false
        notifyChange(
            TimestampProcessor.updateTimestampWithCurrentTime(
                ActivationStateType.ActivationState.newBuilder().setValue(
                    ActivationStateType.ActivationState.State.UNKNOWN
                ).build()
            )
        )
    }

    override fun isActive(): Boolean {
        return active
    }

    override fun shutdown() {
        try {
            deactivate()
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (ex: CouldNotPerformException) {
            ExceptionPrinter.printHistory("Could not shutdown $this", ex, LOGGER)
        }
        super.shutdown()
    }
}
