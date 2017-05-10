package org.openbase.bco.dal.lib.simulation.service;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.lang.reflect.InvocationTargetException;
import javafx.util.Pair;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Factory;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * This service simulator factory tries to detect if any custom service simulator for the given service. If no custom implementation exists the generic unit simulator is used for the simulation.
 */
public class ServiceSimulatorFactory implements Factory<AbstractServiceSimulator, Pair<UnitController, ServiceType>> {

    protected static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ServiceSimulatorFactory.class);

    /**
     * Instantiate a new service simulator compatible to the given config pair.
     *
     * @return the simulator
     * @throws InstantiationException is thrown if any error occurs during the instantiation.
     * @throws InterruptedException is thrown if the current thread was externally interrupted.
     */
    public AbstractServiceSimulator newInstance(final UnitController unitController, final ServiceType serviceType) throws InstantiationException, InterruptedException {
        return newInstance(new Pair<>(unitController, serviceType));
    }

    /**
     * Instantiate a new service simulator compatible to the given config pair.
     *
     * @param configPair contains the simulation target and the type of service to simulate.
     * @return the simulator
     * @throws InstantiationException is thrown if any error occurs during the instantiation.
     * @throws InterruptedException is thrown if the current thread was externally interrupted.
     */
    @Override
    public AbstractServiceSimulator newInstance(final Pair<UnitController, ServiceType> configPair) throws InstantiationException, InterruptedException {
        try {
            // try to return costum service simulator
            final String serviceSimulatorClassName = AbstractServiceSimulator.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToCamelCase(configPair.getValue().name()) + "Simulator";
            return ((Class<? extends AbstractServiceSimulator>) Class.forName(serviceSimulatorClassName)).getConstructor(UnitController.class).newInstance(configPair.getKey());
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | java.lang.InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            ExceptionPrinter.printHistory("Could not find custom service simulator for " + configPair.getValue().getClass().getName() + "[" + configPair.getValue().name() + "]", ex, LOGGER, LogLevel.DEBUG);
        }

        // return generic service simulator
        return new GenericServiceSimulator(configPair.getKey(), configPair.getValue());
    }
}
