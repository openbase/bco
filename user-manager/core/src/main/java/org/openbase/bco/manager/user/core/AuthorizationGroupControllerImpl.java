package org.openbase.bco.manager.user.core;

/*-
 * #%L
 * BCO Manager User Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.dal.lib.layer.unit.AbstractBaseUnitController;
import org.openbase.bco.manager.user.lib.AuthorizationGroupController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.UserPresenceStateType.UserPresenceState;
import rst.domotic.unit.authorizationgroup.AuthorizationGroupDataType.AuthorizationGroupData;
import rst.domotic.unit.authorizationgroup.AuthorizationGroupDataType.AuthorizationGroupData.Builder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class AuthorizationGroupControllerImpl extends AbstractBaseUnitController<AuthorizationGroupData, AuthorizationGroupData.Builder> implements AuthorizationGroupController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthorizationGroupData.getDefaultInstance()));
    }

    public AuthorizationGroupControllerImpl() throws InstantiationException {
        super(AuthorizationGroupControllerImpl.class, AuthorizationGroupData.newBuilder());
    }

    @Override
    public UserPresenceState getUserPresenceState() throws NotAvailableException {
        try {
            return getData().getUserPresenceState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("user presence state", ex);
        }
    }

    @Override
    public Future<ActionFuture> setUserPresenceState(UserPresenceState userPresenceState) throws CouldNotPerformException {
        try (ClosableDataBuilder<Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setUserPresenceState(userPresenceState);
        } catch (CouldNotPerformException | NullPointerException ex) {
            throw new CouldNotPerformException("Could not set user presence state to [" + userPresenceState + "] for " + this + "!", ex);
        }
        return CompletableFuture.completedFuture(null);
    }
}
