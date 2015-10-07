
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.dal.hal.device.Device;
import de.citec.dal.util.ConnectionManager;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.storage.registry.Registry;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author mpohling
 */
public class DeviceRegistry extends Registry<String, Device> {
    
    private final ConnectionManager connectionManager;
    
    public DeviceRegistry() throws InstantiationException {
        try {
            this.connectionManager = new ConnectionManager();
        } catch (Exception ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public DeviceRegistry(final HashMap<String, Device> entryMap) throws InstantiationException {
        super(entryMap);
        try {
            this.connectionManager = new ConnectionManager();
        } catch (Exception ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void clear() throws CouldNotPerformException {
        for(Device device : getEntries()) {
            connectionManager.deactivate(device);
        }
        super.clear();
    }

    @Override
    public Device remove(Device entry) throws CouldNotPerformException {
        connectionManager.deactivate(entry);
        return super.remove(entry);
    }

    @Override
    public Device update(Device entry) throws CouldNotPerformException {
        Device oldEntry = super.get(entry.getId());
        try {
            connectionManager.deactivate(oldEntry).get();
        } catch (InterruptedException | ExecutionException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
        super.update(entry);
        connectionManager.activate(entry);
        return entry;
    }

    @Override
    public Device register(Device entry) throws CouldNotPerformException {
        connectionManager.activate(entry);
        return super.register(entry);
    }
}
