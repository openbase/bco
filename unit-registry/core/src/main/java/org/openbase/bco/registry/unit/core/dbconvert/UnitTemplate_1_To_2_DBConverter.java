package org.openbase.bco.registry.unit.core.dbconvert;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.io.File;
import java.util.Map;
import org.openbase.bco.registry.unit.lib.generator.UnitTemplateIdGenerator;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitTemplate_1_To_2_DBConverter extends AbstractDBVersionConverter {

    private static final String ID_FIELD = "id";

    private final UnitTemplateIdGenerator idGenerator;

    public UnitTemplate_1_To_2_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
        idGenerator = new UnitTemplateIdGenerator();
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        if (outdatedDBEntry.has(ID_FIELD)) {
            outdatedDBEntry.remove(ID_FIELD);
            outdatedDBEntry.addProperty(ID_FIELD, idGenerator.generateId(null));
        }
        return outdatedDBEntry;
    }
}
