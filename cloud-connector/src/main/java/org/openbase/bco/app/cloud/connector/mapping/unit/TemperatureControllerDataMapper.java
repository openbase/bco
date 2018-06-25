package org.openbase.bco.app.cloud.connector.mapping.unit;

/*-
 * #%L
 * BCO Cloud Connector
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
import org.openbase.bco.app.cloud.connector.mapping.service.TargetTemperatureServiceTemperatureSettingMapper;
import org.openbase.bco.dal.remote.unit.TemperatureControllerRemote;
import org.openbase.jul.exception.CouldNotPerformException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TemperatureControllerDataMapper implements UnitDataMapper<TemperatureControllerRemote> {

    public static final String TEMPERATURE_AMBIENT_KEY = "thermostatTemperatureAmbient";

    @Override
    public void map(final TemperatureControllerRemote unitRemote, final JsonObject jsonObject) throws CouldNotPerformException {
        jsonObject.addProperty(TargetTemperatureServiceTemperatureSettingMapper.TEMPERATURE_SETPOINT_KEY, unitRemote.getTargetTemperatureState().getTemperature());
        jsonObject.addProperty(TEMPERATURE_AMBIENT_KEY, unitRemote.getTemperatureState().getTemperature());
    }
}
