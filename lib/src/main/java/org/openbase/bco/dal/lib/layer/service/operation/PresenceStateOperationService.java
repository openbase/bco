package org.openbase.bco.dal.lib.layer.service.operation;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.dal.lib.layer.service.provider.PresenceStateProviderService;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.state.PresenceStateType.PresenceState;

import java.util.concurrent.Future;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface PresenceStateOperationService extends OperationService, PresenceStateProviderService {

    default Future<ActionDescription> setPresenceState(final PresenceState.State presence) throws CouldNotPerformException {
        return setPresenceState(PresenceState.newBuilder().setValue(presence).build());
    }

    @RPCMethod(legacy = true)
    Future<ActionDescription> setPresenceState(final PresenceState PresenceState) throws CouldNotPerformException;

}
