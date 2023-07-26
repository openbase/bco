package org.openbase.bco.dal.remote.layer.unit

import com.google.protobuf.Message
import org.openbase.bco.dal.lib.layer.service.ServiceStateProvider
import org.openbase.bco.dal.lib.layer.unit.Unit
import org.openbase.bco.dal.lib.layer.unit.UnitRemote
import org.openbase.bco.registry.remote.Registries
import org.openbase.bco.registry.unit.lib.filter.UnitConfigFilterImpl
import org.openbase.jul.exception.*
import org.openbase.jul.exception.ExceptionProcessor.isCausedBySystemShutdown
import org.openbase.jul.exception.printer.ExceptionPrinter
import org.openbase.jul.extension.protobuf.ProtobufListDiff
import org.openbase.jul.iface.Manageable
import org.openbase.jul.pattern.Filter
import org.openbase.jul.pattern.ObservableImpl
import org.openbase.jul.pattern.Observer
import org.openbase.jul.pattern.provider.DataProvider
import org.openbase.jul.storage.registry.RemoteControllerRegistry
import org.openbase.type.domotic.registry.UnitRegistryDataType
import org.openbase.type.domotic.unit.UnitConfigType
import org.openbase.type.domotic.unit.UnitFilterType
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock

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

class CustomUnitPool : Manageable<Collection<Filter<UnitConfigType.UnitConfig>>> {
    private val UNIT_REMOTE_REGISTRY_LOCK = ReentrantReadWriteLock()
    private var unitRegistryDataObserver: Observer<DataProvider<UnitRegistryDataType.UnitRegistryData>, UnitRegistryDataType.UnitRegistryData>
    private var unitDataObserver: Observer<DataProvider<Message>, Message>
    private var serviceStateObserver: Observer<ServiceStateProvider<*>, out Message>
    private var unitRemoteRegistry: RemoteControllerRegistry<String, UnitRemote<Message>>
    private var unitConfigDiff: ProtobufListDiff<String, UnitConfigType.UnitConfig, UnitConfigType.UnitConfig.Builder>
    private var filterSet: MutableSet<Filter<UnitConfigType.UnitConfig>>
    private var unitDataObservable: ObservableImpl<Unit<Message>, Message>
    private var serviceStateObservable: ObservableImpl<ServiceStateProvider<Message>, Message>

    @Volatile
    private var active = false


    init {
        try {
            filterSet = HashSet()
            unitConfigDiff = ProtobufListDiff()
            unitRemoteRegistry = RemoteControllerRegistry()
            unitRegistryDataObserver =
                Observer { source: DataProvider<UnitRegistryDataType.UnitRegistryData>, data: UnitRegistryDataType.UnitRegistryData? -> sync() }
            unitDataObservable = ObservableImpl()
            unitDataObserver = Observer { source: Any, message: Any ->
                try {
                    unitDataObservable.notifyObservers(source as Unit<Message>, message as Message)
                } catch (ex: ClassCastException) {
                    ExceptionPrinter.printHistory("Could not handle incoming data because type is unknown!", ex, LOGGER)
                }
            }
            serviceStateObservable = ObservableImpl()
            serviceStateObserver = Observer { source: Any, data: Any ->
                try {
                    serviceStateObservable.notifyObservers(source as ServiceStateProvider<Message>, data as Message)
                } catch (ex: ClassCastException) {
                    ExceptionPrinter.printHistory("Could not handle incoming data because type is unknown!", ex, LOGGER)
                }
            }
        } catch (ex: CouldNotPerformException) {
            throw InstantiationException(this, ex)
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
    @Throws(InitializationException::class, InterruptedException::class)
    fun init(unitFilter: UnitFilterType.UnitFilter) {
        init(UnitConfigFilterImpl(unitFilter))
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
    @Throws(InitializationException::class, InterruptedException::class)
    fun init(vararg filters: Filter<UnitConfigType.UnitConfig>) {
        init(filters.toList())
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
    @Throws(InitializationException::class, InterruptedException::class)
    override fun init(filters: Collection<Filter<UnitConfigType.UnitConfig>>) {
        UNIT_REMOTE_REGISTRY_LOCK.writeLock().lockInterruptibly()
        try {
            filterSet.clear()
            filterSet.addAll(filters)
            if (active) {
                sync()
            }
        } finally {
            UNIT_REMOTE_REGISTRY_LOCK.writeLock().unlock()
        }
    }

    @Throws(InterruptedException::class)
    private fun sync() {
        // skip if registry is not ready yet.
        try {
            if (!Registries.getUnitRegistry().isDataAvailable) {
                return
            }
        } catch (e: NotAvailableException) {
            return
        }
        try {
            UNIT_REMOTE_REGISTRY_LOCK.writeLock().lockInterruptibly()
            try {
                // update list diff
                Registries.getUnitRegistry().unitConfigs
                    .filter { unitConfig -> filterSet.all { it.match(unitConfig) } }
                    .let { unitConfigDiff.diffMessages(it) }

                // handle new units
                unitConfigDiff.newMessageMap.keys.forEach { addUnitRemote(it) }


                //handle removed units
                unitConfigDiff.removedMessageMap.keys.forEach { removeUnitRemote(it) }
            } finally {
                UNIT_REMOTE_REGISTRY_LOCK.writeLock().unlock()
            }
        } catch (ex: CouldNotPerformException) {
            ExceptionPrinter.printHistory("Could not sync $this", ex, LOGGER)
        }
    }

    @Throws(InterruptedException::class)
    private fun addUnitRemote(unitId: String) {
        try {
            if (!isActive) {
                FatalImplementationErrorException("unid remote registered but pool was never activated!", this)
            }
            val unitRemote: UnitRemote<Message> = Units.getUnit(unitId, false)
            unitRemoteRegistry.register(unitRemote)
            for (serviceType in unitRemote.availableServiceTypes) {
                unitRemote.addServiceStateObserver(serviceType, serviceStateObserver)
            }
            unitRemote.addDataObserver(unitDataObserver)
        } catch (ex: CouldNotPerformException) {
            if (!isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory("Could not add $unitId", ex, LOGGER)
            }
        }
    }

    private fun removeUnitRemote(unitId: String) {
        try {
            unitRemoteRegistry[unitId]?.let { unitRemote ->
                unitRemote.availableServiceTypes.forEach { serviceType ->
                    unitRemote.removeServiceStateObserver(serviceType, serviceStateObserver)
                }
                unitRemote.removeDataObserver(unitDataObserver)
                unitRemoteRegistry.remove(unitRemote)
            }
        } catch (ex: CouldNotPerformException) {
            if (!isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory("Could not remove $unitId", ex, LOGGER)
            }
        }
    }

    /**
     * Method registers the given observer to all internal unit remotes to get informed about state changes.
     *
     * @param observer is the observer to register.
     */
    fun addDataObserver(observer: Observer<Unit<Message>, Message>) {
        unitDataObservable.addObserver(observer)
    }

    /**
     * Method removes the given observer from all internal unit remotes.
     *
     * @param observer is the observer to remove.
     */
    fun removeDataObserver(observer: Observer<Unit<Message>, Message>) {
        unitDataObservable.removeObserver(observer)
    }

    /**
     * Method registers the given observer to all internal unit remotes to get informed about state changes.
     *
     * @param observer is the observer to register.
     */
    fun addServiceStateObserver(observer: Observer<ServiceStateProvider<Message>, Message>) {
        serviceStateObservable.addObserver(observer)
    }

    /**
     * Method removes the given observer from all internal unit remotes.
     *
     * @param observer is the observer to remove.
     */
    fun removeServiceStateObserver(observer: Observer<ServiceStateProvider<Message>, Message>) {
        serviceStateObservable.removeObserver(observer)
    }

    override fun toString(): String {
        return javaClass.simpleName
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun activate() {
        UNIT_REMOTE_REGISTRY_LOCK.writeLock().lockInterruptibly()
        try {

            // skip run if already active
            if (isActive) {
                return
            }

            // add observer
            Registries.getUnitRegistry().addDataObserver(unitRegistryDataObserver)
            active = true

            // trigger initial sync
            if (Registries.getUnitRegistry().isDataAvailable) {
                sync()
            }
        } finally {
            UNIT_REMOTE_REGISTRY_LOCK.writeLock().unlock()
        }
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    override fun deactivate() {
        UNIT_REMOTE_REGISTRY_LOCK.writeLock().lockInterruptibly()
        try {
            active = false
            try {
                Registries.getUnitRegistry().removeDataObserver(unitRegistryDataObserver)
            } catch (ex: NotAvailableException) {
                // if the registry is not available an observer deregistration is not required.
                // This can for example be the case when the unit registry has already been terminated during the shutdown progress.
            }

            // deregister all observed units
            for (unitId in ArrayList(unitRemoteRegistry.entryMap.keys)) {
                removeUnitRemote(unitId)
            }
        } finally {
            UNIT_REMOTE_REGISTRY_LOCK.writeLock().unlock()
        }
    }

    override fun isActive(): Boolean = active

    val internalUnitList: List<UnitRemote<out Message>>
        get() = unitRemoteRegistry.entries

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CustomUnitPool::class.java)
    }
}
