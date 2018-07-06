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
