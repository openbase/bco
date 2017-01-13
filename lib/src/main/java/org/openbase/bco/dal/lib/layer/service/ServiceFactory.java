package org.openbase.bco.dal.lib.layer.service;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.IntensityStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.InstantiationException;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface ServiceFactory {

    public abstract <UNIT extends BrightnessStateOperationService & Unit> BrightnessStateOperationService newBrightnessService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends ColorStateOperationService & Unit> ColorStateOperationService newColorService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends IntensityStateOperationService & Unit> IntensityStateOperationService newIntensityStateService(UNIT unit) throws InstantiationException;
    
    public abstract <UNIT extends PowerStateOperationService & Unit> PowerStateOperationService newPowerService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends BlindStateOperationService & Unit> BlindStateOperationService newShutterService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends StandbyStateOperationService & Unit> StandbyStateOperationService newStandbyService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends TargetTemperatureStateOperationService & Unit> TargetTemperatureStateOperationService newTargetTemperatureService(UNIT unit) throws InstantiationException;

}
