/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

import org.dc.bco.manager.device.binding.openhab.comm.OpenHABCommunicatorImpl;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.CouldNotTransformException;
import org.dc.jul.exception.NotSupportedException;
import rst.homeautomation.openhab.OpenhabCommandType;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 *
 * @author mpohling
 */
public final class OpenhabCommandTransformer {

    public static Object getServiceData(OpenhabCommandType.OpenhabCommand command, ServiceType serviceType) throws CouldNotPerformException {

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
                        throw new NotSupportedException(serviceType, OpenHABCommunicatorImpl.class);
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
                        throw new NotSupportedException(serviceType, OpenHABCommunicatorImpl.class);
                }
            case OPENCLOSED:
                return OpenClosedStateTransformer.transform(command.getOpenClosed().getState());
            case PERCENT:
                return command.getPercent().getValue();
            case STOPMOVE:
                return StopMoveStateTransformer.transform(command.getStopMove().getState());
            case STRING:
                switch (serviceType) {
                    case HANDLE_PROVIDER:
                        return HandleStateTransformer.transform(command.getText());
                    default:
                        // native string type
                        return command.getText();
                }

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
