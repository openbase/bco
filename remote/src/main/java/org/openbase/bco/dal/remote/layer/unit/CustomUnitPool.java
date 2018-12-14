package org.openbase.bco.dal.remote.layer.unit;

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

import com.google.protobuf.Message;
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitFilters;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.ProtobufListDiff;
import org.openbase.jul.iface.DefaultInitializable;
import org.openbase.jul.pattern.Filter;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.storage.registry.RemoteControllerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CustomUnitPool implements DefaultInitializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomUnitPool.class);

    private final ReentrantReadWriteLock UNIT_REMOTE_REGISTRY_LOCK = new ReentrantReadWriteLock();
    private final Observer<DataProvider<UnitRegistryData>, UnitRegistryData> unitRegistryDataObserver;
    private final Observer unitDataObserver;
    private final RemoteControllerRegistry<String, UnitRemote<? extends Message>> unitRemoteRegistry;
    private final ProtobufListDiff<String, UnitConfig, Builder> unitConfigDiff;
    private final Set<Filter<UnitConfig>> filterSet;
    private final ObservableImpl<ServiceStateProvider<Message>, Message> unitDataObservable;

    public CustomUnitPool(final Collection<Filter<UnitConfig>> filters) throws InstantiationException {
        this(filters.toArray(new Filter[filters.size()]));
    }

    public CustomUnitPool(final Filter<UnitConfig>... filters) throws InstantiationException {
        try {
            this.filterSet = new HashSet<>(Arrays.asList(filters));
            this.filterSet.add(UnitFilters.DISABELED_UNIT_FILTER);
            this.unitConfigDiff = new ProtobufListDiff<>();
            this.unitRemoteRegistry = new RemoteControllerRegistry<>();
            this.unitDataObservable = new ObservableImpl<>();
            this.unitRegistryDataObserver = (source, data) -> {
                sync();
            };
            this.unitDataObserver = (source, data) -> {
                try {
                    unitDataObservable.notifyObservers((ServiceStateProvider<Message>) source, (Message) data);
                } catch (ClassCastException ex) {
                    ExceptionPrinter.printHistory("Could not handle incoming data because type is unknown!", ex, LOGGER);
                }
            };

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            Registries.getUnitRegistry().addDataObserver(unitRegistryDataObserver);
            if (Registries.getUnitRegistry().isDataAvailable()) {
                sync();
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void sync() throws InterruptedException {
        try {
            UNIT_REMOTE_REGISTRY_LOCK.writeLock().lock();
            unitConfigDiff.diff(Registries.getUnitRegistry().getUnitConfigs());
            try {

                // handle new units
                for (Entry<String, IdentifiableMessage<String, UnitConfig, Builder>> entry : unitConfigDiff.getNewMessageMap().entrySet()) {

                    // apply unit filter
                    for (Filter<UnitConfig> filter : filterSet) {
                        if (filter.match(entry.getValue().getMessage())) {
                            continue;
                        }
                    }
                    addUnitRemote(entry.getKey());
                }

                // handle updated units
                for (Entry<String, IdentifiableMessage<String, UnitConfig, Builder>> entry : unitConfigDiff.getUpdatedMessageMap().entrySet()) {

                    for (Filter<UnitConfig> filter : filterSet) {

                        // remove known units which match the filter
                        if (filter.match(entry.getValue().getMessage()) && unitRemoteRegistry.contains(entry.getKey())) {
                            removeUnitRemote(entry.getKey());
                        }

                        // add unknown units which pass the filter
                        if (filter.pass(entry.getValue().getMessage()) && !unitRemoteRegistry.contains(entry.getKey())) {
                            addUnitRemote(entry.getKey());
                        }
                    }
                }

                //handle removed units
                for (Entry<String, IdentifiableMessage<String, UnitConfig, Builder>> entry : unitConfigDiff.getRemovedMessageMap().entrySet()) {
                    if (unitRemoteRegistry.contains(entry.getKey())) {
                        removeUnitRemote(entry.getKey());
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
            final UnitRemote<?> unitRemote = Units.getUnit(unitId, false);
            unitRemoteRegistry.register(unitRemote);
            for (ServiceType serviceType : unitRemote.getAvailableServiceTypes()) {
                unitRemote.addServiceStateObserver(serviceType, unitDataObserver);
            }
            //unitRemote.addDataObserver(unitDataObserver);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not add " + unitId, ex, LOGGER);
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
                unitRemote.removeServiceStateObserver(serviceType, unitDataObserver);
            }
            //unitRemote.removeDataObserver(unitDataObserver);
            unitRemoteRegistry.remove(unitRemote);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not remove " + unitId, ex, LOGGER);
        }
    }

    /**
     * Method registers the given observer to all internal unit remotes to get informed about state changes.
     *
     * @param observer is the observer to register.
     */
    public void addObserver(Observer<ServiceStateProvider<Message>, Message> observer) {
        unitDataObservable.addObserver(observer);
    }

    /**
     * Method removes the given observer from all internal unit remotes.
     *
     * @param observer is the observer to remove.
     */
    public void removeObserver(Observer<ServiceStateProvider<Message>, Message> observer) {
        unitDataObservable.removeObserver(observer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
