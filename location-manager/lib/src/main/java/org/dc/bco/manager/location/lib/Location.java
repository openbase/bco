package org.dc.bco.manager.location.lib;

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
import org.dc.bco.dal.lib.layer.service.BrightnessService;
import org.dc.bco.dal.lib.layer.service.ColorService;
import org.dc.bco.dal.lib.layer.service.DimService;
import org.dc.bco.dal.lib.layer.service.OpeningRatioService;
import org.dc.bco.dal.lib.layer.service.PowerService;
import org.dc.bco.dal.lib.layer.service.ShutterService;
import org.dc.bco.dal.lib.layer.service.StandbyService;
import org.dc.bco.dal.lib.layer.service.TargetTemperatureService;
import org.dc.bco.dal.lib.layer.service.provider.MotionProvider;
import org.dc.bco.dal.lib.layer.service.provider.PowerConsumptionProvider;
import org.dc.bco.dal.lib.layer.service.provider.SmokeAlarmStateProvider;
import org.dc.bco.dal.lib.layer.service.provider.SmokeStateProvider;
import org.dc.bco.dal.lib.layer.service.provider.TamperProvider;
import org.dc.bco.dal.lib.layer.service.provider.TemperatureProvider;
import org.dc.jul.extension.rst.iface.ScopeProvider;
import org.dc.jul.iface.Configurable;
import org.dc.jul.iface.provider.LabelProvider;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public interface Location extends ScopeProvider, LabelProvider, Configurable<String, LocationConfig>,
        BrightnessService, ColorService, DimService, OpeningRatioService, PowerService, ShutterService,
        StandbyService, TargetTemperatureService, MotionProvider, SmokeAlarmStateProvider, SmokeStateProvider,
        TemperatureProvider, PowerConsumptionProvider, TamperProvider {

}
