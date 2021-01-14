package org.openbase.bco.dal.lib.layer.service.operation;

/*
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

import java.util.concurrent.Future;

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.collection.ColorStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.ColorStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.vision.ColorType.Color;
import org.openbase.type.vision.ColorType.Color.Type;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.openbase.type.vision.RGBColorType.RGBColor;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface ColorStateOperationService extends OperationService, ColorStateProviderService {

    @RPCMethod(legacy = true)
    default Future<ActionDescription> setColorState(final ColorState colorState) {
        try {
            return getServiceProvider().applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(colorState, ServiceType.COLOR_STATE_SERVICE));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    default Future<ActionDescription> setColorState(final ColorState colorState, final ActionParameter actionParameter) {
        try {
            return getServiceProvider().applyAction(actionParameter.toBuilder().setServiceStateDescription(ActionDescriptionProcessor.generateServiceStateDescription(colorState, ServiceType.COLOR_STATE_SERVICE)));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    default Future<ActionDescription> setColor(final HSBColor color, final ActionParameter actionParameter) {
        return setColor(Color.newBuilder().setType(Color.Type.HSB).setHsbColor(color).build(), actionParameter);
    }

    default Future<ActionDescription> setColor(final Color color, final ActionParameter actionParameter) {
        return setColorState(ColorState.newBuilder().setColor(color).build(), actionParameter);
    }

    default Future<ActionDescription> setColor(final RGBColor color, final ActionParameter actionParameter) {
        return setColor(Color.newBuilder().setType(Color.Type.RGB).setRgbColor(color).build(), actionParameter);
    }

    @RPCMethod(legacy = true)
    default Future<ActionDescription> setNeutralWhite() {
        try {
            return setColor(getNeutralWhiteColor());
        } catch (NotAvailableException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not set neutral white!", ex));
        }
    }

    default Future<ActionDescription> setNeutralWhite(final ActionParameter actionParameter) {
        try {
            return setColor(getNeutralWhiteColor(), actionParameter);
        } catch (NotAvailableException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not set neutral white!", ex));
        }
    }

    @RPCMethod(legacy = true)
    default Future<ActionDescription> setColor(final HSBColor color) {
        return setColor(Color.newBuilder().setType(Color.Type.HSB).setHsbColor(color).build());
    }

    default Future<ActionDescription> setColor(final Color color) {
        return setColorState(ColorState.newBuilder().setColor(color).build());
    }

    default Future<ActionDescription> setColor(final RGBColor color) {
        return setColor(Color.newBuilder().setType(Color.Type.RGB).setRgbColor(color).build());
    }
}
