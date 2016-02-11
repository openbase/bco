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

import org.dc.bco.dal.lib.layer.service.BrightnessService;
import org.dc.bco.dal.lib.layer.service.ColorService;
import org.dc.bco.dal.lib.layer.service.PowerService;
import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.bco.dal.lib.layer.service.DimService;
import org.dc.bco.dal.lib.layer.service.OpeningRatioService;
import org.dc.bco.dal.lib.layer.service.ShutterService;
import org.dc.bco.dal.lib.layer.service.StandbyService;
import org.dc.bco.dal.lib.layer.service.TargetTemperatureService;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotSupportedException;

/**
 *
 * @author mpohling
 */
public class OpenhabServiceFactory implements ServiceFactory {

    private final static ServiceFactory instance = new OpenhabServiceFactory();

    public static ServiceFactory getInstance() {
        return instance;
    }

    @Override
    public <UNIT extends BrightnessService & Unit> BrightnessService newBrightnessService(final UNIT unit) throws InstantiationException {
        return new BrightnessServiceImpl(unit);
    }

    @Override
    public <UNIT extends ColorService & Unit> ColorService newColorService(final UNIT unit) throws InstantiationException {
        return new ColorServiceImpl(unit);
    }

    @Override
    public <UNIT extends PowerService & Unit> PowerService newPowerService(final UNIT unit) throws InstantiationException {
        return new PowerServiceImpl(unit);
    }

    @Override
    public <UNIT extends OpeningRatioService & Unit> OpeningRatioService newOpeningRatioService(final UNIT unit) throws InstantiationException {
        return new OpeningRatioServiceImpl(unit);
    }

    @Override
    public <UNIT extends ShutterService & Unit> ShutterService newShutterService(final UNIT unit) throws InstantiationException {
        return new ShutterServiceImpl(unit);
    }

    @Override
    public <UNIT extends DimService & Unit> DimService newDimmService(final UNIT unit) throws InstantiationException {
        return new DimServiceImpl(unit);
    }

    @Override
    public <UNIT extends StandbyService & Unit> StandbyService newStandbyService(final UNIT unit) throws InstantiationException {
        throw new InstantiationException(this, new NotSupportedException(StandbyService.class, this));
    }

    @Override
    public <UNIT extends TargetTemperatureService & Unit> TargetTemperatureService newTargetTemperatureService(final UNIT unit) throws InstantiationException {
        return new TargetTemperatureServiceImpl(unit);
    }
}
