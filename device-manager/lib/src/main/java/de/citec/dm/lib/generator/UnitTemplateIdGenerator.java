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
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class UnitTemplateIdGenerator implements IdGenerator<String, UnitTemplate>{
    
    @Override
    public String generateId(final UnitTemplate message) throws CouldNotPerformException {
        String id;
        try {
            if (!message.hasType()) {
                throw new InvalidStateException("Field [UnitType] is missing!");
            }
            return StringProcessor.transformToIdString(message.getType().name());
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }
}
