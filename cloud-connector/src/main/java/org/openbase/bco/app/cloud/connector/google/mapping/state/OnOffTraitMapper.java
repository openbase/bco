package org.openbase.bco.app.cloud.connector.google.mapping.state;

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
