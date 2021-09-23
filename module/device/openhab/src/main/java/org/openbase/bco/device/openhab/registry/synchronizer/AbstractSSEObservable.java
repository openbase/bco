package org.openbase.bco.device.openhab.registry.synchronizer;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.google.gson.JsonObject;
import org.openbase.bco.device.openhab.communication.OpenHABRestCommunicator;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.pattern.provider.ObservableDataProviderAdapter;

/**
 * Abstract observable for server send events. Implementing classes need to provide an observer
 * that converts from received json object from the server to another type.
 *
 * @param <DTO> the type of data the implementing class converts the json object to.
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractSSEObservable<DTO> extends ObservableDataProviderAdapter<DTO> {

    private final String topicFilter;
    private int observerCount;
    private final Observer<Object, JsonObject> observer;

    /**
     * Create a new service send event observable that converts and filters received data.
     *
     * @param topicFilter a regex defining which SSE topics the observable filters
     * @param clazz       the class the observable converts received data to
     */
    public AbstractSSEObservable(final String topicFilter, Class<DTO> clazz) {
        super(new ObservableImpl<>(), clazz);
        this.topicFilter = topicFilter;
        this.observerCount = 0;
        this.observer = (observable, jsonObject) -> {
            if (filter(jsonObject)) {
                return;
            }

            getObservable().notifyObservers(convert(jsonObject));
        };
    }

    /**
     * {@inheritDoc}
     *
     * @param observer {@inheritDoc}
     */
    @Override
    public void addDataObserver(final Observer<DataProvider<DTO>, DTO> observer) {
        if (observerCount == 0) {
            OpenHABRestCommunicator.getInstance().addSSEObserver(this.observer, topicFilter);
        }

        observerCount++;
        super.addDataObserver(observer);
    }

    /**
     * {@inheritDoc}
     *
     * @param observer {@inheritDoc}
     */
    @Override
    public void removeDataObserver(Observer<DataProvider<DTO>, DTO> observer) {
        super.removeDataObserver(observer);

        observerCount--;
        if (observerCount == 0) {
            OpenHABRestCommunicator.getInstance().removeSSEObserver(this.observer, topicFilter);
        }
    }

    /**
     * Filter certain received events.
     *
     * @param jsonObject the json object received
     * @return true if it should be filtered, else false
     */
    protected abstract boolean filter(final JsonObject jsonObject);

    /**
     * Convert the received json object into a different representation.
     *
     * @param jsonObject the json object received
     * @return another representation
     */
    protected abstract DTO convert(final JsonObject jsonObject);
}
