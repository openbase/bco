package org.openbase.bco.app.cloud.connector.mapping.unit;

import com.google.gson.JsonObject;
import org.openbase.bco.dal.remote.unit.TemperatureControllerRemote;
import org.openbase.jul.exception.CouldNotPerformException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TemperatureControllerUnitTypeMapper implements UnitTypeMapper<TemperatureControllerRemote> {

    public static final String TEMPERATURE_SETPOINT_KEY = "thermostatTemperatureSetpoint";
    public static final String TEMPERATURE_AMBIENT_KEY = "thermostatTemperatureAmbient";

    @Override
    public void map(TemperatureControllerRemote unitRemote, JsonObject jsonObject) throws CouldNotPerformException {
        jsonObject.addProperty(TEMPERATURE_SETPOINT_KEY, unitRemote.getTargetTemperatureState().getTemperature());
        jsonObject.addProperty(TEMPERATURE_AMBIENT_KEY, unitRemote.getTemperatureState().getTemperature());
    }
}
