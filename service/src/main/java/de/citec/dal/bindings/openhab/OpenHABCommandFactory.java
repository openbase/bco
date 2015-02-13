/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab;

import de.citec.dal.bindings.openhab.transform.HSVColorTransformer;
import de.citec.dal.bindings.openhab.transform.PowerStateTransformer;
import de.citec.dal.bindings.openhab.transform.StopMoveStateTransformer;
import de.citec.dal.bindings.openhab.transform.UpDownStateTransformer;
import de.citec.jul.exception.CouldNotPerformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.openhab.HSBType;
import rst.homeautomation.openhab.OnOffHolderType;
import rst.homeautomation.openhab.OpenhabCommandType;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand.CommandType;
import rst.homeautomation.openhab.PercentType;
import rst.homeautomation.openhab.StopMoveHolderType;
import rst.homeautomation.openhab.UpDownHolderType;
import rst.homeautomation.states.PowerType;
import rst.homeautomation.states.ShutterType;
import rst.vision.HSVColorType;

/**
 *
 * @author mpohling
 */
public class OpenHABCommandFactory {

    private Logger logger = LoggerFactory.getLogger(getClass());

    public static OpenhabCommandType.OpenhabCommand.Builder getCommandBuilder() {
        return OpenhabCommandType.OpenhabCommand.newBuilder();
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newHSBCommand(final HSVColorType.HSVColor color) throws CouldNotPerformException {
        return newHSBCommand(HSVColorTransformer.transform(color));
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newHSBCommand(final HSBType.HSB color) {
        return getCommandBuilder().setType(CommandType.HSB).setHsb(color);
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newOnOffCommand(final PowerType.Power.PowerState state) throws CouldNotPerformException {
        return newOnOffCommand(PowerStateTransformer.transform(state));
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newOnOffCommand(final OnOffHolderType.OnOffHolder state) {
        return getCommandBuilder().setType(CommandType.ONOFF).setOnOff(state);
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newPercentCommand(final double value) {
        return getCommandBuilder().setType(CommandType.PERCENT).setPercent(PercentType.Percent.newBuilder().setValue((int) (value * 100)).build());
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newPercentCommand(final PercentType.Percent percent) {
        return getCommandBuilder().setType(CommandType.PERCENT).setPercent(percent);
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newStopMoveCommand(final ShutterType.Shutter.ShutterState state) throws CouldNotPerformException {
        return newStopMoveCommand(StopMoveStateTransformer.transform(state));
    }
    
    public static OpenhabCommandType.OpenhabCommand.Builder newStopMoveCommand(final StopMoveHolderType.StopMoveHolder state) {
        return getCommandBuilder().setType(CommandType.STOPMOVE).setStopMove(state);
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newUpDownCommand(final ShutterType.Shutter.ShutterState state) throws CouldNotPerformException {
        return newUpDownCommand(UpDownStateTransformer.transform(state));
    }

    public static OpenhabCommandType.OpenhabCommand.Builder newUpDownCommand(final UpDownHolderType.UpDownHolder state) {
        return getCommandBuilder().setType(CommandType.UPDOWN).setUpDown(state);
    }
}
