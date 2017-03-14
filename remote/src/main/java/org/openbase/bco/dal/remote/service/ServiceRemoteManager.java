package org.openbase.bco.dal.remote.service;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Activatable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class ServiceRemoteManager implements Activatable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRemoteManager.class);

    private boolean active;
    private final SyncObject serviceRemoteMapLock = new SyncObject("ServiceRemoteMapLock");
    private final ServiceRemoteFactory serviceRemoteFactory;
    private final Map<ServiceType, AbstractServiceRemote> serviceRemoteMap;
    private final Observer serviceDataObserver;

    public ServiceRemoteManager() {
        this.serviceRemoteMap = new HashMap<>();
        this.serviceRemoteFactory = ServiceRemoteFactoryImpl.getInstance();

        serviceDataObserver = (Observer) (Observable source, Object data) -> {
            notifyServiceUpdate(source, data);
        };
    }

    public synchronized void applyConfigUpdate(final List<String> unitIDList) throws CouldNotPerformException, InterruptedException {
        synchronized (serviceRemoteMapLock) {
            // shutdown all existing instances.
            for (AbstractServiceRemote serviceRemote : serviceRemoteMap.values()) {
                serviceRemote.removeDataObserver(serviceDataObserver);
                serviceRemote.shutdown();
            }
            serviceRemoteMap.clear();

            // init a new set for each supported service type.
            Map<ServiceType, Set<UnitConfig>> serviceMap = new HashMap<>();
            for (ServiceType serviceType : ServiceType.values()) {
                serviceMap.put(serviceType, new HashSet<>());
            }

            // init service unit map
            for (final String unitId : unitIDList) {

                // resolve unit config by unit registry
                final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);

                // filter non dal units
                if (!UnitConfigProcessor.isDalUnit(unitConfig)) {
                    continue;
                }

                // sort dal unit by service type
                unitConfig.getServiceConfigList().stream().forEach((serviceConfig) -> {
                    // register unit for service type. UnitConfigs are may added twice because of dublicated type of different service pattern but are filtered by the set. 
                    serviceMap.get(serviceConfig.getServiceTemplate().getType()).add(unitConfig);
                });
            }

            // initialize service remotes
            for (ServiceType serviceType : getManagedServiceTypes()) {
                final AbstractServiceRemote serviceRemote = serviceRemoteFactory.newInitializedInstance(serviceType, serviceMap.get(serviceType));
                serviceRemoteMap.put(serviceType, serviceRemote);

                // if already active than update the current location state.
                if (isActive()) {
                    serviceRemote.addDataObserver(serviceDataObserver);
                    serviceRemote.activate();
                }
            }
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        synchronized (serviceRemoteMapLock) {
            active = true;
            for (AbstractServiceRemote serviceRemote : serviceRemoteMap.values()) {
                serviceRemote.addDataObserver(serviceDataObserver);
                serviceRemote.activate();
            }
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            active = false;
            for (AbstractServiceRemote serviceRemote : serviceRemoteMap.values()) {
                serviceRemote.removeDataObserver(serviceDataObserver);
                serviceRemote.deactivate();
            }
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public List<AbstractServiceRemote> getServiceRemoteList() {
        synchronized (serviceRemoteMapLock) {
            return new ArrayList<>(serviceRemoteMap.values());
        }
    }

    public AbstractServiceRemote getServiceRemote(final ServiceType serviceType) {
        synchronized (serviceRemoteMapLock) {
            return serviceRemoteMap.get(serviceType);
        }
    }

    protected abstract Set<ServiceType> getManagedServiceTypes() throws NotAvailableException, InterruptedException;

    protected abstract void notifyServiceUpdate(final Observable source, final Object data) throws NotAvailableException, InterruptedException;

}
