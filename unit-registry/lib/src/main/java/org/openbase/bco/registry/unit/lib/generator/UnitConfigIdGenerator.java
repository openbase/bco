package org.openbase.bco.registry.unit.lib.generator;

/*
 * #%L
 * BCO Registry Unit Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.registry.lib.generator.UUIDGenerator;
import org.openbase.jps.core.JPService;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitConfigIdGenerator extends UUIDGenerator<UnitConfig> {

    @Override
    public String generateId(UnitConfig message) {
        if (JPService.testMode()) {
            return StringProcessor.transformUpperCaseToCamelCase(message.getType().name()) + ":" + super.generateId(message);
        }
        return super.generateId(message);
    }
}
