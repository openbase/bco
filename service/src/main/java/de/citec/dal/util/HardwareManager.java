/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.util;

import java.util.Map;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.RSBInformerPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.InvalidStateException;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;
import de.citec.jul.exception.NotAvailableException;

/**
 *
 * @author mpohling
 */
public class HardwareManager {

	private static final Logger logger = LoggerFactory.getLogger(HardwareManager.class);

	private static HardwareManager instance;

	private final Object SYNC_LOCK = new Object();
	private boolean active;
	private final DALRegistry registry;

	public synchronized static HardwareManager getInstance() {
		if (instance == null) {
			instance = new HardwareManager();
		}
		return instance;
	}

	private HardwareManager() {
		this.registry = DALRegistry.getInstance();
	}

	public void activate() throws Exception {
		synchronized (SYNC_LOCK) {
			active = true;

			for (AbstractDeviceController hardware : registry.getHardwareCollection()) {
				hardware.activate();
			}
			RSBInformerPool.getInstance().activate();
		}
	}

	public void deactivate() {
		synchronized (SYNC_LOCK) {
			active = false;
			for (AbstractDeviceController hardware : registry.getHardwareCollection()) {
				try {
					hardware.deactivate();
				} catch (InterruptedException ex) {
					logger.error("Could not deactivate: " + hardware, ex);
				}
			}
		}
	}

	public void internalReceiveUpdate(OpenhabCommand command) throws CouldNotPerformException, NotAvailableException {
		logger.debug("Incomming Item[" + command.getItem() + "] State[" + command.getType() + "].");
		String itemName = command.getItem();
		if (!active) {
			throw new InvalidStateException("Hardware manager is not active!");
		}
		AbstractDeviceController hardware;
		synchronized (SYNC_LOCK) {
			try {
				Map.Entry<String, AbstractDeviceController> floorEntry = registry.getDeviceMap().floorEntry(itemName);
				hardware = floorEntry.getValue();
			} catch (NullPointerException ex) {
				throw new NotAvailableException("Item[" + itemName + "] not registered!");
			}
		}
		if (!itemName.startsWith(hardware.getId())) {
			throw new CouldNotPerformException("Skip item update [" + itemName + "=" + command.getType() + "]: Item is not registered.");
		}
		hardware.internalReceiveUpdate(itemName, command);
	}

}
