package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
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

import org.openbase.bco.dal.lib.jp.JPBenchmarkMode;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.simulation.UnitSimulationManager;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.iface.Activatable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.storage.registry.ControllerRegistryImpl;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @param <CONTROLLER> the type of unit controller.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitControllerRegistryImpl<CONTROLLER extends UnitController<?, ?>> extends ControllerRegistryImpl<String, CONTROLLER> implements UnitControllerRegistry<CONTROLLER>, Activatable {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UnitControllerRegistryImpl.class);

    private final Map<String, CONTROLLER> scopeControllerMap;
    private final UnitSimulationManager<CONTROLLER> unitSimulationManager;
    private boolean active;

    public UnitControllerRegistryImpl() throws InstantiationException {
        this.scopeControllerMap = new HashMap<>();
        addObserver(new UnitControllerSynchronizer());

        // handle simulation mode
        this.unitSimulationManager = initSimulationManager();
    }

    public UnitControllerRegistryImpl(final HashMap<String, CONTROLLER> entryMap) throws InstantiationException {
        super(entryMap);
        this.scopeControllerMap = new HashMap<>();
        addObserver(new UnitControllerSynchronizer());

        // handle simulation mode
        this.unitSimulationManager = initSimulationManager();
    }

    /**
     * Method builds and inits the simulation mode.
     *
     * @return a UnitSimulationManager or null if the simulation mode is not enabled.
     */
    private UnitSimulationManager<CONTROLLER> initSimulationManager() {
        try {
            if (JPService.getProperty(JPHardwareSimulationMode.class).getValue() || JPService.getProperty(JPBenchmarkMode.class).getValue()) {
                final UnitSimulationManager<CONTROLLER> simulationManager = new UnitSimulationManager<>();
                simulationManager.init(this);
                return simulationManager;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not start simulation/benchmark mode!", ex, LOGGER);
        } catch (JPNotAvailableException ex) {
            ExceptionPrinter.printHistory("Could not detect simulation/benchmark mode!", ex, LOGGER);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    public CONTROLLER getUnitByScope(final String scope) throws NotAvailableException {
        final CONTROLLER controller = scopeControllerMap.get(scope);
        if (controller == null) {
            throw new NotAvailableException("UnitController", new InvalidStateException("No unit controller for given scope registered!"));
        }
        return controller;
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {

        // required to guarantee permission check is possible when controllers are started.
        Registries.getUnitRegistry().waitForData();

        active = true;
        if(unitSimulationManager != null) {
            unitSimulationManager.activate();
        }
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        if(unitSimulationManager != null) {
            unitSimulationManager.deactivate();
        }
        clear();
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * Class to synchronize the scope controller map with the unit controller registry.
     */
    private class UnitControllerSynchronizer implements Observer<DataProvider<Map<String, CONTROLLER>>, Map<String, CONTROLLER>> {

        @Override
        public void update(final DataProvider<Map<String, CONTROLLER>> source, final Map<String, CONTROLLER> data) throws Exception {

            final Collection<CONTROLLER> unitControllerCollection = new ArrayList<>(data.values());
            // add new entries to the scope controller map
            for (final CONTROLLER controller : unitControllerCollection) {
                scopeControllerMap.put(ScopeProcessor.generateStringRep(controller.getScope()), controller);
            }

            // remove controller which are no longer provided by the registry
            for (final CONTROLLER controller : new ArrayList<>(scopeControllerMap.values())) {
                if (unitControllerCollection.contains(controller)) {
                    continue;
                }
                scopeControllerMap.remove(ScopeProcessor.generateStringRep(controller.getScope()));
            }
        }
    }

    @Override
    public void shutdown() {
        if(unitSimulationManager != null) {
            unitSimulationManager.shutdown();
        }
        super.shutdown();
    }
}
