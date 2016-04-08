/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.scene.binding.openhab.transform;

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
import org.dc.bco.manager.scene.binding.openhab.comm.OpenHABCommunicatorImpl;
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
