package org.openbase.bco.app.openhab.manager.transform;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.extension.rst.transform.HSBColorToRGBColorTransformer;
import rst.domotic.state.ColorStateType.ColorState;
import rst.vision.ColorType.Color.Type;
import rst.vision.HSBColorType.HSBColor;

public class ColorStateTransformer {

    public static ColorState transform(final HSBType hsbType) throws CouldNotTransformException {
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

    public static HSBType transform(final ColorState colorState) throws CouldNotTransformException {
        try {
            HSBColor hsbColor;
            if(colorState.getColor().getType() == Type.RGB) {
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
