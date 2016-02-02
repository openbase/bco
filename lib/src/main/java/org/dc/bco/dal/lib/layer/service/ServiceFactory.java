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

import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.jul.exception.InstantiationException;

/**
 *
 * @author mpohling
 */
public interface ServiceFactory {

    public abstract <UNIT extends BrightnessService & Unit> BrightnessService newBrightnessService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends ColorService & Unit> ColorService newColorService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends PowerService & Unit> PowerService newPowerService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends OpeningRatioService & Unit> OpeningRatioService newOpeningRatioService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends ShutterService & Unit> ShutterService newShutterService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends DimService & Unit> DimService newDimmService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends StandbyService & Unit> StandbyService newStandbyService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends TargetTemperatureService & Unit> TargetTemperatureService newTargetTemperatureService(UNIT unit) throws InstantiationException;

}
