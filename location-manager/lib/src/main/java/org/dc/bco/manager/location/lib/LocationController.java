package org.dc.bco.manager.location.lib;

import org.dc.bco.dal.lib.layer.service.collection.BrightnessStateOperationServiceCollection;
import org.dc.bco.dal.lib.layer.service.collection.ColorStateOperationServiceCollection;
import org.dc.bco.dal.lib.layer.service.collection.DimStateOperationServiceCollection;
import org.dc.bco.dal.lib.layer.service.collection.MotionStateProviderServiceCollection;
import org.dc.bco.dal.lib.layer.service.collection.OpeningRatioStateOperationServiceCollection;
import org.dc.bco.dal.lib.layer.service.collection.PowerConsumptionStateProviderServiceCollection;
import org.dc.bco.dal.lib.layer.service.collection.PowerStateOperationServiceCollection;
import org.dc.bco.dal.lib.layer.service.collection.ShutterStateOperationServiceCollection;
import org.dc.bco.dal.lib.layer.service.collection.SmokeAlarmStateProviderServiceCollection;
import org.dc.bco.dal.lib.layer.service.collection.SmokeStateProviderServiceCollection;
import org.dc.bco.dal.lib.layer.service.collection.StandbyStateOperationServiceCollection;
import org.dc.bco.dal.lib.layer.service.collection.TamperStateProviderServiceCollection;
import org.dc.bco.dal.lib.layer.service.collection.TargetTemperatureStateOperationServiceCollection;
import org.dc.bco.dal.lib.layer.service.collection.TemperatureStateProviderServiceCollection;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.pattern.Controller;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationDataType.LocationData;

/*
 * #%L
 * COMA LocationManager Library
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface LocationController extends Location, Controller<LocationData, LocationData.Builder>, BrightnessStateOperationServiceCollection,
        ColorStateOperationServiceCollection, DimStateOperationServiceCollection, OpeningRatioStateOperationServiceCollection,
        PowerStateOperationServiceCollection, ShutterStateOperationServiceCollection, StandbyStateOperationServiceCollection,
        TargetTemperatureStateOperationServiceCollection, MotionStateProviderServiceCollection, SmokeAlarmStateProviderServiceCollection,
        SmokeStateProviderServiceCollection, TemperatureStateProviderServiceCollection, PowerConsumptionStateProviderServiceCollection,
        TamperStateProviderServiceCollection {

    public void init(final LocationConfig config) throws InitializationException, InterruptedException;
}
