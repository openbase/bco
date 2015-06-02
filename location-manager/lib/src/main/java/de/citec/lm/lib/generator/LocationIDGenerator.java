/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.lib.generator;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.extension.rsb.util.IdGenerator;
import de.citec.jul.processing.StringProcessor;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class LocationIDGenerator implements IdGenerator<String, LocationConfig>{

    @Override
    public String generateId(LocationConfig message) throws CouldNotPerformException {
        try {
            if (!message.hasLabel()) {
                throw new InvalidStateException("Field [locationConfig.label] is missing!");
            }

            if (message.getLabel().isEmpty()) {
                throw new InvalidStateException("Field [Label] is empty!");
            }
            
            return StringProcessor.transformToIdString(message.getLabel());

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }
}
