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

import org.openbase.bco.app.cloud.connector.mapping.lib.Mode;
import org.openbase.bco.app.cloud.connector.mapping.lib.Setting;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerConsumptionStateType.PowerConsumptionState;

import java.util.Arrays;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerConsumptionServiceModesMapper extends AbstractSingleModeServiceProviderTraitMapper<PowerConsumptionState> {

    private final Mode mode;
    private final Setting low, medium, high;

    public PowerConsumptionServiceModesMapper() {
        super(ServiceType.POWER_CONSUMPTION_STATE_SERVICE);

        this.mode = new Mode("consumption", true, "verbrauch", "stromverbrauch", "energieverbrauch");
        this.low = new Setting("low", "niedrig", "gering");
        this.medium = new Setting("medium", "mittel", "normal");
        this.high = new Setting("high", "hoch", "stark");
        this.mode.getSettingList().addAll(Arrays.asList(low, medium, high));
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public String getSetting(final PowerConsumptionState powerConsumptionState) {
        final double consumption = powerConsumptionState.getConsumption();
        if (consumption < 100) {
            return low.getName();
        } else if (consumption < 500) {
            return medium.getName();
        } else {
            return high.getName();
        }
    }
}
