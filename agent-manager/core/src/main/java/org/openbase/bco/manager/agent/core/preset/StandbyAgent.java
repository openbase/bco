package org.openbase.bco.manager.agent.core.preset;

/*
 * #%L
 * BCO Manager Agent Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.manager.agent.core.AbstractAgentController;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.schedule.Timeout;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.state.PowerStateType;
import rst.domotic.state.PresenceStateType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationDataType;
import rst.domotic.unit.location.LocationDataType.LocationData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class StandbyAgent extends AbstractAgentController {

    /**
     * 15 min default standby timeout
     */
    public static final long TIMEOUT = 60000 * 15;
    /**
     * 60 second default timeout to record a snapshot.
     */
    public static final long RECORD_SNAPSHOT_TIMEOUT = 60;
    /**
     * 15 second default timeout to restore a snapshot.
     */
    public static final long RESTORE_SNAPSHOT_TIMEOUT = 15;

    private LocationRemote locationRemote;
    private final Timeout timeout;
    private final SyncObject standbySync = new SyncObject("StandbySync");
    private boolean standby;
    private final Observer<LocationData> locationDataObserver;

    private Snapshot snapshot;

    public StandbyAgent() throws InstantiationException, CouldNotPerformException, InterruptedException {
        super(StandbyAgent.class);

        this.standby = false;

        this.timeout = new Timeout(TIMEOUT) {

            @Override
            public void expired() throws InterruptedException {
                try {
                    standby();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        };

        this.locationDataObserver = new Observer<LocationData>() {
            @Override
            public void update(Observable<LocationDataType.LocationData> source, LocationDataType.LocationData data) throws Exception {
                triggerPresenceChange(data);
            }
        };
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        locationRemote = Units.getUnit(getConfig().getPlacementConfig().getLocationId(), true, Units.LOCATION);
        locationRemote.addDataObserver(locationDataObserver);
        locationRemote.waitForData();
        triggerPresenceChange(locationRemote.getData());
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        if (locationRemote != null) {
            locationRemote.removeDataObserver(locationDataObserver);
            locationRemote = null;
        }
        timeout.cancel();
    }

    public void triggerPresenceChange(LocationDataType.LocationData data) throws InterruptedException {
        synchronized (standbySync) {
            if (data.getPresenceState().getValue().equals(PresenceStateType.PresenceState.State.PRESENT)) {
                timeout.cancel();
                if (standby) {
                    try {
                        wakeUp();
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not notify motion state change!", ex), logger);
                    }
                }
            } else if (data.getPresenceState().getValue().equals(PresenceStateType.PresenceState.State.ABSENT)) {
                if (!timeout.isActive()) {
                    try {
                        timeout.start();
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not schedule presence timeout!", ex), logger);
                    }
                }
            }
        }
    }

    private void standby() throws CouldNotPerformException, InterruptedException {
        synchronized (standbySync) {
            if (standby) {
                return;
            }
            logger.info("Standby " + locationRemote.getLabel() + "...");
            try {
                try {
                    logger.debug("Create snapshot of " + locationRemote.getLabel() + " state.");
                    snapshot = locationRemote.recordSnapshot().get(RECORD_SNAPSHOT_TIMEOUT, TimeUnit.SECONDS);

                    // filter out particular units and services 
                    List<ServiceStateDescription> serviceStateDescriptionList = new ArrayList<>();
                    for (ServiceStateDescription serviceStateDescription : snapshot.getServiceStateDescriptionList()) {
                        // filter neutral power states
                        if (serviceStateDescription.getServiceAttribute().toLowerCase().contains("off")) {
                            logger.debug("ignore " + serviceStateDescription.getUnitId() + " because unit is off.");
                            continue;
                        }

                        // filter neutral brightness states
                        if (serviceStateDescription.getServiceAttribute().toLowerCase().contains("brightness: 0.0")) {
                            logger.debug("ignore " + serviceStateDescription.getUnitId() + " because brightness is 0.");
                            continue;
                        }

                        // filter base units
                        if (UnitConfigProcessor.isBaseUnit(serviceStateDescription.getUnitType())) {
                            logger.debug("ignore " + serviceStateDescription.getUnitId() + " because is a base unit.");
                            continue;
                        }

                        // filter roller shutter
                        if (serviceStateDescription.getUnitType().equals(UnitType.ROLLER_SHUTTER)) {
                            logger.debug("ignore " + serviceStateDescription.getUnitId() + " because reconstructing roller shutter states are to dangerous.");
                            continue;
                        }

                        // let only Power + Brightness + Color States pass because these are the ones which are manipulated. 
                        if (!serviceStateDescription.getServiceType().equals(ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE)
                                && !serviceStateDescription.getServiceType().equals(ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE)
                                && !serviceStateDescription.getServiceType().equals(ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_STATE_SERVICE)) {
                            logger.debug("ignore " + serviceStateDescription.getUnitId() + " because this type is not supported by " + this);
                            continue;
                        }

                        serviceStateDescriptionList.add(serviceStateDescription);
                    }
                    snapshot = snapshot.toBuilder().clearServiceStateDescription().addAllServiceStateDescription(serviceStateDescriptionList).build();
                } catch (ExecutionException | CouldNotPerformException | TimeoutException ex) {
                    ExceptionPrinter.printHistory("Could not create snapshot!", ex, logger);
                }
                standby = true;
                logger.info("Switch off all devices in the " + locationRemote.getLabel());
                locationRemote.setPowerState(PowerStateType.PowerState.State.OFF);
                logger.info(locationRemote.getLabel() + " is now standby.");
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Standby failed!", ex);
            } finally {
            }
        }
    }

    private void wakeUp() throws CouldNotPerformException, InterruptedException {
        logger.info("Wake up " + locationRemote.getLabel() + "...");
        synchronized (standbySync) {
            standby = false;

            if (snapshot == null) {
                logger.debug("skip wake up because no snapshot information available!");
                return;
            }

            Future restoreSnapshotFuture = null;
            try {
                logger.debug("restore snapshot: " + snapshot);

                restoreSnapshotFuture = locationRemote.restoreSnapshot(snapshot);
                restoreSnapshotFuture.get(RESTORE_SNAPSHOT_TIMEOUT, TimeUnit.SECONDS);
                snapshot = null;

            } catch (CouldNotPerformException | ExecutionException ex) {
                throw new CouldNotPerformException("WakeUp failed!", ex);
            } catch (TimeoutException ex) {
                if (restoreSnapshotFuture != null) {
                    restoreSnapshotFuture.cancel(true);
                }
                throw new CouldNotPerformException("WakeUp took more than " + RESTORE_SNAPSHOT_TIMEOUT + " seconds", ex);
            }
        }
    }
}
