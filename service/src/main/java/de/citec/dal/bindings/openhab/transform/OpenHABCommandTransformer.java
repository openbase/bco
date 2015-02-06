/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import de.citec.jul.exception.CouldNotTransformException;
import rst.homeautomation.openhab.OpenhabCommandType;

/**
 *
 * @author mpohling
 */
public class OpenHABCommandTransformer {

    public static Object getCommandData(OpenhabCommandType.OpenhabCommand command) throws CouldNotTransformException {
        switch (command.getType()) {
            case DECIMAL:
                return command.getDecimal();
            case HSB:
                return command.getHsb();
            case INCREASEDECREASE:
                return command.getIncreaseDecrease();
            case ONOFF:
                command.getOnOff().getState();
            case OPENCLOSED:
                return command.getOpenClosed().getState();
            case PERCENT:
                return command.getPercent().getValue();
            case STOPMOVE:
                return command.getStopMove().getState();
            case STRING:
                return command.getText();
            case UPDOWN:
                return command.getUpDown().getState();
            default:
                throw new CouldNotTransformException("No corresponding data found for "+command+".");
        }
    }
}
