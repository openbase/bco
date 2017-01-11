package org.openbase.bco.manager.location.lib;

import org.openbase.bco.dal.lib.layer.service.collection.BlindStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.BrightnessStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.ColorStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.MotionStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.PowerConsumptionStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.PowerStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.SmokeAlarmStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.SmokeStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.StandbyStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.TamperStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.TargetTemperatureStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.collection.TemperatureStateProviderServiceCollection;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.protobuf.MessageController;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.location.LocationDataType.LocationData;

/*
 * #%L
 * BCO Manager Location Library
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface LocationController extends Location, MessageController<LocationData, LocationData.Builder>, BrightnessStateOperationServiceCollection,
        ColorStateOperationServiceCollection, PowerStateOperationServiceCollection, BlindStateOperationServiceCollection, StandbyStateOperationServiceCollection,
        TargetTemperatureStateOperationServiceCollection, MotionStateProviderServiceCollection, SmokeAlarmStateProviderServiceCollection,
        SmokeStateProviderServiceCollection, TemperatureStateProviderServiceCollection, PowerConsumptionStateProviderServiceCollection,
        TamperStateProviderServiceCollection {

    public void init(final UnitConfig config) throws InitializationException, InterruptedException;
}
