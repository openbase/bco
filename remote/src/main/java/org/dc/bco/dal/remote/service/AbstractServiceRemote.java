/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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

import org.dc.bco.dal.lib.layer.service.Service;
import org.dc.bco.dal.remote.unit.DALRemoteService;
import org.dc.bco.dal.remote.unit.UnitRemoteFactory;
import org.dc.bco.dal.remote.unit.UnitRemoteFactoryInterface;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.MultiException;
import org.dc.jul.exception.NotSupportedException;
import org.dc.jul.iface.Activatable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author mpohling
 * @param <S> generic definition of the overall service type for this remote.
 */
public abstract class AbstractServiceRemote<S extends Service> implements Service, Activatable {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServiceType serviceType;
    private final Map<String, DALRemoteService> unitRemoteMap;
    private final Map<String, S> serviceMap;
    private UnitRemoteFactoryInterface factory = UnitRemoteFactory.getInstance();

    public AbstractServiceRemote(final ServiceType serviceType) {
        this.serviceType = serviceType;
        this.unitRemoteMap = new HashMap<>();
        this.serviceMap = new HashMap<>();
    }

    public void init(final UnitConfig config) throws CouldNotPerformException {
        try {
            if (!verifyServiceCompatibility(config, serviceType)) {
                throw new NotSupportedException("Unit template is not compatible with given ServiceType[" + serviceType.name() + "]!", config.getId(), this);
            }

            DALRemoteService remote = factory.createAndInitUnitRemote(config);
            try {
                serviceMap.put(config.getId(), (S) remote);
            } catch (ClassCastException ex) {
                throw new NotSupportedException("Remote does not implement service interface!", remote, this, ex);
            }

            unitRemoteMap.put(config.getId(), remote);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not init service unit.", ex);
        }
    }

    public void init(final Collection<UnitConfig> configs) throws CouldNotPerformException {
        MultiException.ExceptionStack exceptionStack = null;
        for (UnitConfig config : configs) {
            try {
                init(config);
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
        }
        MultiException.checkAndThrow("Could not activate all service units!", exceptionStack);
    }
    
    private static boolean verifyServiceCompatibility(final UnitConfig unitConfig, final ServiceType serviceType) {
        for(ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
            if(serviceConfig.getType() == serviceType) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void activate() throws InterruptedException, MultiException {
        MultiException.ExceptionStack exceptionStack = null;
        for (DALRemoteService remote : unitRemoteMap.values()) {
            try {
                remote.activate();
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(remote, ex, exceptionStack);
            }
        }
        MultiException.checkAndThrow("Could not activate all service units!", exceptionStack);
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        MultiException.ExceptionStack exceptionStack = null;
        for (DALRemoteService remote : unitRemoteMap.values()) {
            try {
                remote.deactivate();
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(remote, ex, exceptionStack);
            }
        }
        MultiException.checkAndThrow("Could not deactivate all service units!", exceptionStack);
    }

    @Override
    public boolean isActive() {
        for (DALRemoteService remote : unitRemoteMap.values()) {
            if (!remote.isActive()) {
                return false;
            }
        }
        return true;
    }

    public UnitRemoteFactoryInterface getFactory() {
        return factory;
    }

    public void setFactory(UnitRemoteFactoryInterface factory) {
        this.factory = factory;
    }

    public Collection<DALRemoteService> getInternalUnits() {
        return Collections.unmodifiableCollection(unitRemoteMap.values());
    }

    public Collection<S> getServices() {
        return Collections.unmodifiableCollection(serviceMap.values());
    }

    @Override
    public org.dc.bco.dal.lib.layer.service.ServiceType getServiceType() {
        // TODO mpohling: implement service type transformer.
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ServiceConfigType.ServiceConfig getServiceConfig() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
