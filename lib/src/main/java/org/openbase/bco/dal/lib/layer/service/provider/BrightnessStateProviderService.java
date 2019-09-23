package org.openbase.bco.dal.lib.layer.service.provider;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState.Builder;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.PowerStateType.PowerState.State;
import org.slf4j.LoggerFactory;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface BrightnessStateProviderService extends ProviderService {

    double BRIGHTNESS_MARGIN = 0.1;

    @RPCMethod(legacy = true)
    default BrightnessState getBrightnessState() throws NotAvailableException {
        return (BrightnessState) getServiceProvider().getServiceState(BRIGHTNESS_STATE_SERVICE);
    }

    static BrightnessState verifyBrightnessState(final BrightnessState brightnessState) throws VerificationFailedException {
        final Builder builder = brightnessState.toBuilder();
        switch (builder.getBrightnessDataUnit()) {
            case PERCENT:
                builder.setBrightness(ProviderService.oldValueNormalization(builder.getBrightness(), 100));
                OperationService.verifyValueRange("brightness", builder.getBrightness(), 0, 1);
                break;
            case UNKNOWN:
                throw new VerificationFailedException("BrightnessState data unit unknown!");
            default:
                break;
        }
        return builder.build();
    }

    static PowerState toPowerState(final BrightnessState brightnessState) {
        if (brightnessState.getBrightness() == 0) {
            return PowerState.newBuilder().setValue(State.OFF).build();
        } else {
            return PowerState.newBuilder().setValue(State.ON).build();
        }
    }

    static Boolean isCompatible(final BrightnessState brightnessState, final ColorState colorState) {
        return ColorStateProviderService.isCompatible(colorState, brightnessState);
    }

    static Boolean isCompatible(final BrightnessState brightnessState, final PowerState powerState) {
        switch (powerState.getValue()) {
            case ON:
                return brightnessState.getBrightness() > 0;
            case OFF:
                return brightnessState.getBrightness() == 0;
            case UNKNOWN:
            default:
                return false;
        }
    }
}
