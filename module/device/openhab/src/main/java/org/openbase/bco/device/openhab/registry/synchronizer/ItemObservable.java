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
import org.openhab.core.items.events.ItemAddedEvent;
import org.openhab.core.items.events.ItemRemovedEvent;
import org.openhab.core.items.events.ItemUpdatedEvent;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ItemObservable extends AbstractSSEObservable<JsonObject> {

    private static final String ITEM_TOPIC_FILTER = "openhab/items/(.+)";

    public ItemObservable() {
        super(ITEM_TOPIC_FILTER, JsonObject.class);
    }

    @Override
    public boolean isDataAvailable() {
        // this is a workaround used to trigger an initial update
        return true;
    }

    @Override
    protected boolean filter(JsonObject jsonObject) {
        final String eventType = jsonObject.get("type").getAsString();
        return !(eventType.equals(ItemAddedEvent.TYPE) || eventType.equals(ItemRemovedEvent.TYPE) || eventType.equals(ItemUpdatedEvent.TYPE));
    }

    @Override
    protected JsonObject convert(JsonObject jsonObject) {
        return jsonObject;
    }
}
