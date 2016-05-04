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
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.extension.protobuf.IdGenerator;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.device.DeviceClassType.DeviceClass;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class DeviceClassIdGenerator implements IdGenerator<String, DeviceClass>{
    
    @Override
    public String generateId(DeviceClass message) throws CouldNotPerformException {
        String id;
        try {
            if (!message.hasProductNumber()) {
                throw new InvalidStateException("Field [ProductNumber] is missing!");
            }

            if (message.getProductNumber().isEmpty()) {
                throw new InvalidStateException("Field [ProductNumber] is empty!");
            }

            if (!message.hasCompany()) {
                throw new InvalidStateException("Field [Company] is missing!");
            }

            if (message.getCompany().isEmpty()) {
                throw new InvalidStateException("Field [Company] is empty!");
            }

            id = message.getCompany();
            id += "_";
            id += message.getProductNumber();

            return StringProcessor.transformToIdString(id);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }
}
