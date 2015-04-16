
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.storage.registry.Registry;
import java.util.HashMap;

/**
 *
 * @author mpohling
 */
public class DeviceRegistry extends Registry<String, Device> {

    public DeviceRegistry() throws InstantiationException {
    }

    public DeviceRegistry(HashMap<String, Device> entryMap) throws InstantiationException {
        super(entryMap);
    }
}
