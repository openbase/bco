package org.openbase.bco.dal.remote.trigger;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
 */

import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.remote.layer.unit.AbstractUnitRemote;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;

import java.lang.reflect.Method;

/**
 * @param <UR>  UnitRemote
 * @param <DT>  DataType
 * @param <STE> StateTypeEnum
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class GenericServiceStateValueTrigger<UR extends AbstractUnitRemote<DT>, DT extends Message, STE extends Enum<STE>> extends AbstractBCOTrigger<UR, DT, STE> {

    public GenericServiceStateValueTrigger(final UR unitRemote, final STE targetState, final ServiceType serviceType) throws InstantiationException, InstantiationException {
        super(unitRemote, targetState, serviceType);
    }

    @Override
    protected void verifyCondition(DT data, STE targetState, ServiceType serviceType) {
        try {
            Message serviceState = Services.invokeProviderServiceMethod(serviceType, data);

            Method method = serviceState.getClass().getMethod("getValue");
            if (method.invoke(serviceState).equals(targetState)) {
                notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()));
            } else {
                notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.INACTIVE).build()));
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory("Could not verify condition " + this, ex, LOGGER);
        }
    }
}
