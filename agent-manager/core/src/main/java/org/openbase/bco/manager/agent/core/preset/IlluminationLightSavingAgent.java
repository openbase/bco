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
import java.util.concurrent.Future;
import org.openbase.bco.dal.remote.unit.UnitGroupRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationDataType;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class IlluminationLightSavingAgent extends AbstractAgentController {

    private static final int SLEEP_MILLI = 1000;
    public static final String MINIMUM_NEEDED_KEY = "MINIMUM_ILLUMINATION";
    public static final String MAXIMUM_WANTED_KEY = "MAXIMUM_ILLUMINATION";
    private static double MINIMUM_NEEDED_ILLUMINATION = 2000;
    private static double MAXIMUM_WANTED_ILLUMINATION = 4000;

    private LocationRemote locationRemote;
    private Future<Void> setPowerStateFutureAmbient;
    private Future<Void> setPowerStateFuture;
    private final Observer<LocationDataType.LocationData> locationObserver;
    private boolean regulated = false;

    public IlluminationLightSavingAgent() throws InstantiationException {
        super(IlluminationLightSavingAgent.class);

        locationObserver = (final Observable<LocationDataType.LocationData> source, LocationDataType.LocationData data) -> {
            if (data.getIlluminanceState().getIlluminance() > MAXIMUM_WANTED_ILLUMINATION) {
                if (!regulated) {
                    regulateLightIntensity();
                }
            } else if (data.getIlluminanceState().getIlluminance() < MINIMUM_NEEDED_ILLUMINATION) {
                if (regulated) {
                    deallocateResourceIteratively();
                }
            }
        };
    }

    @Override
    public void init(final UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            logger.debug("Initializing IlluminationLightSavingAgent[" + config.getLabel() + "]");
            CachedUnitRegistryRemote.waitForData();

            MetaConfigVariableProvider configVariableProvider = new MetaConfigVariableProvider("IlluminationLightSavingAgent", config.getMetaConfig());

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
                MINIMUM_NEEDED_ILLUMINATION = minimumNeededMeta;
            }
            if (maximumWantedMeta != -1) {
                MAXIMUM_WANTED_ILLUMINATION = maximumWantedMeta;
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getConfig().getLabel() + "]");
        locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), true, Units.LOCATION);

        /** Add trigger here and replace dataObserver */
        locationRemote.addDataObserver(locationObserver);
        locationRemote.waitForData();
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getConfig().getLabel() + "]");
        locationRemote.removeDataObserver(locationObserver);
    }

    private void regulateLightIntensity() throws CouldNotPerformException {
        try {
            UnitGroupRemote ambientLightGroup = Units.getUnitByLabel(locationRemote.getLabel().concat("AmbientLightGroup"), true, Units.UNITGROUP);
            setPowerStateFutureAmbient = ambientLightGroup.setPowerState(PowerState.newBuilder().setValue(PowerState.State.OFF).build()); // Blocking and trying to realloc all lights
            Thread.sleep(SLEEP_MILLI);
        } catch (NotAvailableException | InterruptedException ex) {
        }

        if (locationRemote.getIlluminanceState().getIlluminance() > MAXIMUM_WANTED_ILLUMINATION) {
            setPowerStateFuture = locationRemote.setPowerState(PowerState.newBuilder().setValue(PowerState.State.OFF).build(), UnitType.LIGHT);
        }
        regulated = true;
    }

    private void deallocateResourceIteratively() throws CouldNotPerformException {
        if (setPowerStateFuture != null && !setPowerStateFuture.isDone()) {
            setPowerStateFuture.cancel(true);
        }

        try {
            Thread.sleep(SLEEP_MILLI);
        } catch (InterruptedException ex) {
        }

        if (locationRemote.getIlluminanceState().getIlluminance() < MINIMUM_NEEDED_ILLUMINATION) {
            if (setPowerStateFutureAmbient != null && !setPowerStateFutureAmbient.isDone()) {
                setPowerStateFutureAmbient.cancel(true);
            }
        }
        regulated = false;
    }
}
