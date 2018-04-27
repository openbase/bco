package org.openbase.bco.app.openhab.manager.transform;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.openbase.jul.exception.CouldNotTransformException;
import rst.domotic.state.ColorStateType.ColorState;
import rst.vision.ColorType.Color.Type;
import rst.vision.HSBColorType.HSBColor;

public class OpenHABColorStateTransformer {

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
            DecimalType hue = new DecimalType(colorState.getColor().getHsbColor().getHue());
            PercentType saturation = new PercentType((int) colorState.getColor().getHsbColor().getSaturation());
            PercentType brightness = new PercentType((int) colorState.getColor().getHsbColor().getBrightness());
            return new HSBType(hue, saturation, brightness);
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + ColorState.class.getName() + " to " + HSBType.class.getName() + "!", ex);
        }
    }
}
