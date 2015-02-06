/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab;

import de.citec.dal.data.transform.HSVColorTransformer;
import de.citec.dal.data.transform.PowerStateTransformer;
import de.citec.jul.exception.CouldNotPerformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.openhab.HSBType;
import rst.homeautomation.openhab.OnOffHolderType;
import rst.homeautomation.openhab.OpenhabCommandType;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand.CommandType;
import rst.homeautomation.states.PowerType;
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
    
}