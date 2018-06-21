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
public class Setting {

    private final String name;
    private final Map<String, List<String>> languageSynonymMap;

    public Setting(final String name) {
        this.name = name;
        this.languageSynonymMap = new HashMap<>();
    }

    public Setting(final String name, final String... germanSynonyms) {
        this(name);
        languageSynonymMap.put(Locale.GERMAN.getLanguage(), Arrays.asList(germanSynonyms));
    }

    public String getName() {
        return name;
    }

    public Map<String, List<String>> getLanguageSynonymMap() {
        return languageSynonymMap;
    }
}
