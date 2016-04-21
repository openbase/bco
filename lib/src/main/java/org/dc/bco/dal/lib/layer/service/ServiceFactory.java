/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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

import org.dc.bco.dal.lib.layer.service.operation.BrightnessOperationService;
import org.dc.bco.dal.lib.layer.service.operation.StandbyOperationService;
import org.dc.bco.dal.lib.layer.service.operation.DimOperationService;
import org.dc.bco.dal.lib.layer.service.operation.TargetTemperatureOperationService;
import org.dc.bco.dal.lib.layer.service.operation.OpeningRatioOperationService;
import org.dc.bco.dal.lib.layer.service.operation.ColorOperationService;
import org.dc.bco.dal.lib.layer.service.operation.ShutterOperationService;
import org.dc.bco.dal.lib.layer.service.operation.PowerOperationService;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.jul.exception.InstantiationException;

/**
 *
 * @author mpohling
 */
public interface ServiceFactory {

    public abstract <UNIT extends BrightnessOperationService & Unit> BrightnessOperationService newBrightnessService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends ColorOperationService & Unit> ColorOperationService newColorService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends PowerOperationService & Unit> PowerOperationService newPowerService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends OpeningRatioOperationService & Unit> OpeningRatioOperationService newOpeningRatioService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends ShutterOperationService & Unit> ShutterOperationService newShutterService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends DimOperationService & Unit> DimOperationService newDimmService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends StandbyOperationService & Unit> StandbyOperationService newStandbyService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends TargetTemperatureOperationService & Unit> TargetTemperatureOperationService newTargetTemperatureService(UNIT unit) throws InstantiationException;

}
