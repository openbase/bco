/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.scene.lib.generator;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.extension.protobuf.IdGenerator;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;

/**
 *
 * @author mpohling
 */
public class SceneConfigIdGenerator implements IdGenerator<String, SceneConfig> {

    @Override
    public String generateId(SceneConfig message) throws CouldNotPerformException {
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
