/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.registry;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.rsb.IdGenerator;
import rst.homeautomation.device.DeviceClassType.DeviceClass;

/**
 *
 * @author mpohling
 */
public class DeviceClassIdGenerator implements IdGenerator<String, DeviceClass>{
    
    @Override
    public String generateId(DeviceClass message) throws CouldNotPerformException {
        try {
            if (!message.hasLabel()) {
                throw new InvalidStateException("Field [Label] is missing!");
            }
            return message.getLabel();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }
}
