/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.lib.generator;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.extension.protobuf.IdGenerator;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class UnitConfigIdGenerator implements IdGenerator<String, UnitConfigType.UnitConfig> {

    private static UnitConfigIdGenerator instance;

    private UnitConfigIdGenerator() {
    }

    public static synchronized UnitConfigIdGenerator getInstance() {
        if (instance == null) {
            instance = new UnitConfigIdGenerator();
        }
        return instance;
    }

    @Override
    public String generateId(UnitConfigType.UnitConfig unitConfig) throws CouldNotPerformException {
        try {
            if (!unitConfig.hasScope()) {
                throw new NotAvailableException("unitconfig.scope");
            }
            return ScopeGenerator.generateStringRep(unitConfig.getScope());
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate unit id!", ex);
        }
    }
}
