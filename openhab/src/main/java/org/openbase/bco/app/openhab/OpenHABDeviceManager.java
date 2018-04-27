package org.openbase.bco.app.openhab;

import org.eclipse.smarthome.core.library.types.HSBType;
import org.openbase.bco.app.openhab.service.OpenHABServiceFactory;
import org.openbase.bco.app.openhab.transform.OpenHABColorStateTransformer;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.manager.device.core.DeviceManagerController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ColorStateType.ColorState;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OpenHABDeviceManager implements Launchable<Void>, VoidInitializable {

    private final Logger logger = LoggerFactory.getLogger(OpenHABDeviceManager.class);

    private final DeviceManagerController deviceManagerController;
    private ScheduledFuture updateTask;

    public OpenHABDeviceManager() throws InterruptedException, InstantiationException {
        deviceManagerController = new DeviceManagerController(new OpenHABServiceFactory());
    }

    @Override
    public void init() throws InterruptedException, InitializationException {
        deviceManagerController.init();
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        deviceManagerController.activate();
        updateTask = GlobalScheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                Map<String, String> states = OpenHABRestCommunicator.getInstance().getStates();
                for (Entry<String, String> entry : states.entrySet()) {
                    logger.info("Update for item[" + entry.getKey() + "] to state[" + entry.getValue() + "]");

                    String alias = entry.getKey().replaceAll("_", "-");
                    for (UnitController<?, ?> unitController : deviceManagerController.getUnitControllerRegistry().getEntries()) {
                        if (unitController.getConfig().getAlias(0).equals(alias)) {
                            logger.info("Found according unit[" + unitController.getLabel() + "]");

                            ColorState colorState = OpenHABColorStateTransformer.transform(HSBType.valueOf(entry.getValue()));
                            unitController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(colorState), ServiceType.COLOR_STATE_SERVICE);
                        }
                    }
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not sync item states", ex), logger);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        if (updateTask != null) {
            updateTask.cancel(true);
            updateTask = null;
        }
        deviceManagerController.deactivate();
    }

    @Override
    public boolean isActive() {
        return deviceManagerController.isActive() && updateTask != null;
    }
}
