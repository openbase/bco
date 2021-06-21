package org.openbase.bco.app.cloudconnector.mapping.service;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import org.openbase.bco.app.cloudconnector.mapping.lib.Mode;
import org.openbase.bco.app.cloudconnector.mapping.lib.Setting;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState;

import java.util.*;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class EmphasisStateModesMapper extends AbstractServiceStateModesMapper<EmphasisState> {

    private static final String SETTING_NAME_LOW = "low";
    private static final String SETTING_NAME_MEDIUM = "medium";
    private static final String SETTING_NAME_HIGH = "high";

    private static final String MODE_NAME_COMFORT = "comfort";
    private static final String MODE_NAME_ECONOMY = "economy";
    private static final String MODE_NAME_SECURITY = "security";

    private final List<Mode> modeList;
    private final Mode comfort, energy, security;
    private final Setting low, medium, high;

    public EmphasisStateModesMapper() {
        super(ServiceType.EMPHASIS_STATE_SERVICE);

        this.low = new Setting(SETTING_NAME_LOW, "niedrig", "gering");
        this.medium = new Setting(SETTING_NAME_MEDIUM, "mittel");
        this.high = new Setting(SETTING_NAME_HIGH, "hoch");

        this.comfort = new Mode(MODE_NAME_COMFORT, true, "comfort");
        this.comfort.getSettingList().addAll(Arrays.asList(low, medium, high));
        this.energy = new Mode(MODE_NAME_ECONOMY, true, "economy");
        this.energy.getSettingList().addAll(Arrays.asList(low, medium, high));
        this.security = new Mode(MODE_NAME_SECURITY, true, "sicherheit");
        this.security.getSettingList().addAll(Arrays.asList(low, medium, high));

        modeList = new ArrayList<>();
        modeList.addAll(Arrays.asList(comfort, energy, security));
    }

    @Override
    public EmphasisState getServiceState(final Map<String, String> modeNameSettingNameMap) throws CouldNotPerformException {
        final EmphasisState.Builder emphasisState = EmphasisState.newBuilder();

        if (modeNameSettingNameMap.containsKey(MODE_NAME_COMFORT)) {
            emphasisState.setComfort(getValueForSetting(modeNameSettingNameMap.get(MODE_NAME_COMFORT)));
        }

        if (modeNameSettingNameMap.containsKey(MODE_NAME_ECONOMY)) {
            emphasisState.setEconomy(getValueForSetting(modeNameSettingNameMap.get(MODE_NAME_ECONOMY)));
        }

        if (modeNameSettingNameMap.containsKey(MODE_NAME_SECURITY)) {
            emphasisState.setSecurity(getValueForSetting(modeNameSettingNameMap.get(MODE_NAME_SECURITY)));
        }

        return emphasisState.build();
    }

    @Override
    public Map<String, String> getSettings(final EmphasisState emphasisState) {
        final Map<String, String> modeNameSettingMap = new HashMap<>();
        modeNameSettingMap.put(MODE_NAME_COMFORT, getSettingForValue(emphasisState.getComfort()));
        modeNameSettingMap.put(MODE_NAME_ECONOMY, getSettingForValue(emphasisState.getEconomy()));
        modeNameSettingMap.put(MODE_NAME_SECURITY, getSettingForValue(emphasisState.getSecurity()));
        return modeNameSettingMap;
    }

    @Override
    public List<Mode> getModes() {
        return modeList;
    }

    private double getValueForSetting(final String settingName) throws CouldNotPerformException {
        switch (settingName) {
            case SETTING_NAME_LOW:
                return 0;
            case SETTING_NAME_MEDIUM:
                return 50;
            case SETTING_NAME_HIGH:
                return 100;
            default:
                throw new CouldNotPerformException("Could not get value for setting[" + settingName + "]");
        }
    }

    private String getSettingForValue(final double value) {
        if (value < 30) {
            return SETTING_NAME_LOW;
        } else if (value < 70) {
            return SETTING_NAME_MEDIUM;
        } else {
            return SETTING_NAME_HIGH;
        }
    }
}
