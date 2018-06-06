package org.openbase.bco.app.cloud.connector.mapping.service;

import com.google.gson.JsonObject;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.ActivationStateType.ActivationState.State;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneTraitMapper extends AbstractTraitMapper<ActivationState> {

    public static final String DEACTIVATE_PARAM_KEY = "deactivate";

    public SceneTraitMapper() {
        super(ServiceType.ACTIVATION_STATE_SERVICE);
    }

    @Override
    protected ActivationState map(JsonObject jsonObject) throws CouldNotPerformException {
        if (!jsonObject.has(DEACTIVATE_PARAM_KEY)) {
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to ["
                    + ActivationState.class.getSimpleName() + "]. Attribute[" + DEACTIVATE_PARAM_KEY + "] is missing");
        }

        try {
            final boolean deactivate = jsonObject.get(DEACTIVATE_PARAM_KEY).getAsBoolean();
            if (deactivate) {
                return ActivationState.newBuilder().setValue(State.INACTIVE).build();
            } else {
                return ActivationState.newBuilder().setValue(State.ACTIVE).build();
            }
        } catch (ClassCastException | IllegalStateException ex) {
            // thrown if it is not a boolean
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to ["
                    + ActivationState.class.getSimpleName() + "]. Attribute[" + DEACTIVATE_PARAM_KEY + "] is not boolean");
        }
    }

    @Override
    public void map(ActivationState activationState, JsonObject jsonObject) throws CouldNotPerformException {
        switch (activationState.getValue()) {
            case DEACTIVE:
                jsonObject.addProperty(DEACTIVATE_PARAM_KEY, true);
                break;
            case ACTIVE:
                jsonObject.addProperty(DEACTIVATE_PARAM_KEY, false);
                break;
            case UNKNOWN:
            default:
                throw new CouldNotTransformException("Could not map [" + activationState.getClass().getSimpleName()
                        + ", " + activationState.getValue().name() + "] to jsonObject");
        }
    }
}
