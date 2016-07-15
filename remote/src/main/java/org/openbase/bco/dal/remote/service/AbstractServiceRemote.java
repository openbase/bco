package org.openbase.bco.dal.remote.service;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactory;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactoryImpl;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.iface.Activatable;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.action.ActionConfigType;
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
    private final Map<String, UnitRemote> unitRemoteMap;
    private final Map<String, S> serviceMap;
    private UnitRemoteFactory factory = UnitRemoteFactoryImpl.getInstance();

    public AbstractServiceRemote(final ServiceType serviceType) {
        this.serviceType = serviceType;
        this.unitRemoteMap = new HashMap<>();
        this.serviceMap = new HashMap<>();
    }

    public void init(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        try {
            if (!verifyServiceCompatibility(config, serviceType)) {
                throw new NotSupportedException("Unit template is not compatible with given ServiceType[" + serviceType.name() + "]!", config.getId(), this);
            }

            UnitRemote remote = factory.newInitializedInstance(config);
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

    public void init(final Collection<UnitConfig> configs) throws CouldNotPerformException, InterruptedException {
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
        for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
            if (serviceConfig.getServiceTemplate().getType() == serviceType) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        MultiException.ExceptionStack exceptionStack = null;
        for (UnitRemote remote : unitRemoteMap.values()) {
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
        for (UnitRemote remote : unitRemoteMap.values()) {
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
        return unitRemoteMap.values().stream().allMatch((remote) -> (remote.isActive()));
    }

    public UnitRemoteFactory getFactory() {
        return factory;
    }

    public void setFactory(UnitRemoteFactory factory) {
        this.factory = factory;
    }

    public Collection<UnitRemote> getInternalUnits() {
        return Collections.unmodifiableCollection(unitRemoteMap.values());
    }

    public Collection<S> getServices() {
        return Collections.unmodifiableCollection(serviceMap.values());
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    @Override
    public Future<Void> applyAction(final ActionConfigType.ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        try {
            if (!actionConfig.getServiceType().equals(getServiceType())) {
                throw new VerificationFailedException("Service type is not compatible to given action config!");
            }

            List<Future> actionFutureList = new ArrayList<>();

            for (UnitRemote remote : getInternalUnits()) {
                actionFutureList.add(remote.applyAction(actionConfig));
//                remote.callMethod("set" + StringProcessor.transformUpperCaseToCamelCase(serviceType.toString()).replaceAll("Service", ""),
//                        ServiceJSonProcessor.deserialize(actionConfig.getServiceAttribute(), actionConfig.getServiceAttributeType()));
            }
            return GlobalExecutionService.allOf(actionFutureList, (Void) null);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply action!", ex);
        }
    }
}
