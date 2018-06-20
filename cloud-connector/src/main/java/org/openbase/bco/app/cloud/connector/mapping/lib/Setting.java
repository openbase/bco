package org.openbase.bco.app.cloud.connector.mapping.lib;

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
