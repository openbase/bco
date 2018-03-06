package org.openbase.bco.authentication.lib.com;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2018 openbase.org
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

import com.google.protobuf.GeneratedMessage;
import org.openbase.bco.authentication.lib.AuthenticatedServerManager;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.iface.AuthenticatedRequestable;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.com.RSBCommunicationService;
import rsb.config.ParticipantConfig;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.rsb.ScopeType.Scope;

import java.io.IOException;


public abstract class AbstractAuthenticatedCommunicationService<M extends GeneratedMessage, MB extends M.Builder<MB>> extends RSBCommunicationService<M, MB> implements AuthenticatedRequestable<M> {

    /**
     * Create a communication service.
     *
     * @param builder the initial data builder
     * @throws InstantiationException if the creation fails
     */
    public AbstractAuthenticatedCommunicationService(final MB builder) throws InstantiationException {
        super(builder);
    }

    @Override
    public void init(Scope scope, ParticipantConfig participantConfig) throws InitializationException, InterruptedException {
        super.init(scope, participantConfig);

        try {
            RPCHelper.registerInterface(AuthenticatedRequestable.class, this, server);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
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
            throw ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not request status update.", ex), logger, LogLevel.ERROR);
        }
    }

    @Override
    public AuthenticatedValue requestDataAuthenticated(final TicketAuthenticatorWrapper ticket) throws CouldNotPerformException {
        try {
            // evaluate the ticket
            AuthenticatedServerManager.TicketEvaluationWrapper ticketEvaluationWrapper = AuthenticatedServerManager.getInstance().evaluateClientServerTicket(ticket);

            // filter data for user
            M newData = filterDataForUser(cloneDataBuilder(), ticketEvaluationWrapper.getUserId());

            // build response
            AuthenticatedValue.Builder response = AuthenticatedValue.newBuilder();
            response.setTicketAuthenticatorWrapper(ticketEvaluationWrapper.getTicketAuthenticatorWrapper());
            response.setValue(EncryptionHelper.encryptSymmetric(newData, ticketEvaluationWrapper.getSessionKey()));

            return response.build();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not request data because interruption while validating ticket", ex);
        } catch (IOException ex) {
            throw new CouldNotPerformException("Could not request data authenticated because encryption or decryption with session key failed", ex);
        }
    }

    @Override
    protected M updateDataToPublish(MB dataBuilder) throws CouldNotPerformException {
        try {
            return filterDataForUser(dataBuilder, null);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not filter data builder for rights", ex);
        }
    }

    protected abstract M filterDataForUser(final MB dataBuilder, final String userId) throws CouldNotPerformException;
}
