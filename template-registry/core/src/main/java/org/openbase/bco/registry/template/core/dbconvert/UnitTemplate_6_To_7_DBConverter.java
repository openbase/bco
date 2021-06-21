package org.openbase.bco.registry.template.core.dbconvert;

/*-
 * #%L
 * BCO Registry Template Core
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

import com.google.gson.JsonObject;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import org.openbase.jul.storage.registry.version.GenericRenameFieldDBVersionConverter;

import java.io.File;
import java.util.Map;

/**
 * This db converter renames the unit template include field into super_type.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitTemplate_6_To_7_DBConverter extends GenericRenameFieldDBVersionConverter {

    public UnitTemplate_6_To_7_DBConverter(final DBVersionControl versionControl) {
        super(versionControl, "unit_type", "type");
    }
}
