package org.openbase.bco.app.cloud.connector.mapping.service;

import com.google.gson.JsonObject;
import org.openbase.bco.app.cloud.connector.mapping.lib.Command;
import org.openbase.bco.app.cloud.connector.mapping.unit.TemperatureControllerUnitTypeMapper;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.TemperatureStateType.TemperatureState;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TemperatureSettingTraitMapper extends AbstractTraitMapper<TemperatureState> {

    public static final String TEMPERATURE_SETPOINT_KEY = "thermostatTemperatureSetpoint";

    public TemperatureSettingTraitMapper() {
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
                TemperatureControllerUnitTypeMapper.class.getSimpleName());
    }
}
