package org.openbase.bco.dal.lib.action;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription.Builder;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;

import static org.junit.jupiter.api.Assertions.*;

class ActionDescriptionProcessorTest {

    @Test
    void getUnitChainSuffixForNonReplaceableAction() {

        final Builder buttonActionDescription = ActionDescription.newBuilder();
        final Builder sceneChillActionDescription = ActionDescription.newBuilder();
        final Builder sceneMoodLightActionDescription = ActionDescription.newBuilder();
        final Builder locationActionDescription = ActionDescription.newBuilder();
        final Builder lightActionDescription = ActionDescription.newBuilder();

        buttonActionDescription.getServiceStateDescriptionBuilder().setUnitId("Button");
        sceneChillActionDescription.getServiceStateDescriptionBuilder().setUnitId("SceneChill");
        sceneMoodLightActionDescription.getServiceStateDescriptionBuilder().setUnitId("SceneMoodLight");
        locationActionDescription.getServiceStateDescriptionBuilder().setUnitId("Location");
        lightActionDescription.getServiceStateDescriptionBuilder().setUnitId("Light");

        sceneChillActionDescription.setReplaceable(false);
        sceneMoodLightActionDescription.setReplaceable(false);

        ActionDescriptionProcessor.updateActionCause(sceneChillActionDescription, buttonActionDescription);
        ActionDescriptionProcessor.updateActionCause(sceneMoodLightActionDescription, sceneChillActionDescription);
        ActionDescriptionProcessor.updateActionCause(locationActionDescription, sceneMoodLightActionDescription);
        ActionDescriptionProcessor.updateActionCause(lightActionDescription, locationActionDescription);

        assertEquals(true, buttonActionDescription.getReplaceable());
        assertEquals(false, sceneChillActionDescription.getReplaceable());
        assertEquals(false, sceneMoodLightActionDescription.getReplaceable());
        assertEquals(true, locationActionDescription.getReplaceable());
        assertEquals(true, lightActionDescription.getReplaceable());

        assertEquals("Button", ActionDescriptionProcessor.getUnitChainSuffixForNonReplaceableAction(buttonActionDescription.build()), "Chain suffix does not match!");
        assertEquals("SceneChill", ActionDescriptionProcessor.getUnitChainSuffixForNonReplaceableAction(sceneChillActionDescription.build()), "Chain suffix does not match!");
        assertEquals("SceneMoodLight", ActionDescriptionProcessor.getUnitChainSuffixForNonReplaceableAction(sceneMoodLightActionDescription.build()), "Chain suffix does not match!");
        assertEquals("SceneMoodLight_Location", ActionDescriptionProcessor.getUnitChainSuffixForNonReplaceableAction(locationActionDescription.build()), "Chain suffix does not match!");
        assertEquals("SceneMoodLight_Location_Light", ActionDescriptionProcessor.getUnitChainSuffixForNonReplaceableAction(lightActionDescription.build()), "Chain suffix does not match!");
    }
}
