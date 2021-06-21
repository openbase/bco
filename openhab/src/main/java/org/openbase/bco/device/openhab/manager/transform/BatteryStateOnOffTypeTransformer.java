package org.openbase.bco.device.openhab.manager.transform;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.TypeNotSupportedException;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState.State;

/**
 * Battery state transformer for battery states only displaying low battery.
 * If the onOffType is on it means the battery is critical and else okay.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BatteryStateOnOffTypeTransformer implements ServiceStateCommandTransformer<BatteryState, OnOffType> {

    @Override
    public BatteryState transform(final OnOffType command) throws CouldNotTransformException, TypeNotSupportedException {
        switch (command) {
            case OFF:
                return BatteryState.newBuilder().setValue(State.OK).setLevel(1).build();
            case ON:
                return BatteryState.newBuilder().setValue(State.CRITICAL).setLevel(0.05).build();
            default:
                throw new TypeNotSupportedException("OnOffType[" + command + "]");
        }
    }

    @Override
    public OnOffType transform(final BatteryState serviceState) throws CouldNotTransformException, TypeNotSupportedException {
        switch (serviceState.getValue()) {
            case CRITICAL:
                return OnOffType.ON;
            default:
                return OnOffType.OFF;
        }
    }
}
