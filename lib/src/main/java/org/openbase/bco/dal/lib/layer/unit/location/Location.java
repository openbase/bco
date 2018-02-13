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
import java.util.*;
import java.util.concurrent.Future;

import com.google.protobuf.GeneratedMessage;
import org.openbase.bco.dal.lib.layer.service.provider.PresenceStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.BaseUnit;
import org.openbase.bco.dal.lib.layer.unit.MultiUnitServiceFusion;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.iface.Snapshotable;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.location.LocationDataType.LocationData;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface Location extends BaseUnit<LocationData>, PresenceStateProviderService, Snapshotable<Snapshot>, MultiUnitServiceFusion {

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

    @Override
    default public Set<ServiceType> getSupportedServiceTypes() throws NotAvailableException, InterruptedException {
        final Set<ServiceTemplate.ServiceType> serviceTypeSet = new HashSet<>();
        try {
            for (final ServiceConfig serviceConfig : getConfig().getServiceConfigList()) {
                serviceTypeSet.add(serviceConfig.getServiceDescription().getType());
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("SupportedServiceTypes", new CouldNotPerformException("Could not generate supported service type list!", ex));
        }
        return serviceTypeSet;
    }

    @Override
    default public PresenceState getPresenceState() throws NotAvailableException {
        try {
            return getData().getPresenceState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PresenceState", ex);
        }
    }

    @RPCMethod
    @Override
    public Future<Snapshot> recordSnapshot(final UnitType unitType) throws CouldNotPerformException, InterruptedException;
}
