package org.openbase.bco.app.cloud.connector.mapping.lib;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 openbase.org
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

import java.util.*;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class Mode {

    private final String name;
    private final Map<String, List<String>> languageSynonymMap;
    private final List<Setting> settingList;
    private final boolean ordered;

    public Mode(final String name, final boolean ordered) {
        this.name = name;
        this.ordered = ordered;
        this.languageSynonymMap = new HashMap<>();
        this.settingList = new ArrayList<>();
    }

    public Mode(final String name, final boolean ordered, final String... germanSynonyms) {
        this(name, ordered);
        this.languageSynonymMap.put(Locale.GERMAN.getLanguage(), Arrays.asList(germanSynonyms));
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

    public String getName() {
        return name;
    }

    public Map<String, List<String>> getLanguageSynonymMap() {
        return languageSynonymMap;
    }

    public List<Setting> getSettingList() {
        return settingList;
    }

    public boolean isOrdered() {
        return ordered;
    }
}
