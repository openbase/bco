package org.openbase.bco.app.openhab.manager;

import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.app.openhab.manager.service.OpenHABServiceFactory;
import org.openbase.bco.manager.device.core.DeviceManagerController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

public class OpenHABDeviceManager implements Launchable<Void>, VoidInitializable {

    public static final String ITEM_STATE_TOPIC_FILTER = "/smarthome/items/*/state";

    private final DeviceManagerController deviceManagerController;
    private final CommandExecutor commandExecutor;

    public OpenHABDeviceManager() throws InterruptedException, InstantiationException {
        this.deviceManagerController = new DeviceManagerController(new OpenHABServiceFactory()) {

            @Override
            public boolean isSupported(UnitConfig config) throws CouldNotPerformException {
                DeviceClass deviceClass = Registries.getClassRegistry().getDeviceClassById(config.getDeviceConfig().getDeviceClassId());
                if(!deviceClass.getBindingConfig().getBindingId().equals("OPENHAB")) {
                    return false;
                }

                return super.isSupported(config);
            }
        };
        this.commandExecutor = new CommandExecutor(deviceManagerController.getUnitControllerRegistry());
    }

    @Override
    public void init() throws InterruptedException, InitializationException {
        deviceManagerController.init();
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        deviceManagerController.activate();
        OpenHABRestCommunicator.getInstance().addSSEObserver(commandExecutor, ITEM_STATE_TOPIC_FILTER);

        //TODO: perform an initial sync?, this has to wait for the synchronizer in the device manager controller
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        OpenHABRestCommunicator.getInstance().removeSSEObserver(commandExecutor, ITEM_STATE_TOPIC_FILTER);
        deviceManagerController.deactivate();
    }

    @Override
    public boolean isActive() {
        return deviceManagerController.isActive();
    }
}
