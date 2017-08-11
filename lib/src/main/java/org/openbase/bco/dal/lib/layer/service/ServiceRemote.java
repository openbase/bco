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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.Remote;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <S>
 * @param <ST>
 */
public interface ServiceRemote<S extends Service, ST extends GeneratedMessage> extends Manageable<UnitConfigType.UnitConfig>, Service, Remote<ST>, ServiceProvider {

    /**
     * Add an observer to get notifications when the service state changes.
     *
     * @param observer the observer which is notified
     */
    @Override
    void addDataObserver(final Observer<ST> observer);

    /**
     * Returns a collection of all internally used unit remotes.
     *
     * @return a collection of unit remotes limited to the service interface.
     */
    Collection<UnitRemote> getInternalUnits();

    /**
     * Return a collection of all internally used unit remotes with the given type
     * and base units.
     * 
     * @param unitType the type witch fill the internal unit remotes are filtered
     * @return an unmodifiable collection of unit remotes limited limited to the type and service interface.
     * @throws NotAvailableException thrown if the type of an internally used unit remote is not available
     * @throws java.lang.InterruptedException
     */
    Collection<UnitRemote> getInternalUnits(UnitType unitType) throws CouldNotPerformException, InterruptedException;

    /**
     *
     * @return the current service state
     * @throws NotAvailableException if the service state has not been set at least once.
     * @deprecated please use getData instead.
     */
    @Deprecated
    ST getServiceState() throws NotAvailableException;

    /**
     * Returns the service type of this remote.
     *
     * @return the remote service type.
     */
    ServiceType getServiceType();

    /**
     * Returns a collection of all internally used unit remotes.
     *
     * @return a collection of unit remotes limited to the service interface.
     */
    Collection<S> getServices();

    /**
     * Returns a collection of all internally used unit remotes filtered by the given unit type.
     *
     * @param unitType the unit type after which the services are filtered
     * @return a collection of unit remotes limited to the service interface.
     */
    Collection<S> getServices(final UnitType unitType);

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
    @Override
    boolean isConnected();

    /**
     * Check if the data object is already available for every underlying remote.
     *
     * @return is true in case that for every underlying remote data is available.
     */
    @Override
    boolean isDataAvailable();

    /**
     * Remove an observer for the service state.
     *
     * @param observer the observer which has been registered
     */
    @Override
    void removeDataObserver(final Observer<ST> observer);

    /**
     * Removes the internal unit remote referred by the given config.
     *
     * @param unitConfig
     * @throws CouldNotPerformException
     * @throws InterruptedException
     */
    void removeUnit(UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException;

    /**
     * Method blocks until an initial data message was dataObserverreceived from every remote controller.
     *
     * @throws CouldNotPerformException is thrown if any error occurs.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    @Override
    void waitForData() throws CouldNotPerformException, InterruptedException;

    /**
     * Method blocks until an initial data message was received from every remote controller or the given timeout is reached.
     *
     * @param timeout maximal time to wait for the main controller data. After the timeout is reached a TimeoutException is thrown.
     * @param timeUnit the time unit of the timeout.
     * @throws CouldNotPerformException is thrown in case the any error occurs, or if the given timeout is reached. In this case a TimeoutException is thrown.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    @Override
    public void waitForData(final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException;

    @Override
    public default void activate(boolean waitForData) throws CouldNotPerformException, InterruptedException {
        activate();
        waitForData();
    }

    /**
     * Method adds the given observer to all internal unit remotes.
     *
     * @param observer the observer to observe the connection state of the internal unit remotes.
     */
    @Override
    public default void addConnectionStateObserver(Observer<ConnectionState> observer) {
        for (Remote remote : getInternalUnits()) {
            remote.addConnectionStateObserver(observer);
        }
    }

    /**
     * Method removes the given observer on all internal unit remotes.
     *
     * @param observer the observer to remove.
     */
    @Override
    public default void removeConnectionStateObserver(Observer<ConnectionState> observer) {
        for (final Remote remote : getInternalUnits()) {
            remote.removeConnectionStateObserver(observer);
        }
    }

    /**
     * Method returns the connection state of the internal unit remotes.
     *
     * Note: While unit remotes return ConnectionState.CONNECTING if they try to reach the remote controller this getConnectionState method returns connecting if at least one internal unit remote is already connected.
     *
     * @return Method returns DISCONNECTED if non of the internal unit remotes is connected. It returns CONNECTING if at least one remote is connected and returns CONNECTED if all internal unit remotes are successfully connected.
     */
    @Override
    public default ConnectionState getConnectionState() {
        boolean disconnectedRemoteDetected = false;
        boolean connectedRemoteDetected = false;

        for (final Remote remote : getInternalUnits()) {
            switch (remote.getConnectionState()) {
                case CONNECTED:
                    connectedRemoteDetected = true;
                    break;
                case CONNECTING:
                case DISCONNECTED:
                    disconnectedRemoteDetected = true;
                    break;
                default:
                //ignore unknown connection state";
            }
        }

        if (disconnectedRemoteDetected && connectedRemoteDetected) {
            return ConnectionState.CONNECTING;
        } else if (disconnectedRemoteDetected) {
            return ConnectionState.DISCONNECTED;
        } else if (connectedRemoteDetected) {
            return ConnectionState.CONNECTED;
        } else {
            return ConnectionState.UNKNOWN;
        }
    }

    /**
     * Method request the data of all internal unit remotes.
     *
     * @return the recalculated server state data based on the newly requested data.
     * @throws CouldNotPerformException is thrown if any error occurs during the request.
     */
    @Override
    public default CompletableFuture<ST> requestData() throws CouldNotPerformException {
        return requestData(true);
    }

    /**
     * Method request the data of all internal unit remotes.
     *
     * @param failOnError flag decides if an exception should be thrown in case one data request fails.
     * @return the recalculated server state data based on the newly requested data.
     * @throws CouldNotPerformException is thrown if non of the request was successful. In case the failOnError is set to true any request error throws an CouldNotPerformException.
     */
    public CompletableFuture<ST> requestData(final boolean failOnError) throws CouldNotPerformException;

}
