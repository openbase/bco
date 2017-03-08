package org.openbase.bco.dal.lib.layer.unit.unitgroup;

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
import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.lib.layer.service.provider.IlluminanceStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.BaseUnit;
import rst.domotic.unit.unitgroup.UnitGroupDataType.UnitGroupData;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface UnitGroup extends BaseUnit<UnitGroupData>, BrightnessStateOperationService,
        ColorStateOperationService, PowerStateOperationService, BlindStateOperationService,
        StandbyStateOperationService, TargetTemperatureStateOperationService, IlluminanceStateProviderService {

}
