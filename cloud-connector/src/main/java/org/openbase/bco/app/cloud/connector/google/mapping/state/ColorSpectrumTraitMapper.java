package org.openbase.bco.app.cloud.connector.google.mapping.state;

/*-
 * #%L
 * BCO Cloud Connector
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

import com.google.gson.JsonObject;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.rst.transform.HSBColorToRGBColorTransformer;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.vision.RGBColorType.RGBColor;

import java.awt.*;

public class ColorSpectrumTraitMapper implements TraitMapper<ColorState> {

    public static final String COLOR_MODEL_ATTRIBUTE_KEY = "colorModel";
    public static final String COLOR_MODEL_RGB = "rgb";
    public static final String COLOR_MODEL_HSB = "hsv";

    public static final String COLOR_KEY = "color";
    public static final String COLOR_NAME_KEY = "name";
    public static final String COLOR_SPECTRUM_KEY = "spectrumRGB";

    @Override
    public ColorState map(JsonObject jsonObject) throws CouldNotPerformException {
//        if (!jsonObject.has(ColorSpectrumTraitMapper.COLOR_KEY)) {
//            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to [" + ColorState.class.getSimpleName() + "]. Attribute[" + ColorSpectrumTraitMapper.COLOR_KEY + "] is missing");
//        }
//
//        //TODO: name for color
//        try {
//            final JsonObject color = jsonObject.get(ColorSpectrumTraitMapper.COLOR_KEY).getAsJsonObject();
//
//            if (!color.has(COLOR_SPECTRUM_KEY)) {
//                throw new CouldNotPerformException("Could not map from jsonObject[" + color.toString() + "] to [" + ColorState.class.getSimpleName() + "]. Attribute[" + COLOR_SPECTRUM_KEY + "] is missing");
//            }
//
//            try {
//                Color javaColor = new Color(color.get(COLOR_SPECTRUM_KEY).getAsInt());
//                final int temperature = ;
////                final RGBColor rgbColor = colorTemperatureToRGB(temperature);
////                final ColorState.Builder colorState = ColorState.newBuilder();
////                colorState.getColorBuilder().setType(Type.RGB).setRgbColor(rgbColor);
////                return colorState.build();
//            } catch (ClassCastException | IllegalStateException ex) {
//                // thrown if it is not an int
//                throw new CouldNotPerformException("Could not map from jsonObject[" + color.toString() + "] to [" + ColorState.class.getSimpleName() + "]. Attribute[" + TEMPERATURE_KEY + "] is not an int");
//            }
//        } catch (ClassCastException | IllegalStateException ex) {
//            // thrown if it is not a json object
//            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to [" + ColorState.class.getSimpleName() + "]. Attribute[" + ColorSpectrumTraitMapper.COLOR_KEY + "] is not an object");
//        }
        throw new CouldNotPerformException("Not yet implemented");
    }

    @Override
    public void map(ColorState colorState, JsonObject jsonObject) throws CouldNotPerformException {
        throw new CouldNotPerformException("Not yet implemented");
    }

    @Override
    public void addAttributes(UnitConfig unitConfig, JsonObject jsonObject) {
        //TODO: hsv should also be possible, but how does state send then look?
        jsonObject.addProperty(COLOR_MODEL_ATTRIBUTE_KEY, COLOR_MODEL_RGB);
    }

    @Override
    public ServiceType getServiceType() {
        return ServiceType.COLOR_STATE_SERVICE;
    }
}
