package org.openbase.bco.manager.device.binding.openhab.service;

/*
 * #%L
 * BCO Manager Device Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.ServiceFactory;
import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotSupportedException;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class OpenhabServiceFactory implements ServiceFactory {

    private final static ServiceFactory instance = new OpenhabServiceFactory();

    public static ServiceFactory getInstance() {
        return instance;
    }

    @Override
    public <UNIT extends BrightnessStateOperationService & Unit> BrightnessStateOperationService newBrightnessService(final UNIT unit) throws InstantiationException {
        return new BrightnessStateServiceImpl(unit);
    }

    @Override
    public <UNIT extends ColorStateOperationService & Unit> ColorStateOperationService newColorService(final UNIT unit) throws InstantiationException {
        return new ColorStateServiceImpl(unit);
    }

    @Override
    public <UNIT extends PowerStateOperationService & Unit> PowerStateOperationService newPowerService(final UNIT unit) throws InstantiationException {
        return new PowerStateServiceImpl(unit);
    }

    @Override
    public <UNIT extends BlindStateOperationService & Unit> BlindStateOperationService newShutterService(final UNIT unit) throws InstantiationException {
        return new BlindStateServiceImpl(unit);
    }

    @Override
    public <UNIT extends StandbyStateOperationService & Unit> StandbyStateOperationService newStandbyService(final UNIT unit) throws InstantiationException {
        throw new InstantiationException(this, new NotSupportedException(StandbyStateOperationService.class, this));
    }

    @Override
    public <UNIT extends TargetTemperatureStateOperationService & Unit> TargetTemperatureStateOperationService newTargetTemperatureService(final UNIT unit) throws InstantiationException {
        return new TargetTemperatureStateServiceImpl(unit);
    }
}
