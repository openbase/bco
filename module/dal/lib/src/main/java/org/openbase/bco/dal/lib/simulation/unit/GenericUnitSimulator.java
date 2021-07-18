package org.openbase.bco.dal.lib.simulation.unit;

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

import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.simulation.service.AbstractServiceSimulator;
import org.openbase.bco.dal.lib.simulation.service.ServiceSimulatorFactory;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * <p>
 * This class implements and generic unit simulator by extending the {@code AbstractUnitSimulator} and simulating each service of the unit by custom or generic service simulators.
 */
public class GenericUnitSimulator extends AbstractUnitSimulator {

    private static final ServiceSimulatorFactory serviceSimulatorFactory = new ServiceSimulatorFactory();
    private final List<AbstractServiceSimulator> serviceSimulatorList;

    /**
     * Constructor creates a new generic unit simulator.
     *
     * @param unitController this controller must be linked to the unit which should be simulated.
     *
     * @throws org.openbase.jul.exception.InstantiationException is thrown if any error occurs during class instantiation.
     */
    public GenericUnitSimulator(final UnitController unitController) throws InstantiationException, InterruptedException {
        super(unitController);
        try {
            this.serviceSimulatorList = new ArrayList<>();

            initServiceSimulators(unitController);
        } catch (InitializationException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private void initServiceSimulators(final UnitController unitController) throws InitializationException, InterruptedException {
        try {
            final MultiException.ExceptionStack exceptionStack = new MultiException.ExceptionStack();
            for (final ServiceDescription serviceDescription : unitController.getUnitTemplate().getServiceDescriptionList()) {
                if (serviceDescription.getPattern() != ServicePattern.PROVIDER) {
                    continue;
                }

                // filter unknown services type
                if (serviceDescription.getServiceType() == ServiceType.UNKNOWN) {
                    LOGGER.warn("Template of " + unitController + " contains an unknown service description: " + serviceDescription);
                    continue;
                }

                // filter aggregated services because those are implicitly simulated by the observed units.
                if (serviceDescription.getAggregated()) {
                    continue;
                }

                try {
                    serviceSimulatorList.add(serviceSimulatorFactory.newInstance(unitController, serviceDescription.getServiceType()));
                } catch (final CouldNotPerformException ex) {
                    MultiException.push(this, ex, exceptionStack);
                }
            }
            try {
                MultiException.checkAndThrow(() -> "Could not init all service simulators of " + unitController + "!", exceptionStack);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, LOGGER);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * Method registers service simulators for all services the simulated unit provides.
     *
     * @return a collection of futures referring to the simulation tasks.
     */
    @Override
    protected Collection<Future> executeSimulationTasks() {
        final List<Future> futureList = new ArrayList<>();
        serviceSimulatorList.forEach((simulator) -> {
            futureList.addAll(simulator.executeSimulationTasks());
        });
        return futureList;
    }
}
