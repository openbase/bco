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
import java.util.List;
import org.dc.bco.dal.lib.layer.service.operation.BrightnessOperationService;
import org.dc.bco.dal.lib.layer.service.operation.ColorOperationService;
import org.dc.bco.dal.lib.layer.service.operation.OpeningRatioOperationService;
import org.dc.bco.dal.lib.layer.service.operation.PowerOperationService;
import org.dc.bco.dal.lib.layer.service.operation.ShutterOperationService;
import org.dc.bco.dal.lib.layer.service.operation.StandbyOperationService;
import org.dc.bco.dal.lib.layer.service.operation.TargetTemperatureOperationService;
import org.dc.bco.dal.lib.layer.service.provider.MotionProviderService;
import org.dc.bco.dal.lib.layer.service.provider.PowerConsumptionProviderService;
import org.dc.bco.dal.lib.layer.service.provider.SmokeAlarmStateProviderService;
import org.dc.bco.dal.lib.layer.service.provider.SmokeStateProviderService;
import org.dc.bco.dal.lib.layer.service.provider.TamperProviderService;
import org.dc.bco.dal.lib.layer.service.provider.TemperatureProviderService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.rst.iface.ScopeProvider;
import org.dc.jul.iface.Configurable;
import org.dc.jul.iface.Snapshotable;
import org.dc.jul.iface.provider.LabelProvider;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public interface Location extends ScopeProvider, LabelProvider, Configurable<String, LocationConfig>,
        BrightnessOperationService, ColorOperationService, OpeningRatioOperationService, PowerOperationService, ShutterOperationService,
        StandbyOperationService, TargetTemperatureOperationService, MotionProviderService, SmokeAlarmStateProviderService, SmokeStateProviderService,
        TemperatureProviderService, PowerConsumptionProviderService, TamperProviderService, Snapshotable<SceneConfig> {

    /**
     * Will return controller/remotes in the final implementation. Waiting for a
     * remote pool...
     *
     * @return
     * @throws CouldNotPerformException
     * @deprecated
     */
    @Deprecated
    public List<String> getNeighborLocationIds() throws CouldNotPerformException;
}
