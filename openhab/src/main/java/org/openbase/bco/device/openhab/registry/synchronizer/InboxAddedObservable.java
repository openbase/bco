package org.openbase.bco.device.openhab.registry.synchronizer;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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

/**
 * Observable notifying on inbox events of openHAB.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class InboxAddedObservable extends AbstractSSEObservable<DiscoveryResultDTO> {

    private static final String PAYLOAD_KEY = "payload";
    private static final String INBOX_ADDED_TOPIC_FILTER = "smarthome/inbox/(.+)/added";

    private final Gson gson;
    private final JsonParser jsonParser;

    public InboxAddedObservable() {
        super(INBOX_ADDED_TOPIC_FILTER, DiscoveryResultDTO.class);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.jsonParser = new JsonParser();
    }

    @Override
    protected boolean filter(JsonObject jsonObject) {
        return false;
    }

    @Override
    protected DiscoveryResultDTO convert(JsonObject jsonObject) {
        final JsonObject payload = jsonParser.parse(jsonObject.get(PAYLOAD_KEY).getAsString()).getAsJsonObject();
        return gson.fromJson(payload, DiscoveryResultDTO.class);
    }
}
