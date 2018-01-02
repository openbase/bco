package org.openbase.bco.manager.device.binding.openhab.execution;

/*
 * #%L
 * BCO Manager Device Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.openhab.binding.execution.AbstractOpenHABCommandFactory;
import org.openbase.jul.extension.openhab.binding.transform.HSBColorTransformer;
import org.openbase.jul.extension.openhab.binding.transform.PowerStateTransformer;
import org.openbase.jul.extension.openhab.binding.transform.StopMoveStateTransformer;
import org.openbase.jul.extension.openhab.binding.transform.UpDownStateTransformer;
import rst.domotic.binding.openhab.HSBType;
import rst.domotic.binding.openhab.OnOffHolderType;
import rst.domotic.binding.openhab.OpenhabCommandType;
import rst.domotic.binding.openhab.OpenhabCommandType.OpenhabCommand.CommandType;
import rst.domotic.binding.openhab.PercentType;
import rst.domotic.binding.openhab.StopMoveHolderType;
import rst.domotic.binding.openhab.UpDownHolderType;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.vision.HSBColorType.HSBColor;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class OpenHABCommandFactory extends AbstractOpenHABCommandFactory {

    public static OpenhabCommandType.OpenhabCommand.Builder newHSBCommand(final HSBColor color) throws CouldNotPerformException {
        return newHSBCommand(HSBColorTransformer.transform(color));
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newHSBCommand(final HSBType.HSB color) {
        return getCommandBuilder().setType(CommandType.HSB).setHsb(color);
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newOnOffCommand(final PowerState.State state) throws CouldNotPerformException {
        return newOnOffCommand(PowerStateTransformer.transform(state));
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newOnOffCommand(final OnOffHolderType.OnOffHolder state) {
        return getCommandBuilder().setType(CommandType.ONOFF).setOnOff(state);
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newPercentCommand(final Double value) {
        return getCommandBuilder().setType(CommandType.PERCENT).setPercent(PercentType.Percent.newBuilder().setValue(value.intValue()).build());
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newPercentCommand(final PercentType.Percent percent) {
        return getCommandBuilder().setType(CommandType.PERCENT).setPercent(percent);
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newStopMoveCommand(final BlindState state) throws CouldNotPerformException {
        return newStopMoveCommand(StopMoveStateTransformer.transform(state));
    }
    
    public static OpenhabCommandType.OpenhabCommand.Builder newStopMoveCommand(final StopMoveHolderType.StopMoveHolder state) {
        return getCommandBuilder().setType(CommandType.STOPMOVE).setStopMove(state);
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newUpDownCommand(final BlindState state) throws CouldNotPerformException {
        return newUpDownCommand(UpDownStateTransformer.transform(state));
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newUpDownCommand(final UpDownHolderType.UpDownHolder state) {
        return getCommandBuilder().setType(CommandType.UPDOWN).setUpDown(state);
    }
    
    public static OpenhabCommandType.OpenhabCommand.Builder newDecimalCommand(final Double value) {
        return getCommandBuilder().setType(CommandType.DECIMAL).setDecimal(value);
    }
}
