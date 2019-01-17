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
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;

import static org.junit.Assert.assertEquals;

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
        final BrightnessState brightnessState = BrightnessState.newBuilder().setBrightness(50).build();
        final Message expectedPowerState = BrightnessStateProviderService.toPowerState(brightnessState);
        final Message actualPowerState = Services.convertToSuperState(ServiceType.BRIGHTNESS_STATE_SERVICE, brightnessState, ServiceType.POWER_STATE_SERVICE);
        assertEquals("Conversion from brightness state to power state did not go as expected", expectedPowerState, actualPowerState);
    }
}
