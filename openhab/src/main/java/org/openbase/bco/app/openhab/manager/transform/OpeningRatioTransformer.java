package org.openbase.bco.app.openhab.manager.transform;

import org.eclipse.smarthome.core.library.types.PercentType;
import rst.domotic.state.BlindStateType.BlindState;

public class OpeningRatioTransformer {

    public static BlindState transform(final PercentType decimalType) {
        return BlindState.newBuilder().setOpeningRatio(decimalType.doubleValue()).build();
    }

    public static PercentType transform(final BlindState blindState) {
        return new PercentType((int) blindState.getOpeningRatio());
    }
}
