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
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState.DataUnit;

public class BrightnessStateBrightnessMapper extends AbstractServiceStateTraitMapper<BrightnessState> {

    public static final String BRIGHTNESS_TRAIT_KEY = "brightness";

    public BrightnessStateBrightnessMapper() {
        super(ServiceType.BRIGHTNESS_STATE_SERVICE);
    }

    @Override
    protected BrightnessState map(final JsonObject jsonObject) throws CouldNotPerformException {
        if (!jsonObject.has(BRIGHTNESS_TRAIT_KEY)) {
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to [" + BrightnessState.class.getSimpleName() + "]. Attribute[" + BRIGHTNESS_TRAIT_KEY + "] is missing");
        }

        try {
            final int brightness = jsonObject.get(BRIGHTNESS_TRAIT_KEY).getAsInt();
            return BrightnessState.newBuilder().setBrightnessDataUnit(DataUnit.PERCENT).setBrightness(brightness).build();
        } catch (ClassCastException | IllegalStateException ex) {
            // thrown if it is not an int
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to [" + BrightnessState.class.getSimpleName() + "]. Attribute[" + BRIGHTNESS_TRAIT_KEY + "] is not an integer");
        }
    }

    @Override
    public void map(final BrightnessState brightnessState, final JsonObject jsonObject) {
        jsonObject.addProperty(BRIGHTNESS_TRAIT_KEY, (int) brightnessState.getBrightness());
    }
}
