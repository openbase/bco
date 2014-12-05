/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import java.util.Map;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractDeviceController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;

/**
 *
 * @author mpohling
 */
public class HardwareManager {

    private static final Logger logger = LoggerFactory.getLogger(HardwareManager.class);

    private static final class InstanceHolder {

        static final HardwareManager INSTANCE = new HardwareManager();
    }

    private final Object SYNC_LOCK = new Object();
    private boolean active;
    private final HardwareRegistry registry = HardwareRegistry.getInstance();

    public static HardwareManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public void activate() throws Exception {
        synchronized (SYNC_LOCK) {
            active = true;
            for (AbstractDeviceController hardware : registry.getHardwareCollection()) {
                try {
                    hardware.activate();
                } catch (RSBBindingException ex) {
                    logger.error("Could not activate: " + hardware, ex);
                }
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
                } catch (RSBBindingException ex) {
                    logger.error("Could not deactivate: " + hardware, ex);
                }
            }
        }
    }

    public void internalReceiveUpdate(OpenhabCommand command) {
        logger.debug("Incomming Item[" + command.getItem() + "] State[" + command.getType() + "].");
        String itemName = command.getItem();
        if (!active) {
            logger.warn("Skip internal update: RSBBinding not activated!");
            return;
        }
        AbstractDeviceController hardware;
        synchronized (SYNC_LOCK) {
            Map.Entry<String, AbstractDeviceController> floorEntry = registry.getHardwareMap().floorEntry(itemName);
            hardware = floorEntry.getValue();
        }
        if (!itemName.startsWith(hardware.getId())) {
            logger.debug("Skip item update [" + itemName + "=" + command.getType() + "] because item is not registered.");
            return;
        }
        hardware.internalReceiveUpdate(itemName, command);
    }

}
