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
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BatteryStateModesMapper extends AbstractServiceStateProviderSingleModeMapper<BatteryState> {

    private final Mode mode;
    private final Setting okay, critical, insufficient, unknown;

    public BatteryStateModesMapper() {
        super(ServiceType.BATTERY_STATE_SERVICE);

        this.mode = new Mode("battery", true, "batterie", "akku");
        this.mode.getLanguageSynonymMap().put(Locale.ENGLISH.getLanguage(), Arrays.asList("battery", "charge"));
        this.critical = new Setting("critical", "kritisch", "gering");
        this.critical.getLanguageSynonymMap().put(Locale.ENGLISH.getLanguage(), Arrays.asList("critical", "very low"));
        this.insufficient = new Setting("insufficient", "ungenügend");
        this.insufficient.getLanguageSynonymMap().put(Locale.ENGLISH.getLanguage(), Arrays.asList("insufficient", "not enough"));
        this.okay = new Setting("okay", "okay", "gut");
        this.okay.getLanguageSynonymMap().put(Locale.ENGLISH.getLanguage(), Collections.singletonList("okay"));
        this.unknown = new Setting("unknown", "unbekannt", "nicht bekannt");
        this.unknown.getLanguageSynonymMap().put(Locale.ENGLISH.getLanguage(), Collections.singletonList("unknown"));
        this.mode.getSettingList().addAll(Arrays.asList(critical, insufficient, okay, unknown));
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public String getSetting(final BatteryState batteryState) {
        switch (batteryState.getValue()) {
            case OK:
                return okay.getName();
            case CRITICAL:
                return critical.getName();
            case INSUFFICIENT:
                return insufficient.getName();
            default:
                return unknown.getName();
        }
    }
}
