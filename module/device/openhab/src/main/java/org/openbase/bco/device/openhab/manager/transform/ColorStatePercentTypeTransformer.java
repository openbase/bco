package org.openbase.bco.device.openhab.manager.transform;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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

import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.extension.type.transform.HSBColorToRGBColorTransformer;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.vision.ColorType.Color.Type;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.openhab.core.library.types.PercentType;

import java.math.BigDecimal;

public class ColorStatePercentTypeTransformer implements ServiceStateCommandTransformer<ColorState, PercentType> {

    @Override
    public ColorState transform(final PercentType percentType) throws CouldNotTransformException {
        try {
            ColorState.Builder colorState = ColorState.newBuilder();
            colorState.getColorBuilder().setType(Type.HSB);
            HSBColor.Builder hsbColor = colorState.getColorBuilder().getHsbColorBuilder();
            hsbColor.setBrightness(percentType.doubleValue() / 100d);
            return colorState.build();
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + PercentType.class.getName() + " to " + ColorState.class.getName() + "!", ex);
        }
    }

    @Override
    public PercentType transform(final ColorState colorState) throws CouldNotTransformException {
        try {
            HSBColor hsbColor;
            if (colorState.getColor().getType() == Type.RGB) {
                hsbColor = HSBColorToRGBColorTransformer.transform(colorState.getColor().getRgbColor());
            } else {
                hsbColor = colorState.getColor().getHsbColor();
            }
            return new PercentType(BigDecimal.valueOf(hsbColor.getBrightness() * 100d));
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + ColorState.class.getName() + " to " + PercentType.class.getName() + "!", ex);
        }
    }
}
