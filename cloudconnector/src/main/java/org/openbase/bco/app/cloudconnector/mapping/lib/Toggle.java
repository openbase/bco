package org.openbase.bco.app.cloudconnector.mapping.lib;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import com.google.gson.JsonArray;
import org.openbase.jul.exception.CouldNotPerformException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class Toggle {

    public static final String AVAILABLE_TOGGLES_KEY = "availableToggles";

    private final Named on, off;

    public Toggle(final String nameOn, final String nameOff) {
        this.on = new Named(nameOn);
        this.off = new Named(nameOff);
    }

    public Toggle(final Named on, final Named off) {
        this.on = on;
        this.off = off;
    }

    public Named getOff() {
        return off;
    }

    public Named getOn() {
        return on;
    }

    public JsonArray toJson() throws CouldNotPerformException {
        final JsonArray jsonArray = new JsonArray();
        jsonArray.add(on.toJson());
        jsonArray.add(off.toJson());
        return jsonArray;
    }
}
