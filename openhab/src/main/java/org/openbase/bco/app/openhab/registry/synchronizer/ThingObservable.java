package org.openbase.bco.app.openhab.registry.synchronizer;

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
