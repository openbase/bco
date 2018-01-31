package org.openbase.bco.dal.remote.service;

/*-
 * #%L
 * BCO DAL Remote
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import org.openbase.bco.dal.lib.layer.unit.UnitProcessor;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.NotSupportedException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Activatable;
import org.openbase.jul.iface.Processable;
import org.openbase.jul.iface.Snapshotable;
import org.openbase.jul.iface.provider.LabelProvider;
import org.openbase.jul.iface.provider.PingProvider;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.Remote;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.processing.Processor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.SnapshotType;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.jul.exception.printer.LogLevel;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class ServiceRemoteManager<D> implements Activatable, Snapshotable<Snapshot>, PingProvider, DataProvider<D> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRemoteManager.class);

    private boolean active;
    private long connectionPing;
    private final SyncObject serviceRemoteMapLock = new SyncObject("ServiceRemoteMapLock");
    private final ServiceRemoteFactory serviceRemoteFactory;
    private final Map<ServiceType, AbstractServiceRemote> serviceRemoteMap;
    private final Observer serviceDataObserver;
    private final DataProvider<D> responsibleInstance;

    public ServiceRemoteManager(final DataProvider<D> responsibleInstance) {
        this.responsibleInstance = responsibleInstance;
        this.serviceRemoteMap = new HashMap<>();
        this.serviceRemoteFactory = ServiceRemoteFactoryImpl.getInstance();

        serviceDataObserver = (Observer) (Observable source, Object data) -> {
            notifyServiceUpdate(source, data);
        };
    }

    public synchronized void applyConfigUpdate(final List<String> unitIDList) throws CouldNotPerformException, InterruptedException {
        Registries.getUnitRegistry().waitForData();
        synchronized (serviceRemoteMapLock) {
            // shutdown all existing instances.
            for (final AbstractServiceRemote serviceRemote : serviceRemoteMap.values()) {
                serviceRemote.removeDataObserver(serviceDataObserver);
                serviceRemote.shutdown();
            }
            serviceRemoteMap.clear();

            // init a new set for each supported service type.
            Map<ServiceType, Set<UnitConfig>> serviceMap = new HashMap<>();
            for (final ServiceType serviceType : ServiceType.values()) {
                serviceMap.put(serviceType, new HashSet<>());
            }

            // init service unit map
            for (final String unitId : unitIDList) {

                try {

                    // resolve unit config by unit registry
                    final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);

                    // filter non dal units
                    try {
                        if (!UnitConfigProcessor.isDalUnit(unitConfig)) {
                            continue;
                        }
                    } catch (VerificationFailedException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("UnitConfig[" + unitConfig + "] could not be verified as a dal unit!", ex), LOGGER);
                    }

                    // sort dal unit by service type
                    unitConfig.getServiceConfigList().stream().forEach((serviceConfig) -> {
                        // register unit for service type. UnitConfigs are may added twice because of dublicated type of different service pattern but are filtered by the set.
                        serviceMap.get(serviceConfig.getServiceDescription().getType()).add(unitConfig);
                    });

                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not process unit config update of Unit[" + unitId + "]!", ex), LOGGER);
                }
            }

            // initialize service remotes
            for (final ServiceType serviceType : getManagedServiceTypes()) {
                final AbstractServiceRemote serviceRemote = serviceRemoteFactory.newInitializedInstance(serviceType, serviceMap.get(serviceType));
                serviceRemoteMap.put(serviceType, serviceRemote);

                // if already active than update the current location state.
                synchronized (serviceRemoteMapLock) {
                    if (isActive()) {
                        serviceRemote.addDataObserver(serviceDataObserver);
                        serviceRemote.activate();
                    }
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

    /**
     * Method checks if the given {@code ServiceType} is currently available by this {@code ServiceRemoteManager}
     *
     * @param serviceType the {@code ServiceType} to check.
     * @return returns true if the {@code ServiceType} is available, otherwise false.
     */
    public boolean isServiceAvailable(final ServiceType serviceType) {
        try {
            return getServiceRemote(serviceType).hasInternalRemotes();
        } catch (NotAvailableException ex) {
            // no service entry means the service is not available.
            return false;
        }
    }

    public AbstractServiceRemote getServiceRemote(final ServiceType serviceType) throws NotAvailableException {
        synchronized (serviceRemoteMapLock) {
            AbstractServiceRemote serviceRemote = serviceRemoteMap.get(serviceType);
            if (serviceRemote == null) {
                final String responsible = (responsibleInstance != null ? responsibleInstance.toString() : "the underlying instance");
                throw new NotAvailableException("ServiceRemote", serviceType.name(), new NotSupportedException("ServiceType[" + serviceType + "]", responsible));
            }
            return serviceRemote;
        }
    }

    public <B> B updateBuilderWithAvailableServiceStates(final B builder) throws InterruptedException, CouldNotPerformException {
        return updateBuilderWithAvailableServiceStates(builder, responsibleInstance.getDataClass(), getManagedServiceTypes());
    }

    public <B> B updateBuilderWithAvailableServiceStates(final B builder, final Class dataClass, final Set<ServiceType> supportedServiceTypeSet) throws InterruptedException {
        try {
            for (final ServiceTemplateType.ServiceTemplate.ServiceType serviceType : supportedServiceTypeSet) {

                final Object serviceState;

                try {
                    final AbstractServiceRemote serviceRemote = getServiceRemote(serviceType);
                    /* When the locationRemote is active and a config update occurs the serviceRemoteManager clears
                     * its map of service remotes and fills it with new ones. When they are activated an update is triggered while
                     * the map is not completely filled. Therefore the serviceRemote can be null.
                     */
                    if (serviceRemote == null) {
                        continue;
                    }
                    if (!serviceRemote.isDataAvailable()) {
                        continue;
                    }

                    serviceState = Services.invokeProviderServiceMethod(serviceType, serviceRemote);
                } catch (NotAvailableException ex) {
                    ExceptionPrinter.printHistory("No service data for type[" + serviceType + "] on location available!", ex, LOGGER);
                    continue;
                } catch (NotSupportedException | IllegalArgumentException ex) {
                    ExceptionPrinter.printHistory(new FatalImplementationErrorException(this, ex), LOGGER);
                    continue;
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update ServiceState[" + serviceType.name() + "] for " + this, ex, LOGGER);
                    continue;
                }

                try {
                    Services.invokeOperationServiceMethod(serviceType, builder, serviceState);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new NotSupportedException("Field[" + serviceType.name().toLowerCase().replace("_service", "") + "] is missing in protobuf type " + dataClass + "!", this, ex), LOGGER);
                }
            }
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                throw (InterruptedException) ex;
            }
            new CouldNotPerformException("Could not update current status!", ex);
        }
        return builder;
    }

    @Override
    public Future<SnapshotType.Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        return recordSnapshot(UnitTemplateType.UnitTemplate.UnitType.UNKNOWN);
    }

    public Future<Snapshot> recordSnapshot(final UnitType unitType) throws CouldNotPerformException, InterruptedException {
        return GlobalCachedExecutorService.submit(() -> {
            try {
                SnapshotType.Snapshot.Builder snapshotBuilder = SnapshotType.Snapshot.newBuilder();
                Set<UnitRemote> unitRemoteSet = new HashSet<>();

                if (unitType == UnitType.UNKNOWN) {
                    // if the type is unknown then take the snapshot for all units
                    getServiceRemoteList().stream().forEach((serviceRemote) -> {
                        unitRemoteSet.addAll(serviceRemote.getInternalUnits());
                    });
                } else {
                    // for efficiency reasons only one serviceType implemented by the unitType is regarded because the unitRemote is part of
                    // every abstractServiceRemotes internal units if the serviceType is implemented by the unitType
                    ServiceType serviceType;
                    try {
                        serviceType = Registries.getUnitRegistry().getUnitTemplateByType(unitType).getServiceDescriptionList().get(0).getType();
                    } catch (IndexOutOfBoundsException ex) {
                        // if there is not at least one serviceType for the unitType then the snapshot is empty
                        return snapshotBuilder.build();
                    }

                    for (final AbstractServiceRemote abstractServiceRemote : getServiceRemoteList()) {
                        if (!(serviceType == abstractServiceRemote.getServiceType())) {
                            continue;
                        }

                        Collection<UnitRemote> internalUnits = abstractServiceRemote.getInternalUnits();
                        for (final UnitRemote unitRemote : internalUnits) {
                            // just add units with the according type
                            if (unitRemote.getUnitType() == unitType) {
                                unitRemoteSet.add(unitRemote);
                            }
                        }
                    }
                }

                // take the snapshot
                final Map<UnitRemote, Future<SnapshotType.Snapshot>> snapshotFutureMap = new HashMap<UnitRemote, Future<SnapshotType.Snapshot>>();
                for (final UnitRemote<?> remote : unitRemoteSet) {
                    try {
                        if (UnitProcessor.isDalUnit(remote)) {
                            if (!remote.isConnected()) {
                                throw new NotAvailableException("Unit[" + remote.getLabel() + "] is currently not reachable!");
                            }
                            snapshotFutureMap.put(remote, remote.recordSnapshot());
                        }
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not record snapshot of " + remote.getLabel(), ex), LOGGER, LogLevel.WARN);
                    }
                }

                // build snapshot
                for (final Map.Entry<UnitRemote, Future<SnapshotType.Snapshot>> snapshotFutureEntry : snapshotFutureMap.entrySet()) {
                    try {
                        snapshotBuilder.addAllServiceStateDescription(snapshotFutureEntry.getValue().get(5, TimeUnit.SECONDS).getServiceStateDescriptionList());
                    } catch (ExecutionException | TimeoutException ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not record snapshot of " + snapshotFutureEntry.getKey().getLabel(), ex), LOGGER);
                    }
                }
                return snapshotBuilder.build();
            } catch (final CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not record snapshot!", ex);
            }
        });
    }

    @Override
    public Future<Void> restoreSnapshot(final Snapshot snapshot) throws CouldNotPerformException, InterruptedException {
        try {
            final Map<String, UnitRemote<?>> unitRemoteMap = new HashMap<>();
            for (AbstractServiceRemote<?, ?> serviceRemote : this.getServiceRemoteList()) {
                for (UnitRemote<?> unitRemote : serviceRemote.getInternalUnits()) {
                    unitRemoteMap.put(unitRemote.getId(), unitRemote);
                }
            }

            Collection<Future> futureCollection = new ArrayList<>();
            for (final ServiceStateDescription serviceStateDescription : snapshot.getServiceStateDescriptionList()) {
                ActionDescription actionDescription = ActionDescription.newBuilder().setServiceStateDescription(serviceStateDescription).build();
                futureCollection.add(unitRemoteMap.get(serviceStateDescription.getUnitId()).applyAction(actionDescription));
            }
            return GlobalCachedExecutorService.allOf(futureCollection);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not record snapshot!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<Long> ping() {
        synchronized (serviceRemoteMapLock) {
            if (serviceRemoteMap.isEmpty()) {
                return CompletableFuture.completedFuture(0l);
            }

            final List<Future<Long>> futurePings = new ArrayList<>();

            for (final Remote<?> remote : serviceRemoteMap.values()) {
                if (remote.isConnected()) {
                    futurePings.add(remote.ping());
                }
            }

            return GlobalCachedExecutorService.allOf(input -> {
                try {
                    long sum = 0;
                    for (final Future<Long> future : input) {
                        sum += future.get();
                    }

                    long ping;
                    if (!input.isEmpty()) {
                        ping = sum / input.size();
                    } else {
                        ping = 0;
                    }
                    connectionPing = ping;
                    return ping;
                } catch (ExecutionException ex) {
                    throw new CouldNotPerformException("Could not compute ping!", ex);
                }
            }, futurePings);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Long getPing() {
        return connectionPing;
    }

    public Future<ActionFuture> applyAction(final ActionDescription actionDescription) throws CouldNotPerformException, InterruptedException {
        return getServiceRemote(actionDescription.getServiceStateDescription().getServiceType()).applyAction(actionDescription);
    }

    protected abstract Set<ServiceType> getManagedServiceTypes() throws NotAvailableException, InterruptedException;

    protected abstract void notifyServiceUpdate(final Observable source, final Object data) throws NotAvailableException, InterruptedException;

    @Override
    public boolean isDataAvailable() {
        return responsibleInstance.isDataAvailable();
    }

    @Override
    public Class<D> getDataClass() {
        return responsibleInstance.getDataClass();
    }

    @Override
    public D getData() throws NotAvailableException {
        return responsibleInstance.getData();
    }

    @Override
    public CompletableFuture<D> getDataFuture() {
        return responsibleInstance.getDataFuture();
    }

    @Override
    public void addDataObserver(final Observer<D> observer) {
        synchronized (serviceRemoteMapLock) {
            for (final Remote<D> remote : serviceRemoteMap.values()) {
                remote.addDataObserver(observer);
            }
        }
    }

    @Override
    public void removeDataObserver(Observer<D> observer) {
        synchronized (serviceRemoteMapLock) {
            for (final Remote<D> remote : serviceRemoteMap.values()) {
                remote.removeDataObserver(observer);
            }
        }
    }

    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        synchronized (serviceRemoteMapLock) {
            for (final Remote<D> remote : serviceRemoteMap.values()) {
                remote.waitForData();
            }
        }
    }

    @Override
    public void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        synchronized (serviceRemoteMapLock) {
            for (final Remote<D> remote : serviceRemoteMap.values()) {
                remote.waitForData(timeout, timeUnit);
            }
        }
    }

    public <B> Future<B> requestData(final B builder) throws CouldNotPerformException {
        synchronized (serviceRemoteMapLock) {
            final List<Future> futureData = new ArrayList<>();

            for (final Remote<?> remote : serviceRemoteMap.values()) {
                futureData.add(remote.requestData());
            }

            return GlobalCachedExecutorService.allOf(() -> {
                try {
                    return updateBuilderWithAvailableServiceStates(builder);
                } catch (CouldNotPerformException ex) {
                    throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not generate data!", ex), LOGGER);
                }
            }, futureData);
        }
    }
}
