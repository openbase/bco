/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.manager.agent.core.preset;

/*
 * #%L
 * BCO Manager Agent Core
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.util.concurrent.ExecutionException;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.Observable;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.location.LocationDataType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.Future;
import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import rst.domotic.unit.UnitConfigType;


/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class BrightnessLightSavingAgent extends AbstractAgentController {
    private static final int SLEEP_MILLI = 1000;
    public static final String MINIMUM_NEEDED_KEY = "MINIMUM_BRIGHTNESS";
    public static final String MAXIMUM_WANTED_KEY = "MAXIMUM_BRIGHTNESS";
    private static double MINIMUM_NEEDED_BRIGHTNESS = 2000;
    private static double MAXIMUM_WANTED_BRIGHTNESS = 4000;    
    
    private LocationRemote locationRemote;
    private Future<Void> setPowerStateFutureAmbient;
    private Future<Void> setPowerStateFuture;

    public BrightnessLightSavingAgent() throws InstantiationException {
        super(BrightnessLightSavingAgent.class);
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            logger.debug("Initializing BrightnessLightSavingAgent[" + config.getLabel() + "]");
            CachedUnitRegistryRemote.waitForData();

            MetaConfigVariableProvider configVariableProvider = new MetaConfigVariableProvider("BrightnessLightSavingAgent", config.getMetaConfig());

            int minimumNeededMeta = -1;
            int maximumWantedMeta = -1;
            try {
                minimumNeededMeta = Integer.parseInt(configVariableProvider.getValue(MINIMUM_NEEDED_KEY));
            } catch (CouldNotPerformException ex) {
            }
            try {
                maximumWantedMeta = Integer.parseInt(configVariableProvider.getValue(MAXIMUM_WANTED_KEY));
            } catch (CouldNotPerformException ex) {
            }
            if (minimumNeededMeta != -1) {
                MINIMUM_NEEDED_BRIGHTNESS = minimumNeededMeta;
            }
            if (maximumWantedMeta != -1) {
                MAXIMUM_WANTED_BRIGHTNESS = maximumWantedMeta;
            }            
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }
    
    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getConfig().getLabel() + "]");
        locationRemote = new LocationRemote();
        Registries.getLocationRegistry().waitForData();
        locationRemote.init(Registries.getLocationRegistry().getLocationConfigById(getConfig().getId()));

        /** Add trigger here and replace dataObserver */
        locationRemote.addDataObserver((Observable<LocationDataType.LocationData> source, LocationDataType.LocationData data) -> {
            if (data.getBrightnessState().getBrightness() > MAXIMUM_WANTED_BRIGHTNESS) {
                regulateLightIntensity();
            } else if (data.getBrightnessState().getBrightness() < MINIMUM_NEEDED_BRIGHTNESS) {
                deallocateResourceIteratively();
            }
        });
        locationRemote.activate();
        super.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getClass().getSimpleName() + "]");
        locationRemote.deactivate();
        super.deactivate();
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        locationRemote.activate();
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        locationRemote.deactivate();
    }

    private void regulateLightIntensity() throws CouldNotPerformException {
        try {
            ColorableLightRemote ambientLightGroup = Units.getUnitByLabel(locationRemote.getLabel().concat("AmbientLightGroup"), true, Units.COLORABLE_LIGHT);
            setPowerStateFutureAmbient = ambientLightGroup.setPowerState(PowerState.newBuilder().setValue(PowerState.State.OFF).build()); // Blocking and trying to realloc all lights
            Thread.sleep(SLEEP_MILLI);
        } catch (NotAvailableException | InterruptedException ex) {
        }
        
        if (locationRemote.getBrightnessState().getBrightness() > MAXIMUM_WANTED_BRIGHTNESS) {
            setPowerStateFuture = locationRemote.setPowerState(PowerState.newBuilder().setValue(PowerState.State.OFF).build(), UnitType.LIGHT);
        }
    }

    private void deallocateResourceIteratively() throws CouldNotPerformException {
        if (setPowerStateFuture != null && !setPowerStateFuture.isDone()) {
            setPowerStateFuture.cancel(true);
            try {
                setPowerStateFuture.get();
                Thread.sleep(SLEEP_MILLI);
            } catch (InterruptedException | ExecutionException ex) {
            }
        }
        if (locationRemote.getBrightnessState().getBrightness() < MINIMUM_NEEDED_BRIGHTNESS) {
            if (setPowerStateFutureAmbient != null && !setPowerStateFutureAmbient.isDone()) {
                setPowerStateFutureAmbient.cancel(true);
            }
        }
    }
}
