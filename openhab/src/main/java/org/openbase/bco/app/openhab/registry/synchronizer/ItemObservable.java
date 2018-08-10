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
import org.eclipse.smarthome.core.items.events.ItemAddedEvent;
import org.eclipse.smarthome.core.items.events.ItemRemovedEvent;
import org.eclipse.smarthome.core.items.events.ItemUpdatedEvent;
import org.openbase.jul.pattern.Observer;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ItemObservable extends AbstractDTOObservable<JsonObject> {

    private static final String ITEM_TOPIC_FILTER = "smarthome/items/(.+)";

    private final Observer<JsonObject> observer;

    public ItemObservable() {
        super(ITEM_TOPIC_FILTER, JsonObject.class);

        this.observer = (observable, jsonObject) -> {
            final String eventType = jsonObject.get("type").getAsString();

            if (eventType.equals(ItemAddedEvent.TYPE) || eventType.equals(ItemRemovedEvent.TYPE) || eventType.equals(ItemUpdatedEvent.TYPE)) {
                getObservable().notifyObservers(jsonObject);
            }
        };
    }

    @Override
    protected Observer<JsonObject> getInternalObserver() {
        return observer;
    }

    @Override
    public boolean isDataAvailable() {
        // this is a workaround used to trigger an initial update
        return true;
    }
}
