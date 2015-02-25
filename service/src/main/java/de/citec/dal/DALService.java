/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.registry.CSRADeviceInitializerImpl;
import de.citec.dal.registry.DeviceRegistry;
import de.citec.dal.registry.UnitRegistry;
import de.citec.dal.util.ConnectionManager;
import de.citec.dal.util.DeviceInitializer;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPDebugMode;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.rsb.RSBInformerPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 */
public class DALService implements RegistryProvider {

    private static final Logger logger = LoggerFactory.getLogger(DALService.class);

	private static RegistryProvider registryProvider;

	private final DeviceRegistry deviceRegistry;
	private final UnitRegistry unitRegistry;
	private final ConnectionManager connectionManager;

	public DALService() {
		this(new CSRADeviceInitializerImpl());
	}

	public DALService(final DeviceInitializer initializer) {
		this.deviceRegistry = new DeviceRegistry();
		this.unitRegistry = new UnitRegistry();
		this.connectionManager = new ConnectionManager(deviceRegistry);

		registryProvider = this;
		initializer.initDevices(deviceRegistry);
	}

	public static RegistryProvider getRegistryProvider() throws NotAvailableException {
		if(registryProvider == null) {
			throw new NotAvailableException(RegistryProvider.class);
		}
		return registryProvider;
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
		RSBInformerPool.getInstance().shutdown();
		deviceRegistry.shutdown();
		unitRegistry.shutdown();
		registryProvider = null;
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
	 */
	public static void main(String[] args) {
		/* Setup CLParser */
		JPService.setApplicationName("DeviceManager");
		JPService.registerProperty(JPDebugMode.class);
		JPService.registerProperty(JPHardwareSimulationMode.class);
		JPService.parseAndExitOnError(args);

		new DALService().activate();
	}
}
