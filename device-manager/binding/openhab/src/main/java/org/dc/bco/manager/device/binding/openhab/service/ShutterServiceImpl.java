/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.service;

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
import org.dc.bco.manager.device.binding.openhab.execution.OpenHABCommandFactory;
import org.dc.bco.manager.device.lib.Device;
import org.dc.bco.dal.lib.layer.service.ShutterService;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import rst.homeautomation.state.ShutterStateType.ShutterState;

/**
 *
 * @author thuxohl
 * @param <ST> Related service type.
 */
public class ShutterServiceImpl<ST extends ShutterService & Unit> extends OpenHABService<ST> implements ShutterService {

    public ShutterServiceImpl(final ST unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public void setShutter(ShutterState state) throws CouldNotPerformException {
        executeCommand(OpenHABCommandFactory.newUpDownCommand(state.getValue()));
    }

    @Override
    public ShutterState getShutter() throws CouldNotPerformException {
        return unit.getShutter();
    }

}
