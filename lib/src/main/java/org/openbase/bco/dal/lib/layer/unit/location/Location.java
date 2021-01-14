package org.openbase.bco.dal.lib.layer.unit.location;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.dal.lib.layer.service.provider.PresenceStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.BaseUnit;
import org.openbase.bco.dal.lib.layer.unit.MultiUnit;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Snapshotable;
import org.openbase.type.domotic.action.SnapshotType.Snapshot;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.location.LocationDataType.LocationData;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface Location extends BaseUnit<LocationData>, MultiUnit<LocationData>, PresenceStateProviderService, Snapshotable<Snapshot> {

    @RPCMethod(legacy = true)
    @Override
    Future<Snapshot> recordSnapshot(final UnitType unitType);

    /**
     * Method returns a list of configuration of all aggregated units.
     *
     * Note: Base units and disabled units are not included.
     *
     * @return {@inheritDoc}
     *
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    default List<UnitConfig> getAggregatedUnitConfigList() throws NotAvailableException {
        final ArrayList<UnitConfig> unitConfigList = new ArrayList<>();

        // init service unit map
        for (final String unitId : getConfig().getLocationConfig().getUnitIdList()) {
            try {
                // resolve unit config by unit registry
                UnitConfig unitConfig;
                try {
                    unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);
                } catch (NotAvailableException ex) {
                    LoggerFactory.getLogger(Location.class).warn("Unit[" + unitId + "] not available for [" + this + "]");
                    continue;
                }

                // filter non dal units and disabled units
                try {
                    if (!UnitConfigProcessor.isDalUnit(unitConfig) || !UnitConfigProcessor.isEnabled(unitConfig)) {
                        continue;
                    }
                } catch (VerificationFailedException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("UnitConfig[" + UnitConfigProcessor.getDefaultAlias(unitConfig, "?") + "] could not be verified as a dal unit!", ex), LoggerFactory.getLogger(Location.class));
                }

                unitConfigList.add(unitConfig);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not process unit config update of Unit[" + unitId + "] for " + this + "!", ex), LoggerFactory.getLogger(Location.class));
            }
        }
        return unitConfigList;
    }
}
