package org.openbase.bco.app.cloudconnector.mapping.lib;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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
import org.openbase.jul.exception.FatalImplementationErrorException;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class Named {

    private static final String NAME_KEY = "name";
    private static final String NAME_VALUES_KEY = "name_values";
    private static final String NAME_SYNONYM_KEY = "name_synonym";
    private static final String LANGUAGE_KEY = "lang";

    private final String name;
    private final Map<String, List<String>> languageSynonymMap;

    public Named(final String name) {
        this.name = name;
        this.languageSynonymMap = new HashMap<>();
    }

    public Named(final String name, final String... germanSynonyms) {
        this(name);
        languageSynonymMap.put(Locale.GERMAN.getLanguage(), Arrays.asList(germanSynonyms));
    }

    public void addLanguageSynonyms(final Locale locale, final String... synonyms) {
        if (languageSynonymMap.containsKey(locale.getLanguage())) {
            final List<String> synonymList = languageSynonymMap.get(locale.getLanguage());
            for (final String synonym : synonyms) {
                if (!synonymList.contains(synonym)) {
                    synonymList.add(synonym);
                }
            }
        } else {
            languageSynonymMap.put(locale.getLanguage(), Arrays.asList(synonyms));
        }
    }

    public JsonObject toJson() throws CouldNotPerformException {
        return toJson(NAME_KEY, NAME_VALUES_KEY, NAME_SYNONYM_KEY);
    }

    protected JsonObject toJson(final String nameKey, final String nameValuesKey, final String nameSynonymKey) throws CouldNotPerformException {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(nameKey, getName());
        jsonObject.add(nameValuesKey, synonymMapToJsonArray(nameSynonymKey));
        return jsonObject;
    }

    public JsonArray synonymMapToJsonArray(final String synonymKey) throws CouldNotPerformException {
        if (languageSynonymMap.isEmpty()) {
            throw new FatalImplementationErrorException("Empty language synonym map[" + synonymKey + "]", this);
        }

        final JsonArray nameValues = new JsonArray();
        for (final Entry<String, List<String>> entry : languageSynonymMap.entrySet()) {
            //TODO: validate languageCode (entry.getKey())
            if (entry.getValue().isEmpty() || entry.getValue().get(0).isEmpty()) {
                // skip if list is empty, or first synonym is empty
                continue;
            }

            final JsonObject nameValue = new JsonObject();
            final JsonArray nameSynonyms = new JsonArray();
            for (final String synonym : entry.getValue()) {
                if (synonym.isEmpty()) {
                    continue;
                }

                nameSynonyms.add(synonym);
            }
            nameValue.add(synonymKey, nameSynonyms);
            nameValue.addProperty(LANGUAGE_KEY, entry.getKey());

            nameValues.add(nameValue);
        }

        if (nameValues.size() == 0) {
            throw new FatalImplementationErrorException("Name values could not be extracted from map[" + synonymKey + "]", this);
        }
        return nameValues;
    }

    public String getName() {
        return name;
    }

    public Map<String, List<String>> getLanguageSynonymMap() {
        return languageSynonymMap;
    }
}
