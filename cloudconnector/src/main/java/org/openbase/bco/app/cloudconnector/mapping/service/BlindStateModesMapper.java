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

import org.openbase.bco.app.cloudconnector.mapping.lib.Mode;
import org.openbase.bco.app.cloudconnector.mapping.lib.Setting;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BlindStateType.BlindState;
import org.openbase.type.domotic.state.BlindStateType.BlindState.State;

import java.util.Arrays;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BlindStateModesMapper extends AbstractServiceStateSingleModeMapper<BlindState> {

    private static final String UP_SETTING_NAME = "up";
    private static final String DOWN_SETTING_NAME = "down";
    private static final String STOP_SETTING_NAME = "stop";

    private final Mode mode;
    private final Setting up, down, stop;

    public BlindStateModesMapper() {
        super(ServiceType.BLIND_STATE_SERVICE);

        this.mode = new Mode("movement", false, "bewegung");
        this.up = new Setting(UP_SETTING_NAME, "auf", "nach oben");
        this.down = new Setting(DOWN_SETTING_NAME, "runter", "nach unten");
        this.stop = new Setting(STOP_SETTING_NAME, "stopp", "aufhören");
        this.mode.getSettingList().addAll(Arrays.asList(up, down, stop));
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public String getSetting(BlindState blindState) throws CouldNotPerformException {
        switch (blindState.getValue()) {
            case UP:
                return up.getName();
            case DOWN:
                return down.getName();
            case STOP:
                return stop.getName();
            default:
                throw new NotAvailableException("Setting for blindState[" + blindState.getValue() + "]");
        }
    }

    @Override
    public BlindState getServiceState(final String settingName) throws CouldNotPerformException {
        switch (settingName) {
            case UP_SETTING_NAME:
                return BlindState.newBuilder().setValue(State.UP).build();
            case DOWN_SETTING_NAME:
                return BlindState.newBuilder().setValue(State.DOWN).build();
            case STOP_SETTING_NAME:
                return BlindState.newBuilder().setValue(State.STOP).build();
            default:
                throw new CouldNotPerformException("Could not map setting[" + settingName + "] to BlindState");
        }
    }
}
