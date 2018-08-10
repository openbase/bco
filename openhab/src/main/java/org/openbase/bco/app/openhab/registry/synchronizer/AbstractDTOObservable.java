package org.openbase.bco.app.openhab.registry.synchronizer;

/*-
 * #%L
 * BCO Openhab App
 * %%
 * Copyright (C) 2018 openbase.org
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
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.ObservableDataProviderAdapter;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractDTOObservable<DTO> extends ObservableDataProviderAdapter<DTO> {

    private final String topicFilter;
    private int observerCount;

    public AbstractDTOObservable(final String topicFilter, Class<DTO> clazz) {
        super(new ObservableImpl<>(), clazz);
        this.topicFilter = topicFilter;
        observerCount = 0;
    }

    @Override
    public void addDataObserver(final Observer<DTO> observer) {
        if (observerCount == 0) {
            OpenHABRestCommunicator.getInstance().addSSEObserver(getInternalObserver(), topicFilter);
        }

        observerCount++;
        super.addDataObserver(observer);
    }

    @Override
    public void removeDataObserver(Observer<DTO> observer) {
        super.removeDataObserver(observer);

        observerCount--;
        if (observerCount == 0) {
            OpenHABRestCommunicator.getInstance().removeSSEObserver(getInternalObserver(), topicFilter);
        }
    }

    protected abstract Observer<JsonObject> getInternalObserver();
}
