/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.registry.CSRADeviceInitializerImpl;
import de.citec.dal.util.DALRegistry;
import de.citec.dal.util.ConnectionManager;
import de.citec.dal.util.DeviceInitializer;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPDebugMode;
import de.citec.jps.properties.JPHardwareSimulationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 */
public class DALService {

    private static final Logger logger = LoggerFactory.getLogger(DALService.class);
    
	private final DALRegistry registry;
	private final ConnectionManager hardwareManager;

	public DALService() {
		this(new CSRADeviceInitializerImpl());
	}

	public DALService(DeviceInitializer initializer) {
		this.registry = DALRegistry.getInstance();
		initializer.initDevices(registry);
		this.hardwareManager = new ConnectionManager(registry);
	}

	public void activate() {
		try {
			this.hardwareManager.activate();
		} catch (Exception ex) {
			logger.warn("Hardware manager could not be activated!", ex);
		}
	}

	public void deactivate() {
		try {
			this.hardwareManager.deactivate();
		} catch (Exception ex) {
			logger.warn("Hardware manager could not be deactivated!", ex);
		}
	}

	public DALRegistry getRegistry() {
		return registry;
	}

	public ConnectionManager getHardwareManager() {
		return hardwareManager;
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
