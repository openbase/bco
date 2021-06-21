package org.openbase.bco.dal.lib.layer.service.operation;

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

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.provider.GlobalPositionStateProviderService;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.GlobalPositionStateType.GlobalPositionState;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface GlobalPositionStateOperationService extends OperationService, GlobalPositionStateProviderService {

    @RPCMethod(legacy = true)
    default Future<ActionDescription> setGlobalPositionState(final GlobalPositionState globalPositionState) {
        try {
            return getServiceProvider().applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(globalPositionState, ServiceType.GLOBAL_POSITION_STATE_SERVICE));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    default Future<ActionDescription> setGlobalPositionState(final GlobalPositionState globalPositionState, final ActionParameter actionParameter) {
        try {
            return getServiceProvider().applyAction(actionParameter.toBuilder().setServiceStateDescription(ActionDescriptionProcessor.generateServiceStateDescription(globalPositionState, ServiceType.GLOBAL_POSITION_STATE_SERVICE)));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }
}
