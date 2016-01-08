package org.dc.bco.manager.device.core;

import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.dal.lib.layer.service.mock.ServiceFactoryMock;
import org.dc.bco.manager.device.lib.DeviceManager;
import org.dc.bco.registry.device.lib.jp.JPDeviceRegistryScope;
import org.dc.bco.registry.location.lib.jp.JPLocationRegistryScope;
import org.dc.jps.core.JPService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class DeviceManagerLauncher {

    private static final Logger logger = LoggerFactory.getLogger(DeviceManagerLauncher.class);

    private DeviceManagerController deviceManagerController;
    
    public void launch() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        try {
            deviceManagerController = new DeviceManagerController(new ServiceFactoryMock());
            deviceManagerController.init();
        } catch (CouldNotPerformException ex) {
            deviceManagerController.shutdown();
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    public void shutdown() {
        deviceManagerController.shutdown();
    }

    public DeviceManager getDeviceManager() {
        return deviceManagerController;
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     * @throws org.dc.jul.exception.CouldNotPerformException
     */
    public static void main(String[] args) throws InterruptedException, CouldNotPerformException {

        /* Setup JPService */
        JPService.setApplicationName(DeviceManager.class);
        JPService.registerProperty(JPHardwareSimulationMode.class);
        JPService.registerProperty(JPLocationRegistryScope.class);
        JPService.registerProperty(JPDeviceRegistryScope.class);
        JPService.parseAndExitOnError(args);

        /* Start main app */
        logger.info("Start " + JPService.getApplicationName() + "...");
        try {
            new DeviceManagerLauncher().launch();
        } catch (CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }
}
