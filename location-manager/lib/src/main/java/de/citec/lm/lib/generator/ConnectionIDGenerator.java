/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.lib.generator;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.protobuf.IdGenerator;
import java.util.UUID;
import rst.spatial.ConnectionConfigType.ConnectionConfig;

/**
 *
 * @author mpohling
 */
public class ConnectionIDGenerator implements IdGenerator<String, ConnectionConfig> {

    @Override
    public String generateId(ConnectionConfig message) throws CouldNotPerformException {
        return UUID.randomUUID().toString();
    }
}
