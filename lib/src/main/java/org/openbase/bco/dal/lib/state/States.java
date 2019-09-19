package org.openbase.bco.dal.lib.state;

import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.openbase.type.vision.ColorType;
import org.openbase.type.vision.ColorType.Color.Type;
import org.openbase.type.vision.HSBColorType.HSBColor;

public class States {

    /**
     * Power State Prototypes
     */
    public static class Power {

        public static final PowerState ON = PowerState.newBuilder().setValue(State.ON).build();
        public static final PowerState OFF = PowerState.newBuilder().setValue(State.OFF).build();
    }

    /**
     * Color State Prototypes
     */
    public static class Color {

        public static final ColorType.Color BLACK_VALUE = ColorType.Color.newBuilder().setType(Type.HSB).setHsbColor(HSBColor.newBuilder().setHue(0.0).setSaturation(1.0).setBrightness(0.0).build()).build();
        public static final ColorType.Color WHITE_VALUE = ColorType.Color.newBuilder().setType(Type.HSB).setHsbColor(HSBColor.newBuilder().setHue(0.0).setSaturation(0.0).setBrightness(1.0).build()).build();
        public static final ColorType.Color RED_VALUE = ColorType.Color.newBuilder().setType(Type.HSB).setHsbColor(HSBColor.newBuilder().setHue(0.0).setSaturation(1.0).setBrightness(1.0).build()).build();
        public static final ColorType.Color GREEN_VALUE = ColorType.Color.newBuilder().setType(Type.HSB).setHsbColor(HSBColor.newBuilder().setHue(120.0).setSaturation(1.0).setBrightness(1.0).build()).build();
        public static final ColorType.Color BLUE_VALUE = ColorType.Color.newBuilder().setType(Type.HSB).setHsbColor(HSBColor.newBuilder().setHue(240.0).setSaturation(1.0).setBrightness(1.0).build()).build();
        public static final ColorType.Color YELLOW_VALUE = ColorType.Color.newBuilder().setType(Type.HSB).setHsbColor(HSBColor.newBuilder().setHue(60.0).setSaturation(1.0).setBrightness(1.0).build()).build();

        public static final ColorState BLACK = ColorState.newBuilder().setColor(BLACK_VALUE).build();
        public static final ColorState WHITE = ColorState.newBuilder().setColor(WHITE_VALUE).build();
        public static final ColorState RED = ColorState.newBuilder().setColor(RED_VALUE).build();
        public static final ColorState GREEN = ColorState.newBuilder().setColor(GREEN_VALUE).build();
        public static final ColorState BLUE = ColorState.newBuilder().setColor(BLUE_VALUE).build();
        public static final ColorState YELLOW = ColorState.newBuilder().setColor(YELLOW_VALUE).build();
    }

}
