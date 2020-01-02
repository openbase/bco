package org.openbase.bco.device.openhab.manager.transform;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.TypeNotSupportedException;
import org.openbase.jul.extension.type.transform.HSBColorToRGBColorTransformer;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.vision.ColorType.Color.Type;
import org.openbase.type.vision.HSBColorType;

public class BrightnessStateOnOffTypeTransformer implements ServiceStateCommandTransformer<BrightnessState, OnOffType> {

    @Override
    public BrightnessState transform(final OnOffType onOffType) throws CouldNotTransformException {

        try {
            BrightnessState.Builder brightnessState = BrightnessState.newBuilder();

            switch (onOffType) {
                case OFF:
                    brightnessState.setBrightness(0);
                    break;
                case ON:
                    throw new TypeNotSupportedException("Transformation would generate invalid data.");
                    // todo make sure brightness types are only mapped on brightness states!
                    //brightnessState.setBrightness(1);
                    //break;
                default:
                    throw new CouldNotTransformException("Could not transform " + OnOffType.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
            }
            return brightnessState.build();
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + OnOffType.class.getName() + " to " + BrightnessState.class.getName() + "!", ex);
        }
    }

    @Override
    public OnOffType transform(final BrightnessState brightnessState) throws CouldNotTransformException {
        try {
            if (brightnessState.getBrightness() > 0) {
                return OnOffType.ON;
            } else if (brightnessState.getBrightness() == 0) {
                return OnOffType.OFF;
            } else {
                throw new InvalidStateException("Brightness has an invalid value: "+brightnessState.getBrightness());
            }
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + BrightnessState.class.getName() + " to " + OnOffType.class.getName() + "!", ex);
        }
    }
}
