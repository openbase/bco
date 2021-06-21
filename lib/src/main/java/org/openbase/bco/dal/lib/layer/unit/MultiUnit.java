package org.openbase.bco.dal.lib.layer.unit;

/*-
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

import com.google.protobuf.Message;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface describes a unit which internally holds further units and aggregates its states.
 *
 * @param <DATA> the data type of this unit used for the state synchronization.
 */
public interface MultiUnit<DATA extends Message> extends Unit<DATA>, ServiceAggregator {

    /**
     * {@inheritDoc}
     *
     * @param onlyAvailableServices {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    default UnitTemplate getUnitTemplate(final boolean onlyAvailableServices) throws NotAvailableException {
        // return the unfiltered unit template if filter is not active.
        if (!onlyAvailableServices) {
            return getUnitTemplate();
        }
        final UnitTemplate.Builder unitTemplateBuilder = getUnitTemplate().toBuilder();

        unitTemplateBuilder.clearServiceDescription();
        for (final ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {
            if (isServiceAvailable(serviceDescription.getServiceType())) {
                unitTemplateBuilder.addServiceDescription(serviceDescription);
            }
        }
        return unitTemplateBuilder.build();
    }

    /**
     * Method returns true if the service referred by the given type is available for this unit.
     *
     * @param serviceType the type to refer the unit.
     *
     * @return true if available and otherwise false.
     */
    boolean isServiceAvailable(final ServiceType serviceType);

    /**
     * Method returns if the provided service type is aggregated by this unit.
     *
     * @param serviceType the type checked.
     *
     * @return true if the service type is aggregated and else false.
     *
     * @throws NotAvailableException if the unit template cannot be accessed.
     */
    default boolean isServiceAggregated(final ServiceType serviceType) throws NotAvailableException {
        for (final ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {
            if (serviceDescription.getServiceType() != serviceType) {
                continue;
            }

            if (serviceDescription.getAggregated()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method returns a list of ids of all aggregated units.
     *
     * @return a list of the ids of all aggregated units.
     */
    default List<String> getAggregatedUnitIdList() throws NotAvailableException {
        final ArrayList<String> unitIdList = new ArrayList<>();
        for (UnitConfig aggregatedUnitConfig : getAggregatedUnitConfigList()) {
            unitIdList.add(aggregatedUnitConfig.getId());
        }
        return unitIdList;
    }

    /**
     * Method returns a list of configuration of all aggregated units.
     *
     * @return a list of the configurations of all aggregated units.
     */
    List<UnitConfig> getAggregatedUnitConfigList() throws NotAvailableException;
}
