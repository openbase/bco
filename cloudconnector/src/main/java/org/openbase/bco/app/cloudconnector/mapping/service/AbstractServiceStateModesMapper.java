package org.openbase.bco.app.cloudconnector.mapping.service;

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
import com.google.protobuf.Message;
import org.openbase.bco.app.cloudconnector.mapping.lib.Mode;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractServiceStateModesMapper<SERVICE_STATE extends Message> extends AbstractServiceStateTraitMapper<SERVICE_STATE> {

    private static final String AVAILABLE_MODES_KEY = "availableModes";

    private static final String UPDATE_MODE_SETTINGS_KEY = "updateModeSettings";
    private static final String CURRENT_MODE_SETTINGS_KEY = "currentModeSettings";

    AbstractServiceStateModesMapper(ServiceType serviceType) {
        super(serviceType);
    }

    @Override
    protected SERVICE_STATE map(JsonObject jsonObject) throws CouldNotPerformException {
        if (!jsonObject.has(UPDATE_MODE_SETTINGS_KEY)) {
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "]. " +
                    "Attribute[" + UPDATE_MODE_SETTINGS_KEY + "] is missing");
        }

        try {
            final JsonObject updateModeSettings = jsonObject.getAsJsonObject(UPDATE_MODE_SETTINGS_KEY);

            final Map<String, String> modeNameSettingNameMap = new HashMap<>();
            for (final Mode mode : getModes()) {
                if (updateModeSettings.has(mode.getName())) {
                    modeNameSettingNameMap.put(mode.getName(), updateModeSettings.get(mode.getName()).getAsString());
                }
            }
            if (modeNameSettingNameMap.isEmpty()) {
                throw new NotAvailableException("Supported mode in [" + updateModeSettings.toString() + "]");
            }

            return getServiceState(modeNameSettingNameMap);
        } catch (ClassCastException | IllegalStateException ex) {
            // expected data type from json do not match, e.g. updateModeSettings is not a jsonObject
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "]", ex);
        }
    }

    @Override
    public void map(SERVICE_STATE serviceState, JsonObject jsonObject) throws CouldNotPerformException {
        final JsonObject currentModeSettings;
        if (jsonObject.has(CURRENT_MODE_SETTINGS_KEY)) {
            currentModeSettings = jsonObject.getAsJsonObject(CURRENT_MODE_SETTINGS_KEY);
        } else {
            currentModeSettings = new JsonObject();
            jsonObject.add(CURRENT_MODE_SETTINGS_KEY, currentModeSettings);
        }

        final Map<String, String> modeSettingMap = getSettings(serviceState);
        if (modeSettingMap.isEmpty()) {
            throw new CouldNotPerformException("Could not resolve mode settings for serviceState[" + serviceState + "] of serviceType[" + getServiceType().name() + "]");
        }

        for (final Entry<String, String> entry : modeSettingMap.entrySet()) {
            currentModeSettings.addProperty(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void addAttributes(final UnitConfig unitConfig, final JsonObject jsonObject) throws CouldNotPerformException {
        final JsonArray availableModes;
        if (jsonObject.has(AVAILABLE_MODES_KEY)) {
            availableModes = jsonObject.getAsJsonArray(AVAILABLE_MODES_KEY);
        } else {
            availableModes = new JsonArray();
            jsonObject.add(AVAILABLE_MODES_KEY, availableModes);
        }

        final List<Mode> modeList = getModes();

        if (modeList.isEmpty()) {
            throw new FatalImplementationErrorException("ModeList is empty", this);
        }

        for (final Mode mode : modeList) {
            if (mode.getName().isEmpty()) {
                throw new FatalImplementationErrorException("Mode without a name", this);
            }

            if (mode.getSettingList().size() < 2) {
                throw new FatalImplementationErrorException("Mode[" + mode.getName() + "] has only one setting", this);
            }

            availableModes.add(mode.toJson());
        }
    }

    public abstract SERVICE_STATE getServiceState(final Map<String, String> modeNameSettingNameMap) throws CouldNotPerformException;

    public abstract Map<String, String> getSettings(final SERVICE_STATE service_state) throws CouldNotPerformException;

    public abstract List<Mode> getModes();
}
