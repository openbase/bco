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

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class Mode extends Named {

    private static final String SETTINGS_KEY = "settings";
    private static final String ORDERED_KEY = "ordered";

    private final List<Setting> settingList;
    private final boolean ordered;

    public Mode(final String name, final boolean ordered) {
        super(name);
        this.ordered = ordered;
        this.settingList = new ArrayList<>();
    }

    public Mode(final String name, final boolean ordered, final String... germanSynonyms) {
        super(name, germanSynonyms);
        this.ordered = ordered;
        this.settingList = new ArrayList<>();
    }

    @Override
    public JsonObject toJson() throws CouldNotPerformException {
        final JsonObject jsonObject = super.toJson();
        jsonObject.addProperty(ORDERED_KEY, isOrdered());

        final JsonArray settings = new JsonArray();
        for (final Setting setting : getSettingList()) {
            settings.add(setting.toJson());
        }
        jsonObject.add(SETTINGS_KEY, settings);

        return jsonObject;
    }

    public List<Setting> getSettingList() {
        return settingList;
    }

    public boolean isOrdered() {
        return ordered;
    }
}
