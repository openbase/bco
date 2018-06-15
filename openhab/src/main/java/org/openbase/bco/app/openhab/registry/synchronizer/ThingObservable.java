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
import org.eclipse.smarthome.core.thing.events.ThingAddedEvent;
import org.eclipse.smarthome.core.thing.events.ThingRemovedEvent;
import org.eclipse.smarthome.core.thing.events.ThingUpdatedEvent;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.ObservableDataProviderAdapter;

public class ThingObservable extends ObservableDataProviderAdapter<JsonObject> {

    private final Observer<JsonObject> observer;
    private int observerCount;

    public ThingObservable() {
        super(new ObservableImpl<>(), JsonObject.class);

        observerCount = 0;
        this.observer = (observable, jsonObject) -> {
            final String eventType = jsonObject.get("type").getAsString();

            if (eventType.equals(ThingUpdatedEvent.TYPE) || eventType.equals(ThingAddedEvent.TYPE) || eventType.equals(ThingRemovedEvent.TYPE)) {
                getObservable().notifyObservers(jsonObject);
            }
        };
    }

    @Override
    public void addDataObserver(Observer<JsonObject> observer) {
        if (observerCount == 0) {
            OpenHABRestCommunicator.getInstance().addSSEObserver(this.observer);
        }

        observerCount++;
        super.addDataObserver(observer);
    }

    @Override
    public void removeDataObserver(Observer<JsonObject> observer) {
        super.removeDataObserver(observer);

        observerCount--;
        if (observerCount == 0) {
            OpenHABRestCommunicator.getInstance().removeSSEObserver(this.observer);
        }
    }

    @Override
    public boolean isDataAvailable() {
        // this is a workaround to make the Synchronizer always trigger its initial sync
        return true;
    }
}
