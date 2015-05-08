/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.bindings.DALBindingRegistry;
import de.citec.dal.bindings.openhab.OpenHABBinding;
import de.citec.dal.registry.DeviceManagerRemoteDalConnector;
import de.citec.dal.registry.DeviceRegistry;
import de.citec.dal.registry.UnitRegistry;
import de.citec.dal.util.ConnectionManager;
import de.citec.dal.util.DeviceInitializer;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPDebugMode;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
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
    private final UnitRegistry unitRegistry;
    private final ConnectionManager connectionManager;

    public DALService() throws InstantiationException {
        try {
            this.bindingRegistry = new DALBindingRegistry();
            this.deviceRegistry = new DeviceRegistry();
            this.unitRegistry = new UnitRegistry();
            this.connectionManager = new ConnectionManager(deviceRegistry);

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

    public void init() throws CouldNotPerformException {
        init(new DeviceManagerRemoteDalConnector());
    }

    public void init(final DeviceInitializer initializer) throws CouldNotPerformException {
        try {
            initBindings();
            initializer.initDevices(deviceRegistry);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private void initBindings() throws CouldNotPerformException {
        bindingRegistry.register(OpenHABBinding.class);
    }

    public void activate() {
        try {
            this.connectionManager.activate();
        } catch (Exception ex) {
            logger.warn("Hardware manager could not be activated!", ex);
        }
    }

    public void deactivate() {
        try {
            this.connectionManager.deactivate();
        } catch (Exception ex) {
            logger.warn("Hardware manager could not be deactivated!", ex);
        }
    }

    public void shutdown() {
        deactivate();
//        RSBInformerPool.getInstance().shutdown();
        bindingRegistry.clean();
        deviceRegistry.clean();
        unitRegistry.clean();
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

    public ConnectionManager getConnectionManager() {
        return connectionManager;
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
        JPService.parseAndExitOnError(args);

        try {
            DALService dalService = new DALService();
            dalService.init();
            dalService.activate();
        } catch (de.citec.jul.exception.InstantiationException ex) {
            throw ExceptionPrinter.printHistory(logger, ex);
        }

        logger.info(APP_NAME
                + " successfully started.");
    }
}
