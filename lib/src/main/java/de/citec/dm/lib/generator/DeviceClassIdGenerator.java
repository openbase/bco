/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.lib.generator;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.extension.rsb.util.IdGenerator;
import rst.homeautomation.device.DeviceClassType.DeviceClass;

/**
 *
 * @author mpohling
 */
public class DeviceClassIdGenerator implements IdGenerator<String, DeviceClass>{
    
    @Override
    public String generateId(DeviceClass message) throws CouldNotPerformException {
        String id;
        try {
            if (!message.hasProductNumber()) {
                throw new InvalidStateException("Field [ProductNumber] is missing!");
            }

            if (message.getProductNumber().isEmpty()) {
                throw new InvalidStateException("Field [ProductNumber] is empty!");
            }

            if (!message.hasCompany()) {
                throw new InvalidStateException("Field [Company] is missing!");
            }

            if (message.getCompany().isEmpty()) {
                throw new InvalidStateException("Field [Company] is empty!");
            }

            id = message.getCompany();
            id += "_";
            id += message.getProductNumber();

            return id;
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }
}
