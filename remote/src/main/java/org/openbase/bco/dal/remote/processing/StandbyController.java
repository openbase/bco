package org.openbase.bco.dal.remote.processing;

import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.state.PowerStateType;

import java.awt.dnd.Autoscroll;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StandbyProcessor {

    private void standby(final StandbyStateOperationService controller, final Logger logger) throws CouldNotPerformException, InterruptedException {
        synchronized (standbySync) {
            if (standby) {
                return;
            }
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
                standby = true;
                logger.info("Switch off all devices in the " + controller.getLabel());
                controller.setPowerState(PowerStateType.PowerState.State.OFF);
                logger.info(controller.getLabel() + " is now standby.");
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Standby failed!", ex);
            } finally {
            }
        }
    }

    private void wakeUp(final StandbyStateOperationService controller, final Logger logger) throws CouldNotPerformException, InterruptedException {
        logger.info("Wake up " + controller.getLabel() + "...");
        synchronized (standbySync) {
            standby = false;

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
}
