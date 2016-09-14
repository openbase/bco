package org.openbase.bco.registry.device.lib.generator;

/*
 * #%L
 * REM DeviceRegistry Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.extension.protobuf.IdGenerator;
import org.openbase.jul.processing.StringProcessor;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class UnitTemplateIdGenerator implements IdGenerator<String, UnitTemplate> {
    
    @Override
    public String generateId(final UnitTemplate message) throws CouldNotPerformException {
        String id;
        try {
            if (!message.hasType()) {
                throw new InvalidStateException("Field [UnitType] is missing!");
            }
            return generateId(message.getType());
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate id!", ex);
        }
    }
    
    public String generateId(final UnitType type) {
        return StringProcessor.transformToIdString(type.name());
    }
}
