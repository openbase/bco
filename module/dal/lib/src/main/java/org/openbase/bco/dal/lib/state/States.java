package org.openbase.bco.dal.lib.state;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState;
import org.openbase.type.domotic.state.BlindStateType.BlindState;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.ContactStateType.ContactState;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState;
import org.openbase.type.domotic.state.MotionStateType.MotionState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.vision.ColorType;
import org.openbase.type.vision.ColorType.Color.Type;
import org.openbase.type.vision.HSBColorType.HSBColor;

public class States {

    /**
     * Activation State Prototypes
     */
    public static class Activation {

        public static final ActivationState ACTIVE = ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build();
        public static final ActivationState INACTIVE = ActivationState.newBuilder().setValue(ActivationState.State.INACTIVE).build();
    }

    /**
     * Power State Prototypes
     */
    public static class Power {

        public static final PowerState ON = PowerState.newBuilder().setValue(PowerState.State.ON).build();
        public static final PowerState OFF = PowerState.newBuilder().setValue(PowerState.State.OFF).build();
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
        public static final ColorType.Color ORANGE_VALUE = ColorType.Color.newBuilder().setType(Type.HSB).setHsbColor(HSBColor.newBuilder().setHue(30).setSaturation(1.0).setBrightness(1.0).build()).build();
        public static final ColorType.Color PURPLE_VALUE = ColorType.Color.newBuilder().setType(Type.HSB).setHsbColor(HSBColor.newBuilder().setHue(270).setSaturation(1.0).setBrightness(1.0).build()).build();
        public static final ColorType.Color PINK_VALUE = ColorType.Color.newBuilder().setType(Type.HSB).setHsbColor(HSBColor.newBuilder().setHue(300).setSaturation(1.0).setBrightness(1.0).build()).build();

        public static final ColorState BLACK = ColorState.newBuilder().setColor(BLACK_VALUE).build();
        public static final ColorState WHITE = ColorState.newBuilder().setColor(WHITE_VALUE).build();
        public static final ColorState RED = ColorState.newBuilder().setColor(RED_VALUE).build();
        public static final ColorState GREEN = ColorState.newBuilder().setColor(GREEN_VALUE).build();
        public static final ColorState BLUE = ColorState.newBuilder().setColor(BLUE_VALUE).build();
        public static final ColorState YELLOW = ColorState.newBuilder().setColor(YELLOW_VALUE).build();
        public static final ColorState ORANGE = ColorState.newBuilder().setColor(ORANGE_VALUE).build();
        public static final ColorState PURPLE = ColorState.newBuilder().setColor(PURPLE_VALUE).build();
        public static final ColorState PINK = ColorState.newBuilder().setColor(PINK_VALUE).build();
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

    /**
     * Blind State Prototypes
     */
    public static class Blind {
        public static final BlindState UP = BlindState.newBuilder().setValue(BlindState.State.UP).build();
        public static final BlindState DOWN = BlindState.newBuilder().setValue(BlindState.State.DOWN).build();
    }

    /**
     * Contact State Prototypes
     */
    public static class Contact {
        public static final ContactState OPEN = ContactState.newBuilder().setValue(ContactState.State.OPEN).build();
        public static final ContactState CLOSED = ContactState.newBuilder().setValue(ContactState.State.CLOSED).build();
    }

    /**
     * Battery State Prototypes
     */
    public static class Battery {
        public static final BatteryState OK = BatteryState.newBuilder().setValue(BatteryState.State.OK).build();
        public static final BatteryState LOW = BatteryState.newBuilder().setValue(BatteryState.State.LOW).build();
        public static final BatteryState CRITICAL = BatteryState.newBuilder().setValue(BatteryState.State.CRITICAL).build();
        public static final BatteryState INSUFFICIENT = BatteryState.newBuilder().setValue(BatteryState.State.INSUFFICIENT).build();
    }
}
