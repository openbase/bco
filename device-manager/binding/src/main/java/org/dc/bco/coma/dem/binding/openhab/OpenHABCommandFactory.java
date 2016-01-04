/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.dem.binding.openhab;

import org.dc.bco.coma.dem.binding.openhab.transform.HSVColorTransformer;
import org.dc.bco.coma.dem.binding.openhab.transform.PowerStateTransformer;
import org.dc.bco.coma.dem.binding.openhab.transform.StopMoveStateTransformer;
import org.dc.bco.coma.dem.binding.openhab.transform.UpDownStateTransformer;
import org.dc.jul.exception.CouldNotPerformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @author mpohling
 */
public class OpenHABCommandFactory {

    private final Logger logger = LoggerFactory.getLogger(OpenHABCommandFactory.class);

    public static OpenhabCommandType.OpenhabCommand.Builder getCommandBuilder() {
        return OpenhabCommandType.OpenhabCommand.newBuilder();
    }

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
