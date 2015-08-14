/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.lib.generator;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.extension.protobuf.IdGenerator;
import de.citec.jul.processing.StringProcessor;
import rst.homeautomation.device.DeviceClassType.DeviceClass;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
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

            return StringProcessor.transformToIdString(id);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }
}
