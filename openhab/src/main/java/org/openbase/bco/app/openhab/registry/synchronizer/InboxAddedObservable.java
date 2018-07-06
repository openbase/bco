package org.openbase.bco.app.openhab.registry.synchronizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.smarthome.config.discovery.dto.DiscoveryResultDTO;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.ObservableDataProviderAdapter;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class InboxAddedObservable extends ObservableDataProviderAdapter<DiscoveryResultDTO> {

    private static final String PAYLOAD_KEY = "payload";
    private static final String INBOX_ADD_TOPIC_FILTER = "smarthome/inbox/(.+)/added";

    private final Observer<JsonObject> observer;
    private int observerCount;
    private final Gson gson;
    private final JsonParser jsonParser;

    public InboxAddedObservable() {
        super(new ObservableImpl<>(), DiscoveryResultDTO.class);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.jsonParser = new JsonParser();

        observerCount = 0;
        this.observer = (observable, jsonObject) -> {
            JsonObject payload = jsonParser.parse(jsonObject.get("payload").getAsString()).getAsJsonObject();
            final DiscoveryResultDTO addedThing = gson.fromJson(payload, DiscoveryResultDTO.class);
            getObservable().notifyObservers(addedThing);
        };
    }

    @Override
    public void addDataObserver(Observer<DiscoveryResultDTO> observer) {
        if (observerCount == 0) {
            OpenHABRestCommunicator.getInstance().addSSEObserver(this.observer, INBOX_ADD_TOPIC_FILTER);
        }

        observerCount++;
        super.addDataObserver(observer);
    }

    @Override
    public void removeDataObserver(Observer<DiscoveryResultDTO> observer) {
        super.removeDataObserver(observer);

        observerCount--;
        if (observerCount == 0) {
            OpenHABRestCommunicator.getInstance().removeSSEObserver(this.observer, INBOX_ADD_TOPIC_FILTER);
        }
    }
}
