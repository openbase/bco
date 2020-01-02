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

import com.google.gson.JsonObject;
import com.google.protobuf.Message;
import org.openbase.bco.app.cloudconnector.mapping.lib.Toggle;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractServiceStateTogglesMapper<SERVICE_STATE extends Message> extends AbstractServiceStateTraitMapper<SERVICE_STATE> {

    private static final String CURRENT_TOGGLE_SETTINGS_KEY = "currentToggleSettings";
    private static final String UPDATE_TOGGLE_SETTINGS_KEY = "updateToggleSettings";

    public AbstractServiceStateTogglesMapper(ServiceType serviceType) {
        super(serviceType);
    }

    @Override
    protected SERVICE_STATE map(final JsonObject jsonObject) throws CouldNotPerformException {
        if (!jsonObject.has(UPDATE_TOGGLE_SETTINGS_KEY)) {
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "]. " +
                    "Attribute[" + UPDATE_TOGGLE_SETTINGS_KEY + "] is missing");
        }

        try {
            final JsonObject updateToggleSetting = jsonObject.getAsJsonObject(UPDATE_TOGGLE_SETTINGS_KEY);

            if (updateToggleSetting.has(getToggle().getOn().getName())) {
                final boolean isOn = updateToggleSetting.get(getToggle().getOn().getName()).getAsBoolean();
                return getServiceState(isOn);
            } else {
                final boolean isOff = updateToggleSetting.get(getToggle().getOff().getName()).getAsBoolean();
                return getServiceState(!isOff);
            }
        } catch (ClassCastException | IllegalStateException ex) {
            // expected data type from json do not match, e.g. updateToggleSetting is not a jsonObject
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "]", ex);
        }
    }

    @Override
    public void map(final SERVICE_STATE serviceState, final JsonObject jsonObject) throws CouldNotPerformException {
        final JsonObject currentToggleSetting = new JsonObject();
        final boolean on = isOn(serviceState);
        currentToggleSetting.addProperty(getToggle().getOn().getName(), on);
        currentToggleSetting.addProperty(getToggle().getOff().getName(), !on);
        jsonObject.add(CURRENT_TOGGLE_SETTINGS_KEY, currentToggleSetting);
    }

    @Override
    public void addAttributes(final UnitConfig unitConfig, final JsonObject jsonObject) throws CouldNotPerformException {
        if (jsonObject.has(Toggle.AVAILABLE_TOGGLES_KEY)) {
            throw new CouldNotPerformException("Attributes[" + jsonObject.toString() + "] already contain available toggles");
        }
        jsonObject.add(Toggle.AVAILABLE_TOGGLES_KEY, getToggle().toJson());
    }

    public abstract Toggle getToggle();

    public abstract boolean isOn(final SERVICE_STATE serviceState) throws CouldNotPerformException;

    public abstract SERVICE_STATE getServiceState(final boolean on) throws CouldNotPerformException;
}
