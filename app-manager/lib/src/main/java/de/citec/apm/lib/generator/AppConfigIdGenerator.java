/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.apm.lib.generator;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.extension.protobuf.IdGenerator;
import de.citec.jul.processing.StringProcessor;
import rst.homeautomation.control.app.AppConfigType.AppConfig;

/**
 *
 * @author mpohling
 */
public class AppConfigIdGenerator implements IdGenerator<String, AppConfig> {

    @Override
    public String generateId(AppConfig message) throws CouldNotPerformException {
        try {

            if (!message.hasLabel()) {
                throw new InvalidStateException("Field [Label] is missing!");
            }
            
            String id;

            id = message.getLabel();
            return StringProcessor.transformToIdString(id);

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }

}
