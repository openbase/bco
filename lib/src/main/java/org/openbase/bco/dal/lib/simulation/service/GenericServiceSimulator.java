package org.openbase.bco.dal.lib.simulation.service;

/*-
 * #%L
 * BCO DAL Library
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
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.bco.dal.lib.layer.service.Services;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * <p>
 * This generic simulator detects the available service values the service provides any tries to apply those states in random order.
 */
public class GenericServiceSimulator extends AbstractRandomServiceSimulator<Message> {

    /**
     * Creates a new service simulator to control the given unit.
     *
     * @param unitController the controller to simulate.
     * @param serviceType    the service type to simulate.
     *
     * @throws InstantiationException is thrown in case the current thread was externally interrupted.
     */
    public GenericServiceSimulator(final UnitController unitController, final ServiceType serviceType) throws InstantiationException {
        super(unitController, serviceType);
        try {
            detectAndRegisterServiceStates(serviceType);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Method detects the service states of the given service type.
     *
     * @param serviceType the type of the service states.
     *
     * @throws CouldNotPerformException is thrown if the detection fails.
     */
    private void detectAndRegisterServiceStates(final ServiceType serviceType) throws CouldNotPerformException {
        try {
            final MultiException.ExceptionStack exceptionStack = new MultiException.ExceptionStack();
            Services.getServiceStateEnumValues(serviceType).forEach((stateValue) -> {
                try {
                    // filter unknown state values
                    if (stateValue.getValueDescriptor().getName().equals("UNKNOWN")) {
                        return;
                    }

                    // add built service state
                    registerServiceState(Services.buildServiceState(serviceType, stateValue));
                } catch (final Exception ex) {
                    MultiException.push(this, ex, exceptionStack);
                }
            });
            MultiException.checkAndThrow(() -> "Could not generate all service values!", exceptionStack);
        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory(serviceType.name() + " does not provide any service states.", ex, LOGGER, LogLevel.DEBUG);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate service states!", ex);
        }
    }
}
