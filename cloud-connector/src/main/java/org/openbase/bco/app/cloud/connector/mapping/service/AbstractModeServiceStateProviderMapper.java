package org.openbase.bco.app.cloud.connector.mapping.service;

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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.Message;
import org.openbase.bco.app.cloud.connector.mapping.lib.Mode;
import org.openbase.bco.app.cloud.connector.mapping.lib.Setting;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractModeServiceStateProviderMapper<SERVICE_STATE extends Message> extends AbstractServiceStateMapper<SERVICE_STATE> {

    public static final String AVAILABLE_MODES_KEY = "availableModes";
    public static final String NAME_KEY = "name";
    public static final String NAME_VALUES_KEY = "name_values";
    public static final String NAME_SYNONYM_KEY = "name_synonym";
    public static final String LANGUAGE_KEY = "lang";
    public static final String SETTINGS_KEY = "settings";
    public static final String SETTING_NAME_KEY = "setting_name";
    public static final String SETTING_VALUES_KEY = "setting_values";
    public static final String SETTING_SYNONYM_KEY = "setting_synonym";
    public static final String ORDERED_KEY = "ordered";

    public static final String CURRENT_MODE_SETTINGS_KEY = "currentModeSettings";

    public AbstractModeServiceStateProviderMapper(ServiceType serviceType) {
        super(serviceType);
    }

    @Override
    protected SERVICE_STATE map(JsonObject jsonObject) throws CouldNotPerformException {
        throw new CouldNotPerformException("Setting mode not supported for [" + getServiceType().name() + "]");
    }

    @Override
    public void map(SERVICE_STATE serviceState, JsonObject jsonObject) throws CouldNotPerformException {
        final JsonObject currentModeSettings = new JsonObject();
        final Map<String, String> modeSettingMap = getSettings(serviceState);
        if (modeSettingMap.isEmpty()) {
            throw new CouldNotPerformException("Could not resolve mode settings for serviceState[" + serviceState + "] of serviceType[" + getServiceType().name() + "]");
        }

        for (final Entry<String, String> entry : modeSettingMap.entrySet()) {
            currentModeSettings.addProperty(entry.getKey(), entry.getValue());
        }
        jsonObject.add(CURRENT_MODE_SETTINGS_KEY, currentModeSettings);
    }

    @Override
    public void addAttributes(final UnitConfig unitConfig, final JsonObject jsonObject) {
        final JsonArray availableModes = new JsonArray();

        final List<Mode> modeList = getModes();

        if (modeList.isEmpty()) {
            // programming error...
        }

        for (final Mode mode : modeList) {
            final JsonObject modeJson = new JsonObject();

            if (mode.getSettingList().size() < 2) {
                // programming error
            }

            modeJson.addProperty(NAME_KEY, mode.getName());
            modeJson.addProperty(ORDERED_KEY, mode.isOrdered());
            modeJson.add(NAME_VALUES_KEY, synonymMapToJsonArray(mode.getLanguageSynonymMap(), NAME_SYNONYM_KEY));

            final JsonArray settings = new JsonArray();
            for (final Setting setting : mode.getSettingList()) {
                final JsonObject settingJson = new JsonObject();
                settingJson.addProperty(SETTING_NAME_KEY, setting.getName());
                settingJson.add(SETTING_VALUES_KEY, synonymMapToJsonArray(setting.getLanguageSynonymMap(), SETTING_SYNONYM_KEY));
                settings.add(settingJson);
            }
            modeJson.add(SETTINGS_KEY, settings);

            availableModes.add(modeJson);
        }
        jsonObject.add(AVAILABLE_MODES_KEY, availableModes);
    }

    private JsonArray synonymMapToJsonArray(final Map<String, List<String>> languageSynonymMap, final String synonymKey) {
        final JsonArray nameValues = new JsonArray();
        for (final Entry<String, List<String>> entry : languageSynonymMap.entrySet()) {
            final JsonObject nameValue = new JsonObject();
            final JsonArray nameSynonyms = new JsonArray();
            for (final String synonym : entry.getValue()) {
                nameSynonyms.add(synonym);
            }
            nameValue.add(synonymKey, nameSynonyms);
            nameValue.addProperty(LANGUAGE_KEY, entry.getKey());

            nameValues.add(nameValue);
        }
        return nameValues;
    }

    public abstract Map<String, String> getSettings(final SERVICE_STATE service_state) throws CouldNotPerformException;

    public abstract List<Mode> getModes();
}
