/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.transform;

import de.citec.dal.bindings.openhab.OpenHABBinding;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.NotSupportedException;
import rst.homeautomation.openhab.OpenhabCommandType;
import rst.homeautomation.service.ServiceTypeHolderType;

/**
 *
 * @author mpohling
 */
public final class OpenhabCommandTransformer {

    public static Object getServiceData(OpenhabCommandType.OpenhabCommand command, ServiceTypeHolderType.ServiceTypeHolder.ServiceType serviceType) throws CouldNotPerformException {

        // Transform service data.
        switch (command.getType()) {
            case DECIMAL:
                switch (serviceType) {
                    case POWER_CONSUMPTION_PROVIDER:
                        return PowerConsumptionStateTransformer.transform(command.getDecimal());
                    case MOTION_PROVIDER:
                        return MotionStateTransformer.transform(command.getDecimal());
                    case TAMPER_PROVIDER:
                        return TamperStateTransformer.transform(command.getDecimal());
                    case BATTERY_PROVIDER:
                        return BatteryStateTransformer.transform(command.getDecimal());
                    case TEMPERATURE_ALARM_STATE_PROVIDER:
                    case SMOKE_ALARM_STATE_PROVIDER:
                        return AlarmStateTransformer.transform(command.getDecimal());
                    case SMOKE_STATE_PROVIDER:
                        return SmokeStateTransformer.transform(command.getDecimal());
                    default:
                        // native double type
                        return command.getDecimal();
                }
            case HSB:
                switch (serviceType) {
                    case COLOR_PROVIDER:
                    case COLOR_SERVICE:
                        return HSVColorTransformer.transform(command.getHsb());
                    default:
                        throw new NotSupportedException(serviceType, OpenHABBinding.class);
                }
            case INCREASEDECREASE:
//				return IncreaseDecreaseTransformer(command.getIncreaseDecrease());
                throw new NotSupportedException(command.getType(), OpenhabCommandTransformer.class);
            case ONOFF:
                switch (serviceType) {
                    case BUTTON_PROVIDER:
                        return ButtonStateTransformer.transform(command.getOnOff().getState());
                    case POWER_PROVIDER:
                    case POWER_SERVICE:
                        return PowerStateTransformer.transform(command.getOnOff().getState());
                    default:
                        throw new NotSupportedException(serviceType, OpenHABBinding.class);
                }
            case OPENCLOSED:
                return OpenClosedStateTransformer.transform(command.getOpenClosed().getState());
            case PERCENT:
                return command.getPercent().getValue();
            case STOPMOVE:
                return StopMoveStateTransformer.transform(command.getStopMove().getState());
            case STRING:
                // native string type
                return command.getText();
            case UPDOWN:
                return UpDownStateTransformer.transform(command.getUpDown().getState());
            default:
                throw new CouldNotTransformException("No corresponding data found for " + command + ".");
        }
    }

    public static Object getCommandData(final OpenhabCommandType.OpenhabCommand command) throws CouldNotPerformException {

        switch (command.getType()) {
            case DECIMAL:
                return command.getDecimal();
            case HSB:
                return command.getHsb();
            case INCREASEDECREASE:
                return command.getIncreaseDecrease();
            case ONOFF:
                return command.getOnOff();
            case OPENCLOSED:
                return command.getOpenClosed();
            case PERCENT:
                return command.getPercent();
            case STOPMOVE:
                return command.getStopMove();
            case STRING:
                return command.getText();
            case UPDOWN:
                return command.getUpDown();
            default:
                throw new CouldNotTransformException("No corresponding data found for " + command + ".");
        }
    }
}
