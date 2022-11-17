package org.openbase.bco.registry.unit.lib.filter;

/*-
 * #%L
 * BCO Registry Unit Library
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

import org.openbase.jul.pattern.Filter;
import org.openbase.jul.pattern.ListFilter;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitFilterType.UnitFilter;

public class UnitConfigFilterImpl implements ListFilter<UnitConfig>, UnitConfigFilter {

    private final UnitFilter filter;
    private final UnitConfig properties;
    private final Filter<UnitConfig> andFilter, orFilter;

    private final boolean bypass;

    public UnitConfigFilterImpl(UnitFilter filter) {

        // create default instance if required that filters nothing
        filter = filter != null ? filter : UnitFilter.getDefaultInstance();

        this.filter = filter;
        this.bypass = !filter.hasProperties();
        this.properties = filter.getProperties();

        if (filter.hasAnd()) {
            this.andFilter = new UnitConfigFilterImpl(filter.getAnd());
        } else {
            this.andFilter = null;
        }

        if (filter.hasOr()) {
            this.orFilter = new UnitConfigFilterImpl(filter.getOr());
        } else {
            this.orFilter = null;
        }
    }

    @Override
    public boolean match(final UnitConfig unitConfig) {
        return filter.getNot() ^ (propertyMatch(unitConfig) && andFilterMatch(unitConfig)) || orFilterMatch(unitConfig);
    }

    private boolean propertyMatch(final UnitConfig unitConfig) {

        // handle bypass
        if (bypass) {
            return true;
        }

        // filter by id
        if (properties.hasId() && !(properties.getId().equals(unitConfig.getId()))) {
            return false;
        }

        // filter by alias
        if (properties.getAliasCount() > 0) {
            for (String alias : properties.getAliasList()) {
                if (!unitConfig.getAliasList().contains(alias)) {
                    return false;
                }
            }
        }

        // filter by type
        if (properties.hasUnitType() && !(properties.getUnitType().equals(unitConfig.getUnitType()))) {
            return false;
        }

        // filter by location
        if (properties.getPlacementConfig().hasLocationId() && !(properties.getPlacementConfig().getLocationId().equals(unitConfig.getPlacementConfig().getLocationId()))) {
            return false;
        }

        // filter by location root
        if (properties.getLocationConfig().hasRoot() && !(properties.getLocationConfig().getRoot() == (unitConfig.getLocationConfig().getRoot()))) {
            return false;
        }

        // filter by location type
        if (properties.getLocationConfig().hasLocationType() && !(properties.getLocationConfig().getLocationType() == (unitConfig.getLocationConfig().getLocationType()))) {
            return false;
        }

        // filter by username
        if (properties.hasUserConfig() && unitConfig.hasUserConfig() && !(properties.getUserConfig().getUserName().equals(unitConfig.getUserConfig().getUserName()))) {
            return false;
        }

        return true;
    }

    private boolean orFilterMatch(final UnitConfig unitConfig) {
        if (orFilter != null) {
            return orFilter.match(unitConfig);
        }
        return false;
    }

    private boolean andFilterMatch(final UnitConfig unitConfig) {
        if (andFilter != null) {
            return andFilter.match(unitConfig);
        }
        return true;
    }
}
