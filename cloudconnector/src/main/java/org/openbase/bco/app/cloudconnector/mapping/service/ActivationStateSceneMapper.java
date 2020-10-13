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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState.State;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ActivationStateSceneMapper extends AbstractServiceStateTraitMapper<ActivationState> {

    public static final String DEACTIVATE_PARAM_KEY = "deactivate";
    public static final String REVERSIBLE_KEY = "sceneReversible";

    public ActivationStateSceneMapper() {
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
                    + ActivationState.class.getSimpleName() + "]. Attribute[" + DEACTIVATE_PARAM_KEY + "] is not boolean", ex);
        }
    }

    @Override
    public void map(ActivationState activationState, JsonObject jsonObject) throws CouldNotPerformException {
        switch (activationState.getValue()) {
            case INACTIVE:
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

    @Override
    public void addAttributes(UnitConfig unitConfig, JsonObject jsonObject) {
//        switch (unitConfig.getUnitType()) {
//            case SCENE:
//                jsonObject.addProperty(REVERSIBLE_KEY, false);
//                break;
//            default:
        jsonObject.addProperty(REVERSIBLE_KEY, true);
//        }
    }
}
