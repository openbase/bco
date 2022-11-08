package org.openbase.bco.dal.remote.trigger

import com.google.protobuf.Message
import org.openbase.bco.dal.lib.layer.service.Services
import org.openbase.bco.dal.remote.layer.unit.AbstractUnitRemote
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.type.processing.TimestampProcessor
import org.openbase.type.domotic.service.ServiceTemplateType
import org.openbase.type.domotic.state.ActivationStateType

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
 *
 * @author [Divine Threepwood](mailto:divine@openbase.org)
</STE></DT></UR> */
class GenericServiceStateValueTrigger<UR : AbstractUnitRemote<DT>?, DT : Message?, STE : Enum<STE>>(
    unitRemote: UR,
    targetState: STE,
    serviceType: ServiceTemplateType.ServiceTemplate.ServiceType
) : AbstractBCOTrigger<UR, DT, STE>(unitRemote, targetState, serviceType) {
    override fun verifyCondition(
        data: DT,
        targetState: STE,
        serviceType: ServiceTemplateType.ServiceTemplate.ServiceType?
    ) {
        try {
            val serviceState = Services.invokeProviderServiceMethod(serviceType, data)
            val method = serviceState.javaClass.getMethod("getValue")
            if (method.invoke(serviceState) == targetState) {
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
        } catch (ex: Exception) {
            ExceptionPrinter.printHistory("Could not verify condition $this", ex, LOGGER)
        }
    }
}
