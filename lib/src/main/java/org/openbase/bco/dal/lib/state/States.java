package org.openbase.bco.dal.lib.state;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.bco.dal.lib.layer.unit.RollerShutter;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState;
import org.openbase.type.domotic.state.MotionStateType.MotionState;
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
     * Brightness State Prototypes
     */
    public static class Brightness {

        public static final BrightnessState MIN = BrightnessState.newBuilder().setBrightness(0d).build();
        public static final BrightnessState MAX = BrightnessState.newBuilder().setBrightness(1d).build();
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

    /**
     * Illuminance State Prototypes
     */
    public static class Illuminance {
        public static final IlluminanceState DARK = IlluminanceState.newBuilder().setValue(IlluminanceState.State.DARK).build();
        public static final IlluminanceState DUSKY = IlluminanceState.newBuilder().setValue(IlluminanceState.State.DUSKY).build();
        public static final IlluminanceState SHADY = IlluminanceState.newBuilder().setValue(IlluminanceState.State.SHADY).build();
        public static final IlluminanceState SUNNY = IlluminanceState.newBuilder().setValue(IlluminanceState.State.SUNNY).build();
    }

    /**
     * Motion State Prototypes
     */
    public static class Motion {
        public static final MotionState MOTION = MotionState.newBuilder().setValue(MotionState.State.MOTION).build();
        public static final MotionState NO_MOTION = MotionState.newBuilder().setValue(MotionState.State.NO_MOTION).build();
    }
}
