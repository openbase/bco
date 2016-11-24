package org.openbase.bco.dal.lib.layer.unit.location;

/*
 * #%L
 * COMA LocationManager Library
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.lib.layer.service.provider.MotionStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.PowerConsumptionStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.SmokeAlarmStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.SmokeStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.TamperStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.TemperatureStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import org.openbase.jul.iface.Configurable;
import org.openbase.jul.iface.Snapshotable;
import org.openbase.jul.iface.provider.LabelProvider;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface Location extends ScopeProvider, LabelProvider, Configurable<String, UnitConfig>,
        BrightnessStateOperationService, ColorStateOperationService, PowerStateOperationService, BlindStateOperationService,
        StandbyStateOperationService, TargetTemperatureStateOperationService, MotionStateProviderService, SmokeAlarmStateProviderService, SmokeStateProviderService,
        TemperatureStateProviderService, PowerConsumptionStateProviderService, TamperStateProviderService, Snapshotable<Snapshot> {

    /**
     * TODO: Will return controller/remotes in the final implementation. Waiting for a
     * remote pool...
     *
     * @return
     * @throws CouldNotPerformException
     * @deprecated
     */
    @Deprecated
    public List<String> getNeighborLocationIds() throws CouldNotPerformException;
}
