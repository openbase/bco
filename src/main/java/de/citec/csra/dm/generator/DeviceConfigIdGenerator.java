/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.generator;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.extension.rsb.util.IdGenerator;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author mpohling
 */
public class DeviceConfigIdGenerator implements IdGenerator<String, DeviceConfig>{

    @Override
    public String generateId(DeviceConfig message) throws CouldNotPerformException {
        try {
            if (!message.hasDeviceClass()) {
                throw new InvalidStateException("Field [DeviceClass] is missing!");
            }

            if (!message.getDeviceClass().hasId()) {
                throw new InvalidStateException("Field [DeviceClass.id] is missing!");
            }

            if (message.getDeviceClass().getId().isEmpty()) {
                throw new InvalidStateException("Field [DeviceClass.id] is empty!");
            }

            if (!message.hasSerialNumber()) {
                throw new InvalidStateException("Field [SerialNumber] is missing!");
            }

            if (message.getSerialNumber().isEmpty()) {
                throw new InvalidStateException("Field [SerialNumber] is empty!");
            }

            String id;

            id = message.getDeviceClass().getId();
            id += "_";
            id += message.getSerialNumber();
            return id;

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }
    
}
