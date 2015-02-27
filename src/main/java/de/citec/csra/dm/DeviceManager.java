/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm;

import de.citec.csra.dm.registry.DeviceRegistryImpl;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPReadOnly;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.rsb.RSBInformerInterface;
import de.citec.jul.rsb.jp.JPScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;

/**
 *
 * @author mpohling
 */
public class DeviceManager {

    private static final Logger logger = LoggerFactory.getLogger(DeviceManager.class);

    public static final String APP_NAME = "DeviceManager";

    private final DeviceRegistryImpl deviceRegistry;

    public DeviceManager() throws InitializationException {
        try {
            this.deviceRegistry = new DeviceRegistryImpl();
            this.deviceRegistry.init(RSBInformerInterface.InformerType.Single);
            this.deviceRegistry.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public static void main(String args[]) throws Throwable {
        logger.info("Start " + APP_NAME + "...");
        try {
            /* Setup CLParser */
            JPService.setApplicationName(APP_NAME);
//
            JPService.registerProperty(JPScope.class, new Scope("/devicemanager/registry"));
            JPService.registerProperty(JPReadOnly.class);
            JPService.registerProperty(JPDeviceConfigDatabaseDirectory.class);
            JPService.registerProperty(JPDeviceClassDatabaseDirectory.class);

//        JPService.registerProperty(JPShowGUI.class, true);
            JPService.parseAndExitOnError(args);

//        if (JPService.getAttribute(JPShowGUI.class).getValue()) {
//            DevieManagerGUI.main(args);
//        }
            new DeviceManager();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistory(logger, ex);
        }
    }
}
