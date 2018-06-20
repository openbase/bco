package org.openbase.bco.app.cloud.connector.mapping.lib;

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
