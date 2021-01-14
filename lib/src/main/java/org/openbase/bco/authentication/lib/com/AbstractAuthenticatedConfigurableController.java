package org.openbase.bco.authentication.lib.com;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2021 openbase.org
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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.AuthenticatedServerManager;
import org.openbase.bco.authentication.lib.AuthenticationBaseData;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.iface.AuthenticatedRequestable;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.communication.controller.AbstractConfigurableController;
import org.openbase.jul.communication.controller.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;

import java.io.Serializable;

public abstract class AbstractAuthenticatedConfigurableController<M extends AbstractMessage & Serializable, MB extends M.Builder<MB>, CONFIG extends Message> extends AbstractConfigurableController<M, MB, CONFIG> implements AuthenticatedRequestable {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TicketAuthenticatorWrapper.getDefaultInstance()));
    }

    public AbstractAuthenticatedConfigurableController(MB builder) throws InstantiationException {
        super(builder);
    }

    /**
     * {@inheritDoc}
     *
     * @param server {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(AuthenticatedRequestable.class, this, server);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     */
    @Override
    public M requestStatus() throws CouldNotPerformException {
        logger.debug("requestStatus of " + this);
        try {
            return updateDataToPublish(cloneDataBuilder());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            if (ex.getCause().getCause() instanceof InvalidStateException) {
                // this can happen if the registries already shutdown
                return null;
            }
            throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not request status update.", ex), logger, LogLevel.ERROR);
        }
    }

    @Override
    public AuthenticatedValue requestDataAuthenticated(final TicketAuthenticatorWrapper ticket) throws CouldNotPerformException {
        logger.debug("requestStatusAuthenticated of " + this);
        // evaluate the ticket
        final AuthenticationBaseData authenticationBaseData = AuthenticatedServerManager.getInstance().verifyClientServerTicket(ticket);

        // filter data for user
        M newData = filterDataForUser(cloneDataBuilder(), authenticationBaseData.getUserClientPair());

        // build response
        AuthenticatedValue.Builder response = AuthenticatedValue.newBuilder();
        response.setTicketAuthenticatorWrapper(authenticationBaseData.getTicketAuthenticatorWrapper());
        response.setValue(EncryptionHelper.encryptSymmetric(newData, authenticationBaseData.getSessionKey()));

        return response.build();
    }

    @Override
    protected M updateDataToPublish(MB dataBuilder) throws CouldNotPerformException {
        try {
            if (JPService.getProperty(JPAuthentication.class).getValue()) {
                try {
                    return filterDataForUser(dataBuilder, UserClientPair.getDefaultInstance());
                } catch (CouldNotPerformException ex) {
                    throw new CouldNotPerformException("Could not filter data builder for rights", ex);
                }
            } else {
                return (M) dataBuilder.build();
            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not update data to publish", ex);
        }
    }

    protected abstract M filterDataForUser(final MB dataBuilder, final UserClientPair userClientPair) throws CouldNotPerformException;
}
