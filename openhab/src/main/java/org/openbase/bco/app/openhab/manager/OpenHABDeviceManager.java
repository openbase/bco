package org.openbase.bco.app.openhab.manager;

import org.eclipse.smarthome.core.library.types.HSBType;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.app.openhab.manager.service.OpenHABServiceFactory;
import org.openbase.bco.app.openhab.manager.transform.OpenHABColorStateTransformer;
import org.openbase.bco.app.openhab.registry.synchronizer.OpenHABItemHelper;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.manager.device.core.DeviceManagerController;
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
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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

                    for (UnitController<?, ?> unitController : deviceManagerController.getUnitControllerRegistry().getEntries()) {
                        final UnitConfig unitConfig = unitController.getConfig();
                        final Set<ServiceType> serviceTypeSet = new HashSet<>();
                        for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                            if (serviceTypeSet.contains(serviceConfig.getServiceDescription().getType())) {
                                continue;
                            }
                            serviceTypeSet.add(serviceConfig.getServiceDescription().getType());

                            String itemName = OpenHABItemHelper.generateItemName(unitConfig, serviceConfig.getServiceDescription().getType());
                            if (itemName.equals(entry.getKey())) {
                                logger.info("Found according unit[" + unitController.getLabel() + "] with service[" + serviceConfig.getServiceDescription().getType() + "]");

                                switch (serviceConfig.getServiceDescription().getType()) {
                                    case COLOR_STATE_SERVICE:
                                        ColorState colorState = OpenHABColorStateTransformer.transform(HSBType.valueOf(entry.getValue()));
                                        unitController.applyDataUpdate(TimestampProcessor.updateTimestampWithCurrentTime(colorState), ServiceType.COLOR_STATE_SERVICE);
                                        break;
                                    case POWER_STATE_SERVICE:
                                        break;
                                    case BRIGHTNESS_STATE_SERVICE:
                                        break;
                                }
                            }
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
