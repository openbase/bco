package org.openbase.bco.dal.lib.layer.service;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.google.protobuf.Message;
import org.junit.Test;
import org.openbase.bco.dal.lib.layer.service.provider.BrightnessStateProviderService;
import org.openbase.bco.dal.lib.state.States;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.ColorStateType;
import org.openbase.type.domotic.state.LocalPositionStateType.LocalPositionState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServicesTest {

    /**
     * Test if the generic super state conversion as implemented in Services works properly.
     *
     * @throws Exception if something fails.
     */
    @Test
    public void testSuperStateConversion() throws Exception {
        final BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(.5d).build();
        final Message expectedPowerState = BrightnessStateProviderService.toPowerState(brightnessState);
        final Message actualPowerState = Services.convertToSuperState(ServiceType.BRIGHTNESS_STATE_SERVICE, brightnessState, ServiceType.POWER_STATE_SERVICE);
        assertEquals("Conversion from brightness state to power state did not go as expected", expectedPowerState, actualPowerState);
    }

    /**
     * Test if the equal service state method works as expected.
     *
     * @throws Exception if something fails.
     */
    @Test
    public void testEqualServiceStates() throws Exception {
        final String position1 = "Home";
        final String position2 = "Living Room";
        final String position3 = "Couch";

        final ActionDescription mockActionDescription = ActionDescription.newBuilder().setActionId("MockUp").build();
        final LocalPositionState localPositionState1 = LocalPositionState.newBuilder().addLocationId(position3).addLocationId(position2).addLocationId(position1).setTimestamp(TimestampProcessor.getCurrentTimestamp()).build();
        final LocalPositionState localPositionState2 = LocalPositionState.newBuilder().addLocationId(position3).addLocationId(position2).addLocationId(position1).setResponsibleAction(mockActionDescription).build();
        final LocalPositionState localPositionState3 = LocalPositionState.newBuilder().addLocationId(position3).addLocationId(position2).addLocationId(position1).build();
        final LocalPositionState localPositionState4 = LocalPositionState.newBuilder().addLocationId(position3).addLocationId(position1).build();
        final List<LocalPositionState> localPositionStateList = new ArrayList<>();
        localPositionStateList.add(localPositionState1);
        localPositionStateList.add(localPositionState2);
        localPositionStateList.add(localPositionState3);
        localPositionStateList.add(localPositionState4);
        final PowerState powerState = PowerState.newBuilder().setValue(State.ON).build();

        for (int i = 0; i < localPositionStateList.size(); i++) {
            for (int j = 1; j < localPositionStateList.size(); j++) {
                assertEquals("Comparision between position states " + i + " and " + j + " yields unexpected result", (i == j || (i != 3 && j != 3)), Services.equalServiceStates(localPositionStateList.get(i), localPositionStateList.get(j)));
            }
            assertFalse("PowerState should never match a local position state", Services.equalServiceStates(localPositionStateList.get(i), powerState));
        }
    }

    @Test
    public void testEqualColorStateServices() throws Exception {
        double brightness = 0.5;
        BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(brightness).build();
        BrightnessState brightnessStateInMargin = BrightnessState.newBuilder().setBrightness(brightness + 0.9 * Services.DOUBLE_MARGIN).build();
        BrightnessState brightnessStateOutsideMargin = BrightnessState.newBuilder().setBrightness(brightness + 2 * Services.DOUBLE_MARGIN).build();

        assertTrue("Brightness states are not considered equal even though the value is within margin", Services.equalServiceStates(brightnessState, brightnessStateInMargin));
        assertFalse("Brightness states are considered equal even though the value is outside margin", Services.equalServiceStates(brightnessState, brightnessStateOutsideMargin));
    }
}
