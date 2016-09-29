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
import com.google.protobuf.GeneratedMessage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactory;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactoryImpl;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.iface.Activatable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.action.ActionConfigType;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <S> generic definition of the overall service type for this remote.
 * @param <ST> the corresponding state for the service type of this remote.
 */
public abstract class AbstractServiceRemote<S extends Service, ST extends GeneratedMessage> implements Service, Activatable {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ServiceType serviceType;
    private final Map<String, UnitRemote> unitRemoteMap;
    private final Map<String, S> serviceMap;
    private UnitRemoteFactory factory = UnitRemoteFactoryImpl.getInstance();
    protected ST serviceState;
    private final Observer dataObserver;
    protected final ObservableImpl<ST> dataObservable = new ObservableImpl<>();
    private final SyncObject syncObject = new SyncObject("ServiceStateComputationLock");

    public AbstractServiceRemote(final ServiceType serviceType) {
        this.serviceType = serviceType;
        this.unitRemoteMap = new HashMap<>();
        this.serviceMap = new HashMap<>();
        this.dataObserver = new Observer() {

            @Override
            public void update(Observable source, Object data) throws Exception {
                updateServiceState();
            }
        };
        serviceState = null;
    }

    /**
     * Compute the service state of this service collection if an underlying service changes.
     *
     * @throws CouldNotPerformException if an underlying service throws an exception
     */
    protected abstract void computeServiceState() throws CouldNotPerformException;

    /**
     * Compute the current service state and notify observer.
     *
     * @throws CouldNotPerformException if the computation fails
     */
    private void updateServiceState() throws CouldNotPerformException {
        synchronized (syncObject) {
            computeServiceState();
        }
        dataObservable.notifyObservers(serviceState);
    }

    /**
     *
     * @return the current service state
     * @throws NotAvailableException if the service state has not been set at least once.
     */
    public ST getServiceState() throws NotAvailableException {
        if (serviceState == null) {
            throw new NotAvailableException("servicestate");
        }
        return serviceState;
    }

    /**
     * Add an observer to get notifications when the service state changes.
     *
     * @param observer the observer which is notified
     */
    public void addDataObserver(Observer<ST> observer) {
        dataObservable.addObserver(observer);
    }

    /**
     * Remove an observer for the service state.
     *
     * @param observer the observer which has been registered
     */
    public void removeDataObserver(Observer<ST> observer) {
        dataObservable.removeObserver(observer);
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
        return unitConfig.getServiceConfigList().stream().anyMatch((serviceConfig) -> (serviceConfig.getServiceTemplate().getType() == serviceType));
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
            GlobalExecutionService.submit(new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    remote.waitForData();
                    remote.addDataObserver(dataObserver);
                    return null;
                }
            });
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
            remote.removeDataObserver(dataObserver);
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

    /**
     * Method blocks until an initial data message was received from every remote controller.
     *
     * @throws CouldNotPerformException is thrown if any error occurs.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        for (UnitRemote unitRemote : getInternalUnits()) {
            unitRemote.waitForData();
        }
    }

    /**
     * Method blocks until an initial data message was received from every remote controller or the given timeout is reached.
     *
     * @param timeout maximal time to wait for the main controller data. After the timeout is reached a TimeoutException is thrown.
     * @param timeUnit the time unit of the timeout.
     * @throws CouldNotPerformException is thrown in case the any error occurs, or if the given timeout is reached. In this case a TimeoutException is thrown.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    public void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        for (UnitRemote unitRemote : getInternalUnits()) {
            unitRemote.waitForData(timeout, timeUnit);
        }
    }

    /**
     * Checks if a server connection is established for every underlying remote.
     *
     * @return is true in case that the connection for every underlying remote it established.
     */
    public boolean isConnected() {
        return getInternalUnits().stream().noneMatch((unitRemote) -> (!unitRemote.isConnected()));
    }

    /**
     * Check if the data object is already available for every underlying remote.
     *
     * @return is true in case that for every underlying remote data is available.
     */
    public boolean isDataAvailable() {
        return getInternalUnits().stream().noneMatch((unitRemote) -> (!unitRemote.isDataAvailable()));
    }
}
