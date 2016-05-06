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

import org.dc.bco.dal.lib.layer.service.operation.BrightnessOperationService;
import org.dc.bco.dal.lib.layer.service.operation.ColorOperationService;
import org.dc.bco.dal.lib.layer.service.operation.PowerOperationService;
import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.bco.dal.lib.layer.service.operation.OpeningRatioOperationService;
import org.dc.bco.dal.lib.layer.service.operation.ShutterOperationService;
import org.dc.bco.dal.lib.layer.service.operation.StandbyOperationService;
import org.dc.bco.dal.lib.layer.service.operation.TargetTemperatureOperationService;
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
    public <UNIT extends BrightnessOperationService & Unit> BrightnessOperationService newBrightnessService(final UNIT unit) throws InstantiationException {
        return new BrightnessServiceImpl(unit);
    }

    @Override
    public <UNIT extends ColorOperationService & Unit> ColorOperationService newColorService(final UNIT unit) throws InstantiationException {
        return new ColorServiceImpl(unit);
    }

    @Override
    public <UNIT extends PowerOperationService & Unit> PowerOperationService newPowerService(final UNIT unit) throws InstantiationException {
        return new PowerServiceImpl(unit);
    }

    @Override
    public <UNIT extends OpeningRatioOperationService & Unit> OpeningRatioOperationService newOpeningRatioService(final UNIT unit) throws InstantiationException {
        return new OpeningRatioServiceImpl(unit);
    }

    @Override
    public <UNIT extends ShutterOperationService & Unit> ShutterOperationService newShutterService(final UNIT unit) throws InstantiationException {
        return new ShutterServiceImpl(unit);
    }

    @Override
    public <UNIT extends StandbyOperationService & Unit> StandbyOperationService newStandbyService(final UNIT unit) throws InstantiationException {
        throw new InstantiationException(this, new NotSupportedException(StandbyOperationService.class, this));
    }

    @Override
    public <UNIT extends TargetTemperatureOperationService & Unit> TargetTemperatureOperationService newTargetTemperatureService(final UNIT unit) throws InstantiationException {
        return new TargetTemperatureServiceImpl(unit);
    }
}
