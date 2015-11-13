/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.bindings.DALBindingRegistry;
import de.citec.dal.bindings.openhab.OpenHABBinding;
import de.citec.dal.registry.DeviceRegistrySynchronizer;
import de.citec.dal.registry.DeviceRegistry;
import de.citec.dal.registry.UnitRegistry;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jp.JPDeviceClassDatabaseDirectory;
import de.citec.jp.JPDeviceConfigDatabaseDirectory;
import de.citec.jp.JPDeviceDatabaseDirectory;
import de.citec.jp.JPDeviceRegistryScope;
import de.citec.jp.JPLocationConfigDatabaseDirectory;
import de.citec.jp.JPLocationDatabaseDirectory;
import de.citec.jp.JPLocationRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPDebugMode;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.lm.remote.LocationRegistryRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Divine Threepwood
 */
public class DALService implements RegistryProvider {

    public static final String APP_NAME = DALService.class.getSimpleName();

    private static final Logger logger = LoggerFactory.getLogger(DALService.class);

    private static RegistryProvider registryProvider;

    private final DALBindingRegistry bindingRegistry;

    private final DeviceRegistry deviceRegistry;
    private final DeviceRegistrySynchronizer deviceRegistrySynchronizer;
    private final UnitRegistry unitRegistry;
    private final LocationRegistryRemote locationRegistryRemote;
    private final DeviceRegistryRemote deviceRegistryRemote;

    public DALService() throws InstantiationException {
        try {
            this.bindingRegistry = new DALBindingRegistry();
            this.deviceRegistry = new DeviceRegistry();
            this.unitRegistry = new UnitRegistry();
            this.locationRegistryRemote = new LocationRegistryRemote();
            this.deviceRegistryRemote = new DeviceRegistryRemote();
            this.deviceRegistrySynchronizer = new DeviceRegistrySynchronizer(deviceRegistry, deviceRegistryRemote);

            registryProvider = this;

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public static RegistryProvider getRegistryProvider() throws NotAvailableException {
        if (registryProvider == null) {
            throw new NotAvailableException(RegistryProvider.class);
        }
        return registryProvider;
    }

    public void init() throws CouldNotPerformException, InterruptedException {
        try {
            locationRegistryRemote.init();
            locationRegistryRemote.activate();
            deviceRegistryRemote.init();
            deviceRegistryRemote.activate();
            initBindings();
            deviceRegistrySynchronizer.init();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private void initBindings() throws CouldNotPerformException {
        bindingRegistry.register(OpenHABBinding.class);
    }

    public void shutdown() {
        bindingRegistry.shutdown();
        deviceRegistry.shutdown();
        unitRegistry.shutdown();
        locationRegistryRemote.shutdown();
        deviceRegistryRemote.shutdown();
        deviceRegistrySynchronizer.shutdown();
        registryProvider = null;
    }

    @Override
    public DALBindingRegistry getBindingRegistry() {
        return bindingRegistry;
    }

    @Override
    public DeviceRegistry getDeviceRegistry() {
        return deviceRegistry;
    }

    @Override
    public UnitRegistry getUnitRegistry() {
        return unitRegistry;
    }

    @Override
    public DeviceRegistryRemote getDeviceRegistryRemote() {
        return deviceRegistryRemote;
    }

    @Override
    public LocationRegistryRemote getLocationRegistryRemote() {
        return locationRegistryRemote;
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.Throwable
     */
    public static void main(String[] args) throws Throwable {

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPHardwareSimulationMode.class);
        JPService.registerProperty(JPLocationRegistryScope.class);
        JPService.registerProperty(JPDeviceRegistryScope.class);

        JPService.parseAndExitOnError(args);

        try {
            DALService dalService = new DALService();
            dalService.init();
        } catch (de.citec.jul.exception.InstantiationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }

        logger.info(APP_NAME + " successfully started.");
    }
}
