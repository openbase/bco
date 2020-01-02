package org.openbase.bco.app.cloudconnector.mapping.service;

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
import org.openbase.bco.app.cloudconnector.mapping.lib.Command;
import org.openbase.bco.app.cloudconnector.mapping.unit.TemperatureControllerDataMapper;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState.DataUnit;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TemperatureStateTemperatureSettingMapper extends AbstractServiceStateTraitMapper<TemperatureState> {

    public static final String TEMPERATURE_SETPOINT_KEY = "thermostatTemperatureSetpoint";
    public static final String TEMPERATURE_SETTING_MODE_KEY = "thermostatMode";

    public static final String MODE_OFF = "off";
    public static final String MODE_COOLING = "cooling";
    public static final String MODE_HEATING = "heating";

    public static final String THERMOSTAT_MODES_KEY = "availableThermostatModes";
    public static final String TEMPERATURE_UNIT_KEY = "thermostatTemperatureUnit";

    public TemperatureStateTemperatureSettingMapper() {
        super(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE);
    }

    @Override
    public TemperatureState map(JsonObject jsonObject, Command command) throws CouldNotPerformException {
        switch (command) {
            case THERMOSTAT_TEMPERATURE_SETPOINT:
                if (!jsonObject.has(TEMPERATURE_SETPOINT_KEY)) {
                    throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to ["
                            + TemperatureState.class.getSimpleName() + "]. Attribute[" + TEMPERATURE_SETPOINT_KEY + "] is missing");
                }

                try {
                    final float temperature = jsonObject.get(TEMPERATURE_SETPOINT_KEY).getAsFloat();
                    return TemperatureState.newBuilder().setTemperature(temperature).build();
                } catch (ClassCastException | IllegalStateException ex) {
                    // thrown if it is not a boolean
                    throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to ["
                            + TemperatureState.class.getSimpleName() + "]. Attribute[" + TEMPERATURE_SETPOINT_KEY + "] is not a float");
                }
            case THERMOSTAT_SET_MODE:
                if (!jsonObject.has(TEMPERATURE_SETTING_MODE_KEY)) {
                    throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to ["
                            + TemperatureState.class.getSimpleName() + "]. Attribute[" + TEMPERATURE_SETTING_MODE_KEY + "] is missing");
                }

                try {
                    //TODO: this is just a hack and should be changed when we introduce a temperature mode
                    final String mode = jsonObject.get(TEMPERATURE_SETTING_MODE_KEY).getAsString();
                    switch (mode) {
                        case MODE_OFF:
                            return TemperatureState.newBuilder().setTemperature(0.0).setTemperatureDataUnit(DataUnit.CELSIUS).build();
                        case MODE_COOLING:
                            return TemperatureState.newBuilder().setTemperature(18.0).setTemperatureDataUnit(DataUnit.CELSIUS).build();
                        case MODE_HEATING:
                            return TemperatureState.newBuilder().setTemperature(23.0).setTemperatureDataUnit(DataUnit.CELSIUS).build();
                        default:
                            throw new CouldNotPerformException("Mode[" + mode + "] is not supported by " + getClass().getSimpleName());
                    }
                } catch (ClassCastException | IllegalStateException ex) {
                    // thrown if it is not a boolean
                    throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to ["
                            + TemperatureState.class.getSimpleName() + "]. Attribute[" + TEMPERATURE_SETTING_MODE_KEY + "] is not a string");
                }
            default:
                throw new CouldNotPerformException("Command[" + command.name() + "] not yet supported by " + getClass().getSimpleName());
        }
    }

    @Override
    protected TemperatureState map(JsonObject jsonObject) throws CouldNotPerformException {
        throw new CouldNotPerformException("Use method with command parameter");
    }

    @Override
    public void map(TemperatureState temperatureState, JsonObject jsonObject) throws CouldNotPerformException {
        throw new CouldNotPerformException("Operation not supported, should be handled by " +
                TemperatureControllerDataMapper.class.getSimpleName());
    }

    @Override
    public void addAttributes(UnitConfig unitConfig, JsonObject jsonObject) {
        // currently modes not yet supported
        jsonObject.addProperty(THERMOSTAT_MODES_KEY, MODE_OFF + "," + MODE_COOLING + "," + MODE_HEATING);
        // C for Celsius, F for Fahrenheit
        jsonObject.addProperty(TEMPERATURE_UNIT_KEY, "C");
    }
}
