/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.usr.lib.generator;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.extension.protobuf.IdGenerator;
import java.util.UUID;
import rst.authorization.UserConfigType.UserConfig;

/**
 *
 * @author mpohling
 */
public class UserConfigIdGenerator implements IdGenerator<String, UserConfig> {

    @Override
    public String generateId(UserConfig message) throws CouldNotPerformException {
        return UUID.randomUUID().toString();
    }
}
