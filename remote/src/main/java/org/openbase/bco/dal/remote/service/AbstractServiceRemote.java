package org.openbase.bco.dal.remote.service;

/*
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
import com.google.protobuf.GeneratedMessage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.ShutdownException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.Remote;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionConfigType.ActionConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * @param <S> generic definition of the overall service type for this remote.
 * @param <ST> the corresponding state for the service type of this remote.
 */
public abstract class AbstractServiceRemote<S extends Service, ST extends GeneratedMessage> implements ServiceRemote<S, ST> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private boolean active;
    private final ServiceType serviceType;
    private final Class<ST> serviceDataClass;
    private final Map<String, UnitRemote> unitRemoteMap;
    private final Map<UnitType, List<S>> unitRemoteTypeMap;
    private final Map<String, S> serviceMap;
    private final Observer dataObserver;
    protected final ObservableImpl<ST> serviceStateObservable = new ObservableImpl<>();
    private final SyncObject syncObject = new SyncObject("ServiceStateComputationLock");
    private final SyncObject maintainerLock = new SyncObject("MaintainerLock");
    protected Object maintainer;

    /**
     * AbstractServiceRemote constructor.
     *
     * @param serviceType The remote service type.
     * @param serviceDataClass The service data class.
     */
    public AbstractServiceRemote(final ServiceType serviceType, final Class<ST> serviceDataClass) {
        this.serviceType = serviceType;
        this.serviceDataClass = serviceDataClass;
        this.unitRemoteMap = new HashMap<>();
        this.unitRemoteTypeMap = new HashMap<>();
        this.serviceMap = new HashMap<>();
        this.dataObserver = (Observer) (Observable source, Object data) -> {
            updateServiceState();
        };
        this.serviceStateObservable.setExecutorService(GlobalCachedExecutorService.getInstance().getExecutorService());
    }

    /**
     * Compute the service state of this service collection if an underlying
     * service changes.
     *
     * @return the computed server state is returned.
     * @throws CouldNotPerformException if an underlying service throws an
     * exception
     */
    protected abstract ST computeServiceState() throws CouldNotPerformException;

    /**
     * Compute the current service state and notify observer.
     *
     * @throws CouldNotPerformException if the computation fails
     */
    private void updateServiceState() throws CouldNotPerformException {
        final ST serviceState;
        synchronized (syncObject) {
            serviceState = computeServiceState();
        }
        serviceStateObservable.notifyObservers(serviceState);
        assert serviceStateObservable.isValueAvailable();
    }

    /**
     *
     * @return the current service state
     * @throws NotAvailableException if the service state has not been set at
     * least once.
     * @deprecated please use getData instead.
     */
    @Override
    @Deprecated
    public ST getServiceState() throws NotAvailableException {
        if (!serviceStateObservable.isValueAvailable()) {
            throw new NotAvailableException("ServiceState");
        }
        return serviceStateObservable.getValue();
    }

    /**
     *
     * @return the current service state
     * @throws NotAvailableException if the service state data has not been set at
     * least once.
     */
    @Override
    public ST getData() throws NotAvailableException {
        if (!serviceStateObservable.isValueAvailable()) {
            throw new NotAvailableException("Data");
        }
        return serviceStateObservable.getValue();
    }

    /**
     * Add an observer to get notifications when the service state changes.
     *
     * @param observer the observer which is notified
     */
    @Override
    public void addDataObserver(final Observer<ST> observer) {
        serviceStateObservable.addObserver(observer);
    }

    /**
     * Remove an observer for the service state.
     *
     * @param observer the observer which has been registered
     */
    @Override
    public void removeDataObserver(final Observer<ST> observer) {
        serviceStateObservable.removeObserver(observer);
    }

    @Override
    public Class<ST> getDataClass() {
        return serviceDataClass;
    }

    /**
     * Method request the data of all internal unit remotes.
     *
     * @param failOnError flag decides if an exception should be thrown in case one data request fails.
     * @return the recalculated server state data based on the newly requested data.
     * @throws CouldNotPerformException is thrown if non of the request was successful. In case the failOnError is set to true any request error throws an CouldNotPerformException.
     */
    public CompletableFuture<ST> requestData(final boolean failOnError) throws CouldNotPerformException {
        final CompletableFuture<ST> requestDataFuture = new CompletableFuture<>();
        GlobalCachedExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<Future> taskList = new ArrayList<>();
                    MultiException.ExceptionStack exceptionStack = null;
                    for (final Remote remote : getInternalUnits()) {
                        try {
                            taskList.add(remote.requestData());
                        } catch (CouldNotPerformException ex) {
                            MultiException.push(remote, ex, exceptionStack);
                        }
                    }
                    boolean noResponse = true;
                    for (final Future task : taskList) {
                        try {
                            task.get();
                            noResponse = false;
                        } catch (ExecutionException ex) {
                            MultiException.push(task, ex, exceptionStack);
                        }
                    }

                    try {
                        MultiException.checkAndThrow("Could not request status of all internal remotes!", exceptionStack);
                    } catch (MultiException ex) {
                        if (failOnError || noResponse) {
                            throw ex;
                        }
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not request data of all internal unit remotes!", ex), logger, LogLevel.WARN);
                    }
                    requestDataFuture.complete(getData());
                } catch (InterruptedException | CouldNotPerformException ex) {
                    requestDataFuture.completeExceptionally(ex);
                }
            }
        });

        return requestDataFuture;
    }

    /**
     * {@inheritDoc}
     *
     * @param config {@inheritDoc}
     * @throws InitializationException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        try {
            verifyMaintainability();
            if (!verifyServiceCompatibility(config, serviceType)) {
                throw new NotSupportedException("UnitTemplate[" + serviceType.name() + "]", config.getLabel());
            }

            UnitRemote remote = Units.getUnit(config, false);

            if (!unitRemoteTypeMap.containsKey(remote.getType())) {
                unitRemoteTypeMap.put(remote.getType(), new ArrayList());
                for (UnitType superType : Registries.getUnitRegistry().getSuperUnitTypes(remote.getType())) {
                    if (!unitRemoteTypeMap.containsKey(superType)) {
                        unitRemoteTypeMap.put(superType, new ArrayList<>());
                    }
                }
            }

            try {
                serviceMap.put(config.getId(), (S) remote);
                unitRemoteTypeMap.get(remote.getType()).add((S) remote);
                for (UnitType superType : Registries.getUnitRegistry().getSuperUnitTypes(remote.getType())) {
                    unitRemoteTypeMap.get(superType).add((S) remote);
                }
            } catch (ClassCastException ex) {
                throw new NotSupportedException("ServiceInterface[" + serviceType.name() + "]", remote, "Remote does not implement the service interface!", ex);
            }

            unitRemoteMap.put(config.getId(), remote);

            if (active) {
                if (!remote.isEnabled()) {
                    logger.warn("Using a disabled " + remote + " in " + this + " is not recommended and should be avoided!");
                }
                remote.addDataObserver(dataObserver);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * Initializes this service remote with a set of unit configurations. Each
     * of the units referred by the given configurations should provide the
     * service type of this service remote.
     *
     * @param configs a set of unit configurations.
     * @throws InitializationException is thrown if the service remote could not
     * be initialized.
     * @throws InterruptedException is thrown if the current thread is
     * externally interrupted.
     */
    @Override
    public void init(final Collection<UnitConfig> configs) throws InitializationException, InterruptedException {
        try {
            verifyMaintainability();
            MultiException.ExceptionStack exceptionStack = null;
            for (UnitConfig config : configs) {
                try {
                    init(config);
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
            MultiException.checkAndThrow("Could not activate all service units!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        verifyMaintainability();
        active = true;
        unitRemoteMap.values().stream().map((remote) -> {
            if (!remote.isEnabled()) {
                logger.warn("Using a disabled " + remote + " in " + this + " is not recommended and should be avoided!");
            }
            return remote;
        }).forEach((remote) -> {
            remote.addDataObserver(dataObserver);
        });
        updateServiceState();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        verifyMaintainability();
        active = false;
        unitRemoteMap.values().stream().forEach((remote) -> {
            remote.removeDataObserver(dataObserver);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        try {
            verifyMaintainability();
            deactivate();
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(new ShutdownException(this, ex), logger);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void removeUnit(UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        unitRemoteMap.get(unitConfig.getId()).removeDataObserver(dataObserver);
        unitRemoteMap.remove(unitConfig.getId());
    }

    /**
     * Returns a collection of all internally used unit remotes.
     *
     * @return a collection of unit remotes limited to the service interface.
     */
    @Override
    public Collection<org.openbase.bco.dal.lib.layer.unit.UnitRemote> getInternalUnits() {
        return Collections.unmodifiableCollection(unitRemoteMap.values());
    }

    @Override
    public boolean hasInternalRemotes() {
        return !unitRemoteMap.isEmpty();
    }

    /**
     * Returns a collection of all internally used unit remotes.
     *
     * @return a collection of unit remotes limited to the service interface.
     */
    @Override
    public Collection<S> getServices() {
        return Collections.unmodifiableCollection(serviceMap.values());
    }

    /**
     * Returns a collection of all internally used unit remotes filtered by the
     * given unit type.
     *
     * @return a collection of unit remotes limited to the service interface.
     */
    @Override
    public Collection<S> getServices(final UnitType unitType) {
        if (unitType == UnitType.UNKNOWN) {
            return Collections.unmodifiableCollection(serviceMap.values());
        }

        if (!unitRemoteTypeMap.containsKey(unitType)) {
            return new ArrayList<>();
        }

        return Collections.unmodifiableCollection(unitRemoteTypeMap.get(unitType));
    }

    /**
     * Returns the service type of this remote.
     *
     * @return the remote service type.
     */
    @Override
    public ServiceType getServiceType() {
        return serviceType;
    }

    @Override
    public Future<Void> applyAction(final ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        try {
            if (!actionConfig.getServiceType().equals(getServiceType())) {
                throw new VerificationFailedException("Service type is not compatible to given action config!");
            }

            List<Future> actionFutureList = new ArrayList<>();

            for (final UnitRemote remote : getInternalUnits()) {
                actionFutureList.add(remote.applyAction(actionConfig));
            }
            return GlobalCachedExecutorService.allOf(actionFutureList, (Void) null);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply action!", ex);
        }
    }

    /**
     * Method blocks until an initial data message was dataObserverreceived from
     * every remote controller.
     *
     * @throws CouldNotPerformException is thrown if any error occurs.
     * @throws InterruptedException is thrown in case the thread is externally
     * interrupted.
     */
    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        if (unitRemoteMap.isEmpty()) {
            return;
        }

        for (UnitRemote remote : unitRemoteMap.values()) {
            remote.waitForData();
        }
        serviceStateObservable.waitForValue();
    }

    /**
     * Method blocks until an initial data message was received from every
     * remote controller or the given timeout is reached.
     *
     * @param timeout maximal time to wait for the main controller data. After
     * the timeout is reached a TimeoutException is thrown.
     * @param timeUnit the time unit of the timeout.
     * @throws CouldNotPerformException is thrown in case the any error occurs,
     * or if the given timeout is reached. In this case a TimeoutException is
     * thrown.
     * @throws InterruptedException is thrown in case the thread is externally
     * interrupted.
     */
    @Override
    public void waitForData(final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        if (unitRemoteMap.isEmpty()) {
            return;
        }
        //todo reimplement with respect to the given timeout.
        for (UnitRemote remote : unitRemoteMap.values()) {
            remote.waitForData(timeout, timeUnit);
        }
        serviceStateObservable.waitForValue(timeout, timeUnit);
    }

    /**
     * Checks if a server connection is established for every underlying remote.
     *
     * @return is true in case that the connection for every underlying remote
     * it established.
     */
    @Override
    public boolean isConnected() {
        return getInternalUnits().stream().noneMatch((unitRemote) -> (!unitRemote.isConnected()));
    }

    /**
     * Check if the data object is already available for every underlying
     * remote.
     *
     * @return is true in case that for every underlying remote data is
     * available.
     */
    @Override
    public boolean isDataAvailable() {
        if (!hasInternalRemotes()) {
            return false;
        }
        return serviceStateObservable.isValueAvailable();
    }

    public static boolean verifyServiceCompatibility(final UnitConfig unitConfig, final ServiceType serviceType) {
        return unitConfig.getServiceConfigList().stream().anyMatch((serviceConfig) -> (serviceConfig.getServiceTemplate().getType() == serviceType));
    }

    /**
     * {@inheritDoc}
     *
     * @throws VerificationFailedException {@inheritDoc}
     */
    @Override
    public void verifyMaintainability() throws VerificationFailedException {
        if (isLocked()) {
            throw new VerificationFailedException("Manipulation of " + this + "is currently not valid because the maintains is protected by another instance! "
                    + "Did you try to modify an instance which is locked by a managed instance pool?");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isLocked() {
        synchronized (maintainerLock) {
            return maintainer != null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void lock(final Object maintainer) throws CouldNotPerformException {
        synchronized (maintainerLock) {
            if (this.maintainer != null) {
                throw new CouldNotPerformException("Could not lock remote for because remote is already locked by another instance!");
            }
            this.maintainer = maintainer;
        }
    }

    /**
     * Method unlocks this instance.
     *
     * @param maintainer the instance which currently holds the lock.
     * @throws CouldNotPerformException is thrown if the instance could not be
     * unlocked.
     */
    @Override
    public void unlock(final Object maintainer) throws CouldNotPerformException {
        synchronized (maintainerLock) {
            if (this.maintainer != null && this.maintainer != maintainer) {
                throw new CouldNotPerformException("Could not unlock remote for because remote is locked by another instance!");
            }
            this.maintainer = null;
        }
    }

    public void setInfrastructureFilter(final boolean enabled) {
        // TODO: just a hack, remove me later
    }

    /**
     * Returns a short instance description.
     *
     * @return a description as string.
     */
    @Override
    public String toString() {
        if (serviceType == null) {
            return getClass().getSimpleName() + "[serviceType: ? ]";
        }
        return getClass().getSimpleName() + "[serviceType:" + serviceType.name() + "]";
    }
}
