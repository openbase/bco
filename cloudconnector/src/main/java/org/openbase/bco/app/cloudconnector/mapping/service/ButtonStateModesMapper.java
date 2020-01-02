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
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ButtonStateType.ButtonState;

import java.util.Arrays;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ButtonStateModesMapper extends AbstractServiceStateProviderSingleModeMapper<ButtonState> {

    private final Mode mode;
    private final Setting released, pressed, doublePressed;

    public ButtonStateModesMapper() {
        super(ServiceType.BUTTON_STATE_SERVICE);

        this.mode = new Mode("pressed", false, "drück");
        this.released = new Setting("released", "losgelassen");
        this.pressed = new Setting("pressed", "gedrückt");
        this.doublePressed = new Setting("double pressed", "doppelklick");
        this.mode.getSettingList().addAll(Arrays.asList(released, pressed, doublePressed));
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public String getSetting(ButtonState buttonState) throws CouldNotPerformException {
        switch (buttonState.getValue()) {
            case RELEASED:
                return released.getName();
            case PRESSED:
                return pressed.getName();
            case DOUBLE_PRESSED:
                return doublePressed.getName();
            default:
                throw new CouldNotPerformException("Could not get setting for button state[" + buttonState.getValue().name() + "]");
        }
    }
}
