package org.openbase.bco.dal.lib.layer.unit.location;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.bco.dal.lib.layer.unit.MultiUnitServiceFusion;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Snapshotable;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationDataType.LocationData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface Location extends BaseUnit<LocationData>, PresenceStateProviderService, Snapshotable<Snapshot>, MultiUnitServiceFusion {

    @Override
    default Set<ServiceType> getSupportedServiceTypes() throws NotAvailableException {
        final Set<ServiceTemplate.ServiceType> serviceTypeSet = new HashSet<>();
        try {
            final Map<ServiceType, Boolean> serviceTypeActiveMap = new HashMap<>();
            for (final ServiceConfig serviceConfig : getConfig().getServiceConfigList()) {
//                serviceTypeSet.add(serviceConfig.getServiceDescription().getServiceType());
                serviceTypeActiveMap.put(serviceConfig.getServiceDescription().getServiceType(), false);
            }

            for (final String unitId : getConfig().getLocationConfig().getUnitIdList()) {
                UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    ServiceType serviceType = serviceConfig.getServiceDescription().getServiceType();
                    if (serviceTypeActiveMap.containsKey(serviceType)) {
                        serviceTypeActiveMap.put(serviceType, true);
                    }
                }
            }

            if (serviceTypeActiveMap.get(ServiceType.MOTION_STATE_SERVICE)) {
                serviceTypeActiveMap.put(ServiceType.PRESENCE_STATE_SERVICE, true);
            }

            for (Entry<ServiceType, Boolean> entry : serviceTypeActiveMap.entrySet()) {
                if (entry.getValue()) {
                    serviceTypeSet.add(entry.getKey());
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("SupportedServiceTypes", new CouldNotPerformException("Could not generate supported service type list!", ex));
        }


        return serviceTypeSet;
    }

    @RPCMethod(legacy = true)
    @Override
    Future<Snapshot> recordSnapshot(final UnitType unitType) throws CouldNotPerformException, InterruptedException;
}
