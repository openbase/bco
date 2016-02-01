/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.lib.generator;

/*
 * #%L
 * REM DeviceRegistry Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
