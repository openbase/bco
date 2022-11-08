package org.openbase.bco.dal.remote.trigger

import com.google.protobuf.Message
import org.openbase.bco.dal.lib.layer.service.Services
import org.openbase.bco.dal.remote.layer.unit.AbstractUnitRemote
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.InstantiationException
import org.openbase.jul.exception.NotAvailableException
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.type.processing.TimestampProcessor
import org.openbase.type.domotic.service.ServiceTemplateType
import org.openbase.type.domotic.state.ActivationStateType
import java.lang.reflect.InvocationTargetException

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
 * @param <UR> UnitRemote
 * @param <DT> DataType
 *
 * @author [Timo Michalski](mailto:tmichalski@techfak.uni-bielefeld.de)
</DT></UR> */
class GenericBoundedDoubleValueTrigger<UR : AbstractUnitRemote<DT>?, DT : Message?>(
    unitRemote: UR,
    boundary: Double,
    triggerOperation: TriggerOperation?,
    serviceType: ServiceTemplateType.ServiceTemplate.ServiceType,
    specificValueCall: String
) : AbstractBCOTrigger<UR, DT, Double>(unitRemote, boundary, serviceType) {
    enum class TriggerOperation {
        HIGH_ACTIVE, LOW_ACTIVE
    }

    private var triggerOperation: TriggerOperation
    private var specificValueCall: String

    init {
        try {
            if (triggerOperation == null) {
                throw NotAvailableException("triggerOperation")
            }
            this.triggerOperation = triggerOperation
            this.specificValueCall = specificValueCall
        } catch (ex: CouldNotPerformException) {
            throw InstantiationException(this.javaClass, ex)
        }
    }

    override fun verifyCondition(
        data: DT,
        boundary: Double,
        serviceType: ServiceTemplateType.ServiceTemplate.ServiceType?
    ) {
        try {
            val serviceState: Any = Services.invokeProviderServiceMethod(serviceType, data)
            val method = serviceState.javaClass.getMethod(specificValueCall)
            val value = method.invoke(serviceState) as Double
            if (triggerOperation == TriggerOperation.HIGH_ACTIVE) {
                if (value >= boundary) {
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
            } else {
                if (value <= boundary) {
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
            }
        } catch (ex: CouldNotPerformException) {
            ExceptionPrinter.printHistory("Could not verify condition $this", ex, LOGGER)
        } catch (ex: NoSuchMethodException) {
            ExceptionPrinter.printHistory("Method not known $this", ex, LOGGER)
        } catch (ex: SecurityException) {
            ExceptionPrinter.printHistory("Security Exception $this", ex, LOGGER)
        } catch (ex: IllegalAccessException) {
            ExceptionPrinter.printHistory("Illegal Access Exception $this", ex, LOGGER)
        } catch (ex: IllegalArgumentException) {
            ExceptionPrinter.printHistory("Illegal Argument Exception $this", ex, LOGGER)
        } catch (ex: InvocationTargetException) {
            ExceptionPrinter.printHistory("Could not invoke method $this", ex, LOGGER)
        }
    }
}
