package org.dc.bco.manager.device.binding.openhab.transform;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.TypeNotSupportedException;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.openhab.OnOffHolderType;

/**
 *
 * @author mpohling
 */
public class PowerStateTransformer {

    public static PowerState transform(OnOffHolderType.OnOffHolder.OnOff onOffType) throws CouldNotTransformException {
        switch (onOffType) {
            case OFF:
                return PowerState.newBuilder().setValue(PowerState.State.OFF).build();
            case ON:
                return PowerState.newBuilder().setValue(PowerState.State.ON).build();
            default:
                throw new CouldNotTransformException("Could not transform " + OnOffHolderType.OnOffHolder.OnOff.class.getName() + "! " + OnOffHolderType.OnOffHolder.OnOff.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
        }
    }

    public static OnOffHolderType.OnOffHolder transform(PowerState.State powerState) throws TypeNotSupportedException, CouldNotTransformException {
        switch (powerState) {
            case OFF:
                return OnOffHolderType.OnOffHolder.newBuilder().setState(OnOffHolderType.OnOffHolder.OnOff.OFF).build();
            case ON:
                return OnOffHolderType.OnOffHolder.newBuilder().setState(OnOffHolderType.OnOffHolder.OnOff.ON).build();
            case UNKNOWN:
                throw new TypeNotSupportedException(powerState, OnOffHolderType.OnOffHolder.OnOff.class);
            default:
                throw new CouldNotTransformException("Could not transform " + PowerState.State.class.getName() + "! " + PowerState.State.class.getSimpleName() + "[" + powerState.name() + "] is unknown!");
        }
    }
}
