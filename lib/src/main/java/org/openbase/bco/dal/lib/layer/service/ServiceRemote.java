
package org.openbase.bco.dal.lib.layer.service;

/*-
 * #%L
 * BCO DAL Library
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
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observer;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitTemplateType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <S>
 * @param <ST>
 */
public interface ServiceRemote<S extends Service, ST extends GeneratedMessage> extends Manageable<UnitConfigType.UnitConfig>, Service {


    /**
     * Add an observer to get notifications when the service state changes.
     *
     * @param observer the observer which is notified
     */
    void addDataObserver(final Observer<ST> observer);

    /**
     * Returns a collection of all internally used unit remotes.
     *
     * @return a collection of unit remotes limited to the service interface.
     */
    Collection<UnitRemote> getInternalUnits();

    /**
     *
     * @return the current service state
     * @throws NotAvailableException if the service state has not been set at least once.
     */
    ST getServiceState() throws NotAvailableException;

    /**
     * Returns the service type of this remote.
     *
     * @return the remote service type.
     */
    ServiceTemplateType.ServiceTemplate.ServiceType getServiceType();

    /**
     * Returns a collection of all internally used unit remotes.
     *
     * @return a collection of unit remotes limited to the service interface.
     */
    Collection<S> getServices();

    /**
     * Returns a collection of all internally used unit remotes filtered by the given unit type.
     *
     * @param unitType
     * @return a collection of unit remotes limited to the service interface.
     */
    Collection<S> getServices(final UnitTemplateType.UnitTemplate.UnitType unitType);

    boolean hasInternalRemotes();

    /**
     * Initializes this service remote with a set of unit configurations. Each of the units referred by the given configurations should provide the service type of this service remote.
     *
     * @param configs a set of unit configurations.
     * @throws InitializationException is thrown if the service remote could not be initialized.
     * @throws InterruptedException is thrown if the current thread is externally interrupted.
     */
    void init(final Collection<UnitConfigType.UnitConfig> configs) throws InitializationException, InterruptedException;

    /**
     * Checks if a server connection is established for every underlying remote.
     *
     * @return is true in case that the connection for every underlying remote it established.
     */
    boolean isConnected();

    /**
     * Check if the data object is already available for every underlying remote.
     *
     * @return is true in case that for every underlying remote data is available.
     */
    boolean isDataAvailable();

    /**
     * Remove an observer for the service state.
     *
     * @param observer the observer which has been registered
     */
    void removeDataObserver(final Observer<ST> observer);

    void removeUnit(UnitConfigType.UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException;

    /**
     * Method blocks until an initial data message was dataObserverreceived from every remote controller.
     *
     * @throws CouldNotPerformException is thrown if any error occurs.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    void waitForData() throws CouldNotPerformException, InterruptedException;

    /**
     * Method blocks until an initial data message was received from every remote controller or the given timeout is reached.
     *
     * @param timeout maximal time to wait for the main controller data. After the timeout is reached a TimeoutException is thrown.
     * @param timeUnit the time unit of the timeout.
     * @throws CouldNotPerformException is thrown in case the any error occurs, or if the given timeout is reached. In this case a TimeoutException is thrown.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    void waitForData(final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException;

}
