package org.dc.bco.manager.device.binding.openhab;

import org.dc.bco.manager.device.binding.openhab.comm.OpenHABCommunicator;
import org.dc.bco.manager.device.binding.openhab.comm.OpenHABCommunicatorImpl;
import org.dc.bco.manager.device.binding.openhab.service.OpenhabServiceFactory;
import org.dc.bco.manager.device.core.DeviceManagerController;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.binding.BindingTypeHolderType.BindingTypeHolder.BindingType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class OpenHABBindingImpl implements OpenHABBinding {

    private static final Logger logger = LoggerFactory.getLogger(OpenHABBindingImpl.class);

    private static OpenHABBinding instance;
    private DeviceManagerController deviceManagerController;
    private OpenHABCommunicatorImpl busCommunicator;
    private DeviceRegistryRemote deviceRegistryRemote;

    public OpenHABBindingImpl() {
        instance = this;
    }

    public static OpenHABBinding getInstance() throws NotAvailableException {
        if (instance == null) {
            throw new NotAvailableException(OpenHABBinding.class);
        }
        return instance;
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            this.deviceRegistryRemote = new DeviceRegistryRemote();
            this.deviceRegistryRemote.init();
            this.deviceRegistryRemote.activate();
            this.deviceManagerController = new DeviceManagerController(new OpenhabServiceFactory()) {

                @Override
                public boolean isSupported(DeviceConfigType.DeviceConfig config) throws CouldNotPerformException {
                    try {
                        DeviceClass deviceClass = deviceRegistryRemote.getDeviceClassById(config.getDeviceClassId());
                        if (!deviceClass.getBindingConfig().getType().equals(BindingType.OPENHAB)) {
                            return false;
                        }
                        return super.isSupported(config);
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not check device support!", ex), logger);
                        return false;
                    }
                }
            };
            deviceManagerController.init();
            this.busCommunicator = new OpenHABCommunicatorImpl();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() throws InterruptedException {
        deviceManagerController.shutdown();
        busCommunicator.shutdown();
        instance = null;
    }

    @Override
    public OpenHABCommunicator getBusCommunicator() throws NotAvailableException {
        return busCommunicator;
    }
}
