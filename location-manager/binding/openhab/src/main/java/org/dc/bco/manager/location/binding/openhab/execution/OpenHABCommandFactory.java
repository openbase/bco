/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.location.binding.openhab.execution;

/*
 * #%L
 * COMA LocationManager Binding OpenHAB
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

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.openhab.binding.execution.AbstractOpenHABCommandFactory;
import static org.dc.jul.extension.openhab.binding.execution.AbstractOpenHABCommandFactory.getCommandBuilder;
import org.dc.jul.extension.openhab.binding.transform.HSVColorTransformer;
import org.dc.jul.extension.openhab.binding.transform.PowerStateTransformer;
import org.dc.jul.extension.openhab.binding.transform.StopMoveStateTransformer;
import org.dc.jul.extension.openhab.binding.transform.UpDownStateTransformer;
import rst.homeautomation.openhab.HSBType;
import rst.homeautomation.openhab.OnOffHolderType;
import rst.homeautomation.openhab.OpenhabCommandType;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand.CommandType;
import rst.homeautomation.openhab.PercentType;
import rst.homeautomation.openhab.StopMoveHolderType;
import rst.homeautomation.openhab.UpDownHolderType;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.state.ShutterStateType.ShutterState;
import rst.vision.HSVColorType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class OpenHABCommandFactory extends AbstractOpenHABCommandFactory{
    
    public static OpenhabCommandType.OpenhabCommand.Builder newHSBCommand(final HSVColorType.HSVColor color) throws CouldNotPerformException {
        return newHSBCommand(HSVColorTransformer.transform(color));
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

    public static OpenhabCommandType.OpenhabCommand.Builder newStopMoveCommand(final ShutterState.State state) throws CouldNotPerformException {
        return newStopMoveCommand(StopMoveStateTransformer.transform(state));
    }
    
    public static OpenhabCommandType.OpenhabCommand.Builder newStopMoveCommand(final StopMoveHolderType.StopMoveHolder state) {
        return getCommandBuilder().setType(CommandType.STOPMOVE).setStopMove(state);
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newUpDownCommand(final ShutterState.State state) throws CouldNotPerformException {
        return newUpDownCommand(UpDownStateTransformer.transform(state));
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newUpDownCommand(final UpDownHolderType.UpDownHolder state) {
        return getCommandBuilder().setType(CommandType.UPDOWN).setUpDown(state);
    }
    
    public static OpenhabCommandType.OpenhabCommand.Builder newDecimalCommand(final Double value) {
        return getCommandBuilder().setType(CommandType.DECIMAL).setDecimal(value);
    }
}
