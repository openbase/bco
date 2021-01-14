package org.openbase.bco.dal.lib.layer.unit.unitgroup;

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
import org.openbase.bco.dal.lib.layer.unit.BaseUnit;
import org.openbase.bco.dal.lib.layer.unit.MultiUnit;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.unitgroup.UnitGroupDataType.UnitGroupData;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface UnitGroup extends BaseUnit<UnitGroupData>, MultiUnit<UnitGroupData> {

    /**
     * Method returns a list of configuration of all aggregated units of this group.
     *
     * Note: Disabled units are not included.
     *
     * @return {@inheritDoc}
     *
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    default List<UnitConfig> getAggregatedUnitConfigList() throws NotAvailableException {
        final ArrayList<UnitConfig> unitConfigList = new ArrayList<>();

        // init service unit list
        for (final String unitId : getConfig().getUnitGroupConfig().getMemberIdList()) {
            // resolve unit config by unit registry
            UnitConfig unitConfig;
            try {
                unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);
            } catch (NotAvailableException ex) {
                LoggerFactory.getLogger(UnitGroup.class).warn("Unit[" + unitId + "] not available for [" + this + "]");
                continue;
            }

            // filter disabled units
            if (!UnitConfigProcessor.isEnabled(unitConfig)) {
                continue;
            }
            unitConfigList.add(unitConfig);
        }
        return unitConfigList;
    }
}
