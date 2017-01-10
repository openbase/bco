package org.openbase.bco.registry.unit.lib.generator;

/*
 * #%L
 * BCO Registry Unit Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.extension.protobuf.IdGenerator;
import rst.domotic.unit.app.AppConfigType.AppConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @deprecated use unit config id generator instead
 */
@Deprecated
public class AppConfigIdGenerator implements IdGenerator<String, AppConfig> {

    @Override
    public String generateId(AppConfig message) throws CouldNotPerformException {
        throw new NotSupportedException("generateId", this);
//        try {
//
//            if (!message.hasLabel()) {
//                throw new InvalidStateException("Field [Label] is missing!");
//            }
//            
//            String id;
//
//            id = message.getLabel();
//            return StringProcessor.transformToIdString(id);
//
//        } catch (CouldNotPerformException ex) {
//            throw new CouldNotPerformException("Could not generate id!", ex);
//        }
    }

}
