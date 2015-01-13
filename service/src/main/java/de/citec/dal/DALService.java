/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.service.DALRegistry;
import de.citec.dal.service.HardwareManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 */
public class DALService {

	private static final Logger logger = LoggerFactory.getLogger(DALService.class);

	private final DALRegistry registry;
	private final HardwareManager hardwareManager;

	public DALService() {
		this.registry = DALRegistry.getInstance();
		this.registry.initDevices();
		this.hardwareManager = HardwareManager.getInstance();
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

	public HardwareManager getHardwareManager() {
		return hardwareManager;
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		new DALService().activate();
	}

}
