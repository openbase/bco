/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.util;

import de.citec.dal.registry.DeviceRegistry;
import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.iface.Activatable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class ConnectionManager implements Activatable {

	private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

	private final Object SYNC_LOCK = new Object();
	private boolean active;
	private final DeviceRegistry registry;

	public ConnectionManager(final DeviceRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void activate() throws InterruptedException {
		synchronized (SYNC_LOCK) {
			active = true;

			for (Device device : registry.getEntries()) {
				try {
					device.activate();
				} catch (CouldNotPerformException ex) {
					logger.error("Could not activate: " + device, ex);
				}
			}
		}
	}

	@Override
	public void deactivate() {
		synchronized (SYNC_LOCK) {
			active = false;
			for (Device device : registry.getEntries()) {
				try {
					device.deactivate();
				} catch (CouldNotPerformException | InterruptedException ex) {
					logger.error("Could not deactivate: " + device, ex);
				}
			}
		}
	}

	@Override
	public boolean isActive() {
		synchronized (SYNC_LOCK) {
			return active;
		}
	}
}
