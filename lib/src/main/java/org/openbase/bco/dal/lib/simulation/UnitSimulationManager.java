package org.openbase.bco.dal.lib.simulation;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import com.google.protobuf.GeneratedMessage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openbase.bco.dal.lib.jp.JPBenchmarkMode;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.dal.lib.simulation.unit.AbstractUnitSimulator;
import org.openbase.bco.dal.lib.simulation.unit.GenericUnitSimulator;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.LoggerFactory;
import org.openbase.jul.exception.InstantiationException;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * This class generates unit simulators for each unit controller the given unit controller registry provides.
 */
public class UnitSimulationManager<CONTROLLER extends UnitController<?,?>> implements Manageable<UnitControllerRegistry<CONTROLLER>> {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UnitSimulationManager.class);

    private final ShutdownDaemon shutdownDaemon;

    private final Object UNIT_SIMULATOR_MONITOR = new SyncObject("UnitSimulatorMonitor");

    private boolean enabled = false;
    private boolean active = false;
    private final Map<String, AbstractUnitSimulator> unitSimulatorMap;

    /**
     * Creates a new unit simulator manager.
     *
     * @throws org.openbase.jul.exception.InstantiationException is thrown in case the instance could not be instantiated.
     */
    public UnitSimulationManager() throws InstantiationException {
        try {
            this.unitSimulatorMap = new HashMap<>();
            this.shutdownDaemon = Shutdownable.registerShutdownHook(this);

            try {
                enabled = JPService.getProperty(JPHardwareSimulationMode.class).getValue() || JPService.getProperty(JPBenchmarkMode.class).getValue();
            } catch (JPNotAvailableException ex) {
                ExceptionPrinter.printHistory("Could not detect hardware simulation/benchmark mode!", ex, LOGGER);
            }
        } catch (final CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Method initializes the unit manager with the given unit controller registry.
     *
     * @param unitControllerRegistry the unit controller registry to quire the unit controller to simulate.
     * @throws InterruptedException is thrown if the current thread is externally interrupted.
     */
    @Override
    public void init(final UnitControllerRegistry<CONTROLLER> unitControllerRegistry) throws InterruptedException {
        if (!enabled) {
            return;
        }

        try {
            Registries.waitForData();
            unitControllerRegistry.addObserver((source, data) -> {
                updateUnitSimulators(data.values());
            });
            updateUnitSimulators(unitControllerRegistry.getValue().values());
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(new InitializationException(this, ex), LOGGER);
        }
    }

    private void updateUnitSimulators(final Collection<CONTROLLER> unitControllerList) throws InterruptedException {
        synchronized (UNIT_SIMULATOR_MONITOR) {
            final List<String> previousUnitKeyList = new ArrayList<>(unitSimulatorMap.keySet());
            for (CONTROLLER unitController : unitControllerList) {
                try {

                    previousUnitKeyList.remove(unitController.getId());

                    // filter already registered controller
                    if (unitSimulatorMap.containsKey(unitController.getId())) {
                        continue;
                    }

                    // register new controller
                    GenericUnitSimulator genericUnitSimulator = new GenericUnitSimulator(unitController);
                    unitSimulatorMap.put(unitController.getId(), genericUnitSimulator);
                    if (isActive()) {
                        genericUnitSimulator.activate();
                    }

                } catch (CouldNotPerformException | NullPointerException ex) {
                    ExceptionPrinter.printHistory("Could not handle " + unitController + " update!", ex, LOGGER);
                }
            }
            // remove outdated controller
            for (String unitId : previousUnitKeyList) {
                try {
                    unitSimulatorMap.remove(unitId).deactivate();
                } catch (CouldNotPerformException | NullPointerException ex) {
                    ExceptionPrinter.printHistory("Could not deactivate simulation of Unit[" + unitId + "]", ex, LOGGER);
                }
            }
        }
    }

    /**
     * Method activates the unit simulation but only if the {@code JPHardwareSimulationMode} is activated.
     *
     * @throws CouldNotPerformException is thrown if the simulation could not be started.
     * @throws InterruptedException is thrown if the current thread is externally interrupted.
     */
    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        if (!enabled) {
            return;
        }
        active = true;
        synchronized (UNIT_SIMULATOR_MONITOR) {
            for (AbstractUnitSimulator simulator : unitSimulatorMap.values()) {
                try {
                    simulator.activate();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not activate " + simulator, ex, LOGGER);
                }
            }
        }
    }

    /**
     * Method deactivates the unit simulation.
     *
     * @throws CouldNotPerformException is thrown if the simulation could not be stopped.
     * @throws InterruptedException is thrown if the current thread is externally interrupted.
     */
    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;
        synchronized (UNIT_SIMULATOR_MONITOR) {
            for (AbstractUnitSimulator simulator : unitSimulatorMap.values()) {
                try {
                    simulator.deactivate();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not activate " + simulator, ex, LOGGER);
                }
            }
        }
    }

    /**
     * Returns if the simulation is started.
     *
     * @return true if the simulation is started.
     */
    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void shutdown() {
        try {
            deactivate();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not shutdown " + this, ex, LoggerFactory.getLogger(getClass()));
        }
        shutdownDaemon.cancel();
    }
}
