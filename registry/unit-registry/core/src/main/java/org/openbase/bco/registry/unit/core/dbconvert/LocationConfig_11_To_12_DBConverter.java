package org.openbase.bco.registry.unit.core.dbconvert;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.jul.storage.registry.version.DBVersionControl;
import org.openbase.jul.storage.registry.version.GenericRenameFieldDBVersionConverter;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig;

/**
 * Converter which updates locations to the new pose structure.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Divine Threepwood</a>
 */
public class LocationConfig_11_To_12_DBConverter extends GenericRenameFieldDBVersionConverter {

    public LocationConfig_11_To_12_DBConverter(final DBVersionControl versionControl) {
        super(versionControl, "tile_type", "location_config", "tile_config", "type");
    }
}
