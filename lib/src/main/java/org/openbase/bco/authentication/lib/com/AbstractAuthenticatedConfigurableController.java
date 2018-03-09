package org.openbase.bco.authentication.lib.com;

import com.google.protobuf.GeneratedMessage;
import org.openbase.bco.authentication.lib.AuthenticatedServerManager;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.iface.AuthenticatedRequestable;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableController;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import rsb.config.ParticipantConfig;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.rsb.ScopeType.Scope;

import java.io.IOException;

public abstract class AbstractAuthenticatedConfigurableController<M extends GeneratedMessage, MB extends M.Builder<MB>, CONFIG extends GeneratedMessage> extends AbstractConfigurableController<M, MB, CONFIG> implements AuthenticatedRequestable<M> {

    public AbstractAuthenticatedConfigurableController(MB builder) throws InstantiationException {
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
        logger.debug("requestStatusAuthenticated of " + this);
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
