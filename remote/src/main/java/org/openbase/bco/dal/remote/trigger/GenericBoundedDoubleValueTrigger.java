package org.openbase.bco.dal.remote.trigger;

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
 */

import com.google.protobuf.Message;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.openbase.bco.dal.remote.layer.unit.AbstractUnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.controller.Remote;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.type.domotic.state.ConnectionStateType;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.jul.pattern.trigger.AbstractTrigger;
import org.slf4j.Logger;

/**
 * @param <UR> UnitRemote
 * @param <DT> DataType
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class GenericBoundedDoubleValueTrigger<UR extends AbstractUnitRemote<DT>, DT extends Message> extends AbstractBCOTrigger<UR, DT, Double> {

    public enum TriggerOperation {
        HIGH_ACTIVE, LOW_ACTIVE
    }

    private final TriggerOperation triggerOperation;
    private final String specificValueCall;

    public GenericBoundedDoubleValueTrigger(final UR unitRemote, final double boundary, final TriggerOperation triggerOperation, ServiceType serviceType, String specificValueCall) throws InstantiationException {
        super(unitRemote, boundary, serviceType);

        try {

            if (triggerOperation == null) {
                throw new NotAvailableException("triggerOperation");
            }

            this.triggerOperation = triggerOperation;
            this.specificValueCall = specificValueCall;

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this.getClass(), ex);
        }
    }

    protected void verifyCondition(final DT data, final Double boundary, final ServiceType serviceType) {
        try {
            Object serviceState = Services.invokeProviderServiceMethod(serviceType, data);

            Method method = serviceState.getClass().getMethod(specificValueCall);
            double value = (Double) method.invoke(serviceState);

            if (triggerOperation == TriggerOperation.HIGH_ACTIVE) {
                if (value >= boundary) {
                    notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()));
                } else {
                    notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.INACTIVE).build()));
                }
            } else {
                if (value <= boundary) {
                    notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()));
                } else {
                    notifyChange(TimestampProcessor.updateTimestampWithCurrentTime(ActivationState.newBuilder().setValue(ActivationState.State.INACTIVE).build()));
                }
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not verify condition " + this, ex, LOGGER);
        } catch (NoSuchMethodException ex) {
            ExceptionPrinter.printHistory("Method not known " + this, ex, LOGGER);
        } catch (SecurityException ex) {
            ExceptionPrinter.printHistory("Security Exception " + this, ex, LOGGER);
        } catch (IllegalAccessException ex) {
            ExceptionPrinter.printHistory("Illegal Access Exception " + this, ex, LOGGER);
        } catch (IllegalArgumentException ex) {
            ExceptionPrinter.printHistory("Illegal Argument Exception " + this, ex, LOGGER);
        } catch (InvocationTargetException ex) {
            ExceptionPrinter.printHistory("Could not invoke method " + this, ex, LOGGER);
        }
    }
}
