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
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;

public class PowerStateOnOffMapper extends AbstractServiceStateTraitMapper<PowerState> {

    public static final String ON_PARAM_KEY = "on";

    public PowerStateOnOffMapper() {
        super(ServiceType.POWER_STATE_SERVICE);
    }

    @Override
    protected PowerState map(final JsonObject jsonObject) throws CouldNotPerformException {
        if (!jsonObject.has(ON_PARAM_KEY)) {
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to ["
                    + PowerState.class.getSimpleName() + "]. Attribute[" + ON_PARAM_KEY + "] is missing");
        }

        try {
            final boolean state = jsonObject.get(ON_PARAM_KEY).getAsBoolean();
            if (state) {
                return PowerState.newBuilder().setValue(State.ON).build();
            } else {
                return PowerState.newBuilder().setValue(State.OFF).build();
            }
        } catch (ClassCastException | IllegalStateException ex) {
            // thrown if it is not a boolean
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to [" +
                    PowerState.class.getSimpleName() + "]. Attribute[" + ON_PARAM_KEY + "] is not boolean");
        }
    }

    @Override
    public void map(final PowerState powerState, final JsonObject jsonObject) throws CouldNotTransformException {
        switch (powerState.getValue()) {
            case ON:
                jsonObject.addProperty(ON_PARAM_KEY, true);
                break;
            case OFF:
                jsonObject.addProperty(ON_PARAM_KEY, false);
                break;
            case UNKNOWN:
            default:
                throw new CouldNotTransformException("Could not map [" + powerState.getClass().getSimpleName()
                        + ", " + powerState.getValue().name() + "] to jsonObject");
        }
    }
}
