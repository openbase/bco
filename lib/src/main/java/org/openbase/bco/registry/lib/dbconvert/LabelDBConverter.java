package org.openbase.bco.registry.lib.dbconvert;

/*-
 * #%L
 * BCO Registry Lib
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/**
 * Converter which is able to convert json object from the label string
 * to the new label data type.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LabelDBConverter extends AbstractDBVersionConverter {

    public static final String LABEL_KEY = "label";
    public static final String ENTRY_KEY = "entry";
    public static final String KEY_KEY = "key";
    public static final String VALUE_KEY = "value";

    public static final String LANGUAGE_CODE = Locale.getDefault().getLanguage();

    public LabelDBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        return updateLabel(outdatedDBEntry);
    }

    protected JsonObject updateLabel(final JsonObject jsonObject) {
        if (jsonObject.has(LABEL_KEY)) {
            final String labelString = jsonObject.get(LABEL_KEY).getAsString();

            final JsonObject label = new JsonObject();
            final JsonArray entryList = new JsonArray();
            final JsonObject entry = new JsonObject();

            entry.addProperty(KEY_KEY, LANGUAGE_CODE);
            entry.addProperty(VALUE_KEY, labelString);

            entryList.add(entry);
            label.add(ENTRY_KEY, entryList);

            jsonObject.add(LABEL_KEY, label);
        }

        return jsonObject;
    }
}
