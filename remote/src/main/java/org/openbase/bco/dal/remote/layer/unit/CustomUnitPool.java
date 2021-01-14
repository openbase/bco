package org.openbase.bco.dal.remote.layer.unit;

/*-
 * #%L
 * BCO DAL Remote
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
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.filter.UnitConfigFilterImpl;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.ProtobufListDiff;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Filter;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.storage.registry.RemoteControllerRegistry;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitFilterType.UnitFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CustomUnitPool implements Manageable<Collection<Filter<UnitConfig>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomUnitPool.class);

    private final ReentrantReadWriteLock UNIT_REMOTE_REGISTRY_LOCK = new ReentrantReadWriteLock();
    private final Observer<DataProvider<UnitRegistryData>, UnitRegistryData> unitRegistryDataObserver;
    private final Observer unitDataObserver;
    private final Observer serviceStateObserver;
    private final RemoteControllerRegistry<String, UnitRemote<? extends Message>> unitRemoteRegistry;
    private final ProtobufListDiff<String, UnitConfig, Builder> unitConfigDiff;
    private final Set<Filter<UnitConfig>> filterSet;
    private final ObservableImpl<Unit<Message>, Message> unitDataObservable;
    private final ObservableImpl<ServiceStateProvider<Message>, Message> serviceStateObservable;
    private volatile boolean active;

    public CustomUnitPool() throws InstantiationException {
        try {
            this.filterSet = new HashSet<>();
            this.unitConfigDiff = new ProtobufListDiff<>();
            this.unitRemoteRegistry = new RemoteControllerRegistry<>();
            this.unitRegistryDataObserver = (source, data) -> {
                sync();
            };
            this.unitDataObservable = new ObservableImpl<>();
            this.unitDataObserver = (source, message) -> {
                try {
                    unitDataObservable.notifyObservers(((Unit) source), (Message) message);
                } catch (ClassCastException ex) {
                    ExceptionPrinter.printHistory("Could not handle incoming data because type is unknown!", ex, LOGGER);
                }
            };
            this.serviceStateObservable = new ObservableImpl<>();
            this.serviceStateObserver = (source, data) -> {
                try {
                    serviceStateObservable.notifyObservers((ServiceStateProvider<Message>) source, (Message) data);
                } catch (ClassCastException ex) {
                    ExceptionPrinter.printHistory("Could not handle incoming data because type is unknown!", ex, LOGGER);
                }
            };

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * This filter initialization is optional.
     * Method sets a new filter set for the custom pool.
     * If the pool is already active, then the filter is directly applied.
     * If you call this method twice, only the latest filter set is used.
     *
     * @param unitFilter this is a unit filter that can be used to limit the number of unit to observer. No filter means every unit is observed by this pool.
     *
     * @throws InitializationException is throw if the initialization fails.
     * @throws InterruptedException    is thrown if the thread was externally interrupted.
     */
    public final void init(final UnitFilter unitFilter) throws InitializationException, InterruptedException {
        init(new UnitConfigFilterImpl(unitFilter));
    }

    /**
     * This filter initialization is optional.
     * Method sets a new filter set for the custom pool.
     * If the pool is already active, then the filter is directly applied.
     * If you call this method twice, only the latest filter set is used.
     *
     * @param filters this set of filters can be used to limit the number of unit to observer. No filter means every unit is observed by this pool.
     *
     * @throws InitializationException is throw if the initialization fails.
     * @throws InterruptedException    is thrown if the thread was externally interrupted.
     */
    public final void init(final Filter<UnitConfig>... filters) throws InitializationException, InterruptedException {
        init(Arrays.asList(filters));
    }

    /**
     * This filter initialization is optional.
     * Method sets a new filter set for the custom pool.
     * If the pool is already active, then the filter is directly applied.
     * If you call this method twice, only the latest filter set is used.
     *
     * @param filters this set of filters can be used to limit the number of unit to observer. No filter means every unit is observed by this pool.
     *
     * @throws InitializationException is throw if the initialization fails.
     * @throws InterruptedException    is thrown if the thread was externally interrupted.
     */
    @Override
    public void init(final Collection<Filter<UnitConfig>> filters) throws InitializationException, InterruptedException {
        UNIT_REMOTE_REGISTRY_LOCK.writeLock().lockInterruptibly();
        try {
            filterSet.clear();
            filterSet.addAll(filters);
            if (active) {
                sync();
            }
        } finally {
            UNIT_REMOTE_REGISTRY_LOCK.writeLock().unlock();
        }
    }

    private void sync() throws InterruptedException {
        // skip if registry is not ready yet.
        try {
            if (!Registries.getUnitRegistry().isDataAvailable()) {
                return;
            }
        } catch (NotAvailableException e) {
            return;
        }

        try {
            UNIT_REMOTE_REGISTRY_LOCK.writeLock().lockInterruptibly();
            try {
                // todo: why not filter before diff? That would make so much things easier here.
                unitConfigDiff.diffMessages(Registries.getUnitRegistry().getUnitConfigs());

                // handle new units
                unitLoop:
                for (Entry<String, IdentifiableMessage<String, UnitConfig, Builder>> entry : unitConfigDiff.getNewMessageMap().entrySet()) {

                    // apply unit filter
                    for (Filter<UnitConfig> filter : filterSet) {
                        if (filter.pass(entry.getValue().getMessage())) {
                            continue unitLoop;
                        }
                    }
                    addUnitRemote(entry.getKey());
                }

                // handle updated units
                unitLoop:
                for (Entry<String, IdentifiableMessage<String, UnitConfig, Builder>> entry : unitConfigDiff.getUpdatedMessageMap().entrySet()) {

                    for (Filter<UnitConfig> filter : filterSet) {

                        // remove known units which pass the filter
                        if (filter.pass(entry.getValue().getMessage()) && unitRemoteRegistry.contains(entry.getKey())) {
                            removeUnitRemote(entry.getKey());
                            continue unitLoop;
                        }

                        // we are done if unit is already known
                        if (unitRemoteRegistry.contains(entry.getKey())) {
                            continue unitLoop;
                        }

                        // filter if required
                        if (filter.pass(entry.getValue().getMessage())) {
                            continue unitLoop;
                        }
                    }

                    // unit has not been removed, is not already in the pool and is not skipped by the filters, therefore we need to add it
                    addUnitRemote(entry.getKey());
                }

                //handle removed units
                for (Entry<String, IdentifiableMessage<String, UnitConfig, Builder>> entry : unitConfigDiff.getRemovedMessageMap().entrySet()) {
                    if (unitRemoteRegistry.contains(entry.getKey())) {
                        removeUnitRemote(entry.getKey());
                    }
                }

                // validate already registered units
                unitLoop:
                for (Unit<?> unit : new ArrayList<>(unitRemoteRegistry.getEntries())) {

                    // apply unit filter
                    for (Filter<UnitConfig> filter : filterSet) {
                        if (filter.pass(unit.getConfig())) {
                            removeUnitRemote(unit.getId());
                            continue unitLoop;
                        }
                    }
                }

            } finally {
                UNIT_REMOTE_REGISTRY_LOCK.writeLock().unlock();
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not sync " + this, ex, LOGGER);
        }
    }

    private void addUnitRemote(final String unitId) throws InterruptedException {
        try {

            if (!isActive()) {
                new FatalImplementationErrorException("unid remote registered but pool was never activated!", this);
            }

            final UnitRemote<?> unitRemote = Units.getUnit(unitId, false);
            unitRemoteRegistry.register(unitRemote);

            for (ServiceType serviceType : unitRemote.getAvailableServiceTypes()) {
                unitRemote.addServiceStateObserver(serviceType, serviceStateObserver);
            }
            unitRemote.addDataObserver(unitDataObserver);
        } catch (CouldNotPerformException ex) {
            if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory("Could not add " + unitId, ex, LOGGER);
            }
        }
    }

    private void removeUnitRemote(final String unitId) {
        try {
            final UnitRemote<?> unitRemote;
            try {
                unitRemote = unitRemoteRegistry.get(unitId);
            } catch (NotAvailableException ex) {
                // unit not registered so removal not necessary.
                return;
            }
            for (ServiceType serviceType : unitRemote.getAvailableServiceTypes()) {
                unitRemote.removeServiceStateObserver(serviceType, serviceStateObserver);
            }
            unitRemote.removeDataObserver(unitDataObserver);
            unitRemoteRegistry.remove(unitRemote);
        } catch (CouldNotPerformException ex) {
            if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory("Could not remove " + unitId, ex, LOGGER);
            }
        }
    }

    /**
     * Method registers the given observer to all internal unit remotes to get informed about state changes.
     *
     * @param observer is the observer to register.
     */
    public void addDataObserver(Observer<Unit<Message>, Message> observer) {
        unitDataObservable.addObserver(observer);
    }

    /**
     * Method removes the given observer from all internal unit remotes.
     *
     * @param observer is the observer to remove.
     */
    public void removeDataObserver(Observer<Unit<Message>, Message> observer) {
        unitDataObservable.removeObserver(observer);
    }

    /**
     * Method registers the given observer to all internal unit remotes to get informed about state changes.
     *
     * @param observer is the observer to register.
     */
    public void addServiceStateObserver(Observer<ServiceStateProvider<Message>, Message> observer) {
        serviceStateObservable.addObserver(observer);
    }

    /**
     * Method removes the given observer from all internal unit remotes.
     *
     * @param observer is the observer to remove.
     */
    public void removeServiceStateObserver(Observer<ServiceStateProvider<Message>, Message> observer) {
        serviceStateObservable.removeObserver(observer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        UNIT_REMOTE_REGISTRY_LOCK.writeLock().lockInterruptibly();
        try {

            // skip run if already active
            if (isActive()) {
                return;
            }

            // add observer
            Registries.getUnitRegistry().addDataObserver(unitRegistryDataObserver);

            active = true;

            // trigger initial sync
            if (Registries.getUnitRegistry().isDataAvailable()) {
                sync();
            }
        } finally {
            UNIT_REMOTE_REGISTRY_LOCK.writeLock().unlock();
        }
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        UNIT_REMOTE_REGISTRY_LOCK.writeLock().lockInterruptibly();
        try {
            active = false;
            try {
                Registries.getUnitRegistry().removeDataObserver(unitRegistryDataObserver);
            } catch (NotAvailableException ex) {
                // if the registry is not available an observer deregistration is not required.
                // This can for example be the case when the unit registry has already been terminated during the shutdown progress.
            }

            // deregister all observed units
            for (String unitId : new ArrayList<>(unitRemoteRegistry.getEntryMap().keySet())) {
                removeUnitRemote(unitId);
            }
        } finally {
            UNIT_REMOTE_REGISTRY_LOCK.writeLock().unlock();
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public List<UnitRemote<? extends Message>> getInternalUnitList() {
        return unitRemoteRegistry.getEntries();
    }
}
