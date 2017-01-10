package org.openbase.bco.registry.unit.core.dbconvert;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.io.File;
import java.util.Map;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.EnablingStateType.EnablingState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AgentConfig_0_To_1_DBConverter extends AbstractDBVersionConverter {

    public AgentConfig_0_To_1_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject agentConfig, final Map<File, JsonObject> dbSnapshot) {
        // remove activation state and use it to set up the enabling state
        if (agentConfig.has("activation_state")) {
            JsonObject activationState = agentConfig.getAsJsonObject("activation_state");
            agentConfig.remove("activation_state");
            JsonObject enablingState = new JsonObject();
            if (activationState.has("value")) {
                EnablingState.State enablingValue = EnablingState.State.ENABLED;
                ActivationState.State activationValue = ActivationState.State.valueOf(activationState.getAsJsonPrimitive("value").getAsString());
                switch (activationValue) {
                    case ACTIVE:
                        enablingValue = EnablingState.State.ENABLED;
                        break;
                    case DEACTIVE:
                        enablingValue = EnablingState.State.DISABLED;
                        break;
                    case UNKNOWN:
                        enablingValue = EnablingState.State.UNKNOWN;
                }
                enablingState.addProperty("value", enablingValue.toString());
            }
            agentConfig.add("enabling_state", enablingState);
        }

        return agentConfig;
    }
}
