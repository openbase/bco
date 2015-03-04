/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm;

import de.citec.csra.dm.registry.DeviceRegistryImpl;
import de.citec.jp.JPDatabaseDirectory;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jp.JPInitializeDBFlag;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPReadOnly;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.rsb.RSBInformerInterface;
import de.citec.jul.rsb.jp.JPScope;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;

/**
 *
 * @author mpohling
 */
public class DeviceManager {

    private static final Logger logger = LoggerFactory.getLogger(DeviceManager.class);

	public static final Scope DEFAULT_SCOPE = new Scope("/devicemanager/registry");
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

        /* Setup CLParser */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPScope.class, DEFAULT_SCOPE);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPInitializeDBFlag.class);
        JPService.registerProperty(JPDatabaseDirectory.class, new File("/tmp/database/"));
        JPService.registerProperty(JPDeviceConfigDatabaseDirectory.class, new File("/tmp/database/device"));
        JPService.registerProperty(JPDeviceClassDatabaseDirectory.class, new File("/tmp/database/class"));

        JPService.parseAndExitOnError(args);

        try {
            new DeviceManager();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistory(logger, ex);
        }
    }
}
