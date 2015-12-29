/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.agm.lib.generator;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.extension.protobuf.IdGenerator;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;

/**
 *
 * @author mpohling
 */
public class AgentConfigIdGenerator implements IdGenerator<String, AgentConfig> {

    @Override
    public String generateId(AgentConfig message) throws CouldNotPerformException {
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
