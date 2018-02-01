package org.openbase.bco.dal.remote.processing;

/*-
 * #%L
 * BCO DAL Remote
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

import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.service.provider.PowerStateProviderService;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Initializable;
import org.openbase.jul.iface.Snapshotable;
import org.openbase.jul.iface.provider.LabelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.state.PowerStateType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StandbyController<C extends StandbyStateOperationService & Snapshotable<Snapshot> & LabelProvider & PowerStateOperationService> implements Initializable<C> {

    /**
     * 60 second default timeout to record a snapshot.
     */
    public static final long RECORD_SNAPSHOT_TIMEOUT = 60;

    /**
     * 15 second default timeout to restore a snapshot.
     */
    public static final long RESTORE_SNAPSHOT_TIMEOUT = 15;

    private Snapshot snapshot;
    private C controller;
    private Logger logger;

    @Override
    public void init(final C controller) throws InitializationException, InterruptedException {
        this.controller = controller;
        this.logger = LoggerFactory.getLogger(controller.getClass());
    }

    public void standby() throws CouldNotPerformException, InterruptedException {
        logger.info("Standby " + controller.getLabel() + "...");
        try {
            try {
                logger.debug("Create snapshot of " + controller.getLabel() + " state.");
                snapshot = controller.recordSnapshot().get(RECORD_SNAPSHOT_TIMEOUT, TimeUnit.SECONDS);

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
            logger.info("Switch off all devices in the " + controller.getLabel());
            controller.setPowerState(PowerStateType.PowerState.State.OFF);
            logger.info(controller.getLabel() + " is now standby.");
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Standby failed!", ex);
        }
    }

    public void wakeup() throws CouldNotPerformException, InterruptedException {
        logger.info("Wake up " + controller.getLabel() + "...");

        if (snapshot == null) {
            logger.debug("skip wake up because no snapshot information available!");
            return;
        }

        Future restoreSnapshotFuture = null;
        try {
            logger.debug("restore snapshot: " + snapshot);

            restoreSnapshotFuture = controller.restoreSnapshot(snapshot);
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
