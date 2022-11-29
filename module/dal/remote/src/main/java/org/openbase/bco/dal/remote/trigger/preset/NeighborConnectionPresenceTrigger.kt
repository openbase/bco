package org.openbase.bco.dal.remote.trigger.preset

import org.openbase.bco.dal.remote.layer.unit.connection.ConnectionRemote
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.type.processing.TimestampProcessor
import org.openbase.jul.pattern.Observer
import org.openbase.jul.pattern.controller.Remote
import org.openbase.jul.pattern.provider.DataProvider
import org.openbase.jul.pattern.trigger.AbstractTrigger
import org.openbase.type.domotic.state.*
import org.openbase.type.domotic.unit.connection.ConnectionDataType
import org.openbase.type.domotic.unit.location.LocationDataType
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
 *
 * @author [Timo Michalski](mailto:tmichalski@techfak.uni-bielefeld.de)
 */
class NeighborConnectionPresenceTrigger(
    private val locationRemote: LocationRemote,
    private val connectionRemote: ConnectionRemote
) : AbstractTrigger() {
    private val locationObserver: Observer<DataProvider<LocationDataType.LocationData>, LocationDataType.LocationData>
    private val connectionObserver: Observer<DataProvider<ConnectionDataType.ConnectionData>, ConnectionDataType.ConnectionData>
    private val connectionStateObserver: Observer<Remote<*>, ConnectionStateType.ConnectionState.State>
    private var active = false

    init {
        locationObserver = Observer { _, _ -> verifyCondition() }
        connectionObserver = Observer {  _, _ -> verifyCondition() }
        connectionStateObserver = Observer { _, data: ConnectionStateType.ConnectionState.State ->
            if (data == ConnectionStateType.ConnectionState.State.CONNECTED) {
                verifyCondition()
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

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun activate() {
        locationRemote.addDataObserver(locationObserver)
        connectionRemote.addDataObserver(connectionObserver)
        locationRemote.addConnectionStateObserver(connectionStateObserver)
        connectionRemote.addConnectionStateObserver(connectionStateObserver)
        active = true
        verifyCondition()
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun deactivate() {
        locationRemote.removeDataObserver(locationObserver)
        connectionRemote.removeDataObserver(connectionObserver)
        locationRemote.removeConnectionStateObserver(connectionStateObserver)
        connectionRemote.removeConnectionStateObserver(connectionStateObserver)
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
            ExceptionPrinter.printHistory("Could not shutdown $this", ex, LoggerFactory.getLogger(javaClass))
        }
        super.shutdown()
    }

    private fun verifyCondition() {
        try {
            if (locationRemote.data.presenceState.value == PresenceStateType.PresenceState.State.PRESENT && connectionRemote.doorState.value == DoorStateType.DoorState.State.OPEN || connectionRemote.windowState.value == WindowStateType.WindowState.State.OPEN) {
                notifyChange(
                    TimestampProcessor.updateTimestampWithCurrentTime(
                        ActivationStateType.ActivationState.newBuilder().setValue(
                            ActivationStateType.ActivationState.State.ACTIVE
                        ).build()
                    )
                )
            } else {
                notifyChange(
                    TimestampProcessor.updateTimestampWithCurrentTime(
                        ActivationStateType.ActivationState.newBuilder().setValue(
                            ActivationStateType.ActivationState.State.INACTIVE
                        ).build()
                    )
                )
            }
        } catch (ex: CouldNotPerformException) {
            ExceptionPrinter.printHistory(
                "Could not verify trigger state $this",
                ex,
                LoggerFactory.getLogger(javaClass)
            )
        }
    }
}
