package org.openbase.bco.app.cloudconnector.mapping.lib;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class Setting extends Named {

    private static final String SETTING_NAME_KEY = "setting_name";
    private static final String SETTING_VALUES_KEY = "setting_values";
    private static final String SETTING_SYNONYM_KEY = "setting_synonym";

    public Setting(final String name) {
        super(name);
    }

    public Setting(final String name, final String... germanSynonyms) {
        super(name, germanSynonyms);
    }

    @Override
    public JsonObject toJson() throws CouldNotPerformException {
        return super.toJson(SETTING_NAME_KEY, SETTING_VALUES_KEY, SETTING_SYNONYM_KEY);
    }
}
