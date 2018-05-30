package org.openbase.bco.app.cloud.connector.google.mapping.state;

import com.google.gson.JsonObject;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PowerStateType.PowerState.State;

public class OnOffTraitMapper implements TraitMapper<PowerState> {

    public static final String ON_OFF_TRAIT_KEY = "on";

    @Override
    public PowerState map(final JsonObject jsonObject) throws CouldNotPerformException {
        if (!jsonObject.has(ON_OFF_TRAIT_KEY)) {
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to [" + PowerState.class.getSimpleName() + "]. Attribute[" + ON_OFF_TRAIT_KEY + "] is missing");
        }

        try {
            final boolean state = jsonObject.get(ON_OFF_TRAIT_KEY).getAsBoolean();
            if (state) {
                return PowerState.newBuilder().setValue(State.ON).build();
            } else {
                return PowerState.newBuilder().setValue(State.OFF).build();
            }
        } catch (ClassCastException | IllegalStateException ex) {
            // thrown if it is not a boolean
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to [" + PowerState.class.getSimpleName() + "]. Attribute[" + ON_OFF_TRAIT_KEY + "] is not boolean");
        }
    }

    @Override
    public void map(final PowerState powerState, final JsonObject jsonObject) throws CouldNotTransformException {
        switch (powerState.getValue()) {
            case ON:
                jsonObject.addProperty(ON_OFF_TRAIT_KEY, true);
                break;
            case OFF:
                jsonObject.addProperty(ON_OFF_TRAIT_KEY, false);
                break;
            case UNKNOWN:
            default:
                throw new CouldNotTransformException("Could not map [" + powerState.getClass().getSimpleName() + ", " + powerState.getValue().name() + "] to jsonObject");
        }
    }

    @Override
    public ServiceType getServiceType() {
        return ServiceType.POWER_STATE_SERVICE;
    }
}