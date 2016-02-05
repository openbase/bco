
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.core;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import org.dc.bco.manager.device.lib.Device;
import org.dc.bco.manager.device.lib.DeviceControllerRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.storage.registry.RegistryImpl;

/**
 *
 * @author mpohling
 */
public class DeviceControllerRegistryImpl extends RegistryImpl<String, Device> implements DeviceControllerRegistry {

    private final ControllerManager controllerManager;

    public DeviceControllerRegistryImpl() throws InstantiationException {
        try {
            this.controllerManager = new ControllerManager();
        } catch (Exception ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public DeviceControllerRegistryImpl(final HashMap<String, Device> entryMap) throws InstantiationException {
        super(entryMap);
        try {
            this.controllerManager = new ControllerManager();
        } catch (Exception ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void clear() throws CouldNotPerformException {
        for(Device device : getEntries()) {
            controllerManager.deactivate(device);
        }
        super.clear();
    }

    @Override
    public Device remove(Device entry) throws CouldNotPerformException {
        controllerManager.deactivate(entry);
        return super.remove(entry);
    }

    @Override
    public Device update(Device entry) throws CouldNotPerformException {
        Device oldEntry = super.get(entry.getId());
        try {
            controllerManager.deactivate(oldEntry).get();
        } catch (InterruptedException | ExecutionException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
        super.update(entry);
        controllerManager.activate(entry);
        return entry;
    }

    @Override
    public Device register(Device entry) throws CouldNotPerformException {
        controllerManager.activate(entry);
        return super.register(entry);
    }
}
