package org.openbase.bco.app.openhab.manager.transform;

/*-
 * #%L
 * BCO Openhab App
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

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.extension.rst.transform.HSBColorToRGBColorTransformer;
import rst.domotic.state.ColorStateType.ColorState;
import rst.vision.ColorType.Color.Type;
import rst.vision.HSBColorType.HSBColor;

public class ColorStateHSBTypeTransformer implements ServiceStateCommandTransformer<ColorState, HSBType> {

    @Override
    public ColorState transform(final HSBType hsbType) throws CouldNotTransformException {
        try {
            ColorState.Builder colorState = ColorState.newBuilder();
            colorState.getColorBuilder().setType(Type.HSB);
            HSBColor.Builder hsbColor = colorState.getColorBuilder().getHsbColorBuilder();
            hsbColor.setHue(hsbType.getHue().doubleValue());
            hsbColor.setSaturation(hsbType.getSaturation().doubleValue());
            hsbColor.setBrightness(hsbType.getBrightness().doubleValue());
            return colorState.build();
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + HSBType.class.getName() + " to " + ColorState.class.getName() + "!", ex);
        }
    }

    @Override
    public HSBType transform(final ColorState colorState) throws CouldNotTransformException {
        try {
            HSBColor hsbColor;
            if (colorState.getColor().getType() == Type.RGB) {
                hsbColor = HSBColorToRGBColorTransformer.transform(colorState.getColor().getRgbColor());
            } else {
                hsbColor = colorState.getColor().getHsbColor();
            }
            DecimalType hue = new DecimalType(hsbColor.getHue());
            PercentType saturation = new PercentType((int) hsbColor.getSaturation());
            PercentType brightness = new PercentType((int) hsbColor.getBrightness());
            return new HSBType(hue, saturation, brightness);
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + ColorState.class.getName() + " to " + HSBType.class.getName() + "!", ex);
        }
    }
}
