/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.usr.lib.generator;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdGenerator;
import java.util.UUID;
import rst.authorization.UserGroupConfigType.UserGroupConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UserGroupConfigIdGenerator implements IdGenerator<String, UserGroupConfig> {

    @Override
    public String generateId(UserGroupConfig message) throws CouldNotPerformException {
        return UUID.randomUUID().toString();
    }
}
