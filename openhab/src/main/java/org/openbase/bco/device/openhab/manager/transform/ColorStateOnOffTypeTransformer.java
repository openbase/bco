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

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.TypeNotSupportedException;
import org.openbase.jul.extension.type.transform.HSBColorToRGBColorTransformer;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.PowerStateType;
import org.openbase.type.vision.ColorType.Color.Type;
import org.openbase.type.vision.HSBColorType;

import java.math.BigDecimal;

public class ColorStateOnOffTypeTransformer implements ServiceStateCommandTransformer<ColorState, OnOffType> {

    @Override
    public ColorState transform(final OnOffType onOffType) throws CouldNotTransformException {

        try {
//            ColorState.Builder colorState = ColorState.newBuilder();
//            colorState.getColorBuilder().setType(Type.HSB);
//
//            switch (onOffType) {
//                case OFF:
//                    colorState.getColorBuilder().getHsbColorBuilder().setBrightness(0);
//                    break;
//                case ON:
//                    colorState.getColorBuilder().getHsbColorBuilder().setBrightness(1);
//                    break;
//                default:
//                    throw new CouldNotTransformException("Could not transform " + OnOffType.class.getSimpleName() + "[" + onOffType.name() + "] is unknown!");
//            }
//            return colorState.build();
            throw new TypeNotSupportedException("Transformation would generate invalid data.");
            // todo make sure on off types are only mapped on power states!
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + OnOffType.class.getName() + " to " + ColorState.class.getName() + "!", ex);
        }
    }

    @Override
    public OnOffType transform(final ColorState colorState) throws CouldNotTransformException {
        try {
            HSBColorType.HSBColor hsbColor;
            if (colorState.getColor().getType() == Type.RGB) {
                hsbColor = HSBColorToRGBColorTransformer.transform(colorState.getColor().getRgbColor());
            } else {
                hsbColor = colorState.getColor().getHsbColor();
            }

            if (hsbColor.getBrightness() > 0) {
                return OnOffType.ON;
            } else if (hsbColor.getBrightness() == 0) {
                return OnOffType.OFF;
            } else {
                throw new InvalidStateException("Brightness has an invalid value: "+hsbColor.getBrightness());
            }
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + ColorState.class.getName() + " to " + OnOffType.class.getName() + "!", ex);
        }
    }
}
