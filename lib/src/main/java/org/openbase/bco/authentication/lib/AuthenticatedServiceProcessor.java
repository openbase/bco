package org.openbase.bco.authentication.lib;

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

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import org.openbase.bco.authentication.lib.AuthorizationHelper.Type;
import org.openbase.bco.authentication.lib.future.AuthenticatedSynchronizationFuture;
import org.openbase.bco.authentication.lib.future.AuthenticationFuture;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.PermissionDeniedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rsb.com.TransactionIdProvider;
import org.openbase.jul.pattern.provider.DataProvider;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import javax.crypto.BadPaddingException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Helper class which should be used to implement an authenticated service.
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticatedServiceProcessor {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatedServiceProcessor.class);

    /**
     * Method used by the server which performs an authenticated action.
     *
     * @param <RECEIVE>             The type of value that the server receives to perform its action,
     * @param <RETURN>              The type of value that the server responds with.
     * @param authenticatedValue    The authenticatedValue which is send with the request.
     * @param internalClass         Class of type RECEIVE needed to decrypt the received type.
     * @param transactionIdProvider The object providing the transaction id to be set in the authenticated value.
     * @param executable            Interface defining the cation that the server performs.
     * @return An AuthenticatedValue which should be send as a response.
     * @throws CouldNotPerformException If one step can not be done, e.g. ticket invalid or encryption failed.
     * @throws InterruptedException     It interrupted while checking permissions.
     */
    public static <RECEIVE extends Serializable, RETURN extends Serializable> AuthenticatedValue authenticatedAction(
            final AuthenticatedValue authenticatedValue,
            final Class<RECEIVE> internalClass,
            final TransactionIdProvider transactionIdProvider,
            final InternalIdentifiedProcessable<RECEIVE, RETURN> executable) throws CouldNotPerformException, InterruptedException {
        try {
            // start to build the response
            AuthenticatedValue.Builder response = AuthenticatedValue.newBuilder();
            if (authenticatedValue.hasTicketAuthenticatorWrapper()) {
                try {
                    if (!JPService.getProperty(JPAuthentication.class).getValue()) {
                        throw new CouldNotPerformException("Cannot execute authenticated action because authentication is disabled");
                    }
                } catch (JPNotAvailableException ex) {
                    throw new CouldNotPerformException("Could not check JPEnableAuthentication property", ex);
                }
                // ticket authenticator is available so a logged in user has requested this action
                    // evaluate the users ticket
                    AuthenticatedServerManager.TicketEvaluationWrapper ticketEvaluationWrapper = AuthenticatedServerManager.getInstance().evaluateClientServerTicket(authenticatedValue.getTicketAuthenticatorWrapper());

                    // decrypt the send type from the AuthenticatedValue
                    RECEIVE decrypted = EncryptionHelper.decryptSymmetric(authenticatedValue.getValue(), ticketEvaluationWrapper.getSessionKey(), internalClass);

                    // execute the action of the server
                    RETURN result = executable.process(decrypted, ticketEvaluationWrapper);
                    // set transaction id in response
                    response.setTransactionId(transactionIdProvider.getTransactionId());

                    // encrypt the result and add it to the response
                    response.setValue(EncryptionHelper.encryptSymmetric(result, ticketEvaluationWrapper.getSessionKey()));
                    // add updated ticket to response
                    response.setTicketAuthenticatorWrapper(ticketEvaluationWrapper.getTicketAuthenticatorWrapper());
            } else {
                // ticket no available so request without login
                try {
                    RECEIVE message = null;

                    if (authenticatedValue.hasValue() && !authenticatedValue.getValue().isEmpty()) {
                        if (!GeneratedMessage.class.isAssignableFrom(internalClass)) {
                            throw new CouldNotPerformException("Authenticated value has a value but the method implemented by the server did not expect one");
                        }
                        // when not logged in the received value is not encrypted but just send as a byte string
                        // so get the received message by calling parseFrom which is supported by every message
                        Method parseFrom = internalClass.getMethod("parseFrom", ByteString.class);
                        message = (RECEIVE) parseFrom.invoke(null, authenticatedValue.getValue());
                    }

                    // execute the action of the server
                    RETURN result = executable.process(message, null);
                    // set transaction id in response
                    response.setTransactionId(transactionIdProvider.getTransactionId());
                    if (result != null) {
                        if (!(result instanceof GeneratedMessage)) {
                            throw new CouldNotPerformException("Result[" + result + "] of authenticated action is not a message or not null and therefore not supported");
                        }

                        // add result as a byte string to the response
                        response.setValue(((GeneratedMessage) result).toByteString());
                    }
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw new CouldNotPerformException("Could not invoke parseFrom method on [" + internalClass.getSimpleName() + "]", ex);
                }
            }
            // return the response
            return response.build();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not execute authenticated action", ex);
        }
    }

    /**
     * Method used by the server which performs an authenticated action.
     *
     * @param <RECEIVE>             The type of value that the server receives to perform its action,
     * @param <RETURN>              The type of value that the server responds with.
     * @param authenticatedValue    The authenticatedValue which is send with the request.
     * @param authorizationGroupMap Map of authorization groups to verify if this action can be performed.
     * @param locationMap           Map of locations to verify if this action can be performed.
     * @param internalClass         Class of type RECEIVE needed to decrypt the received type.
     * @param transactionIdProvider The object providing the transaction id to be set in the authenticated value.
     * @param executable            Interface defining the cation that the server performs.
     * @param configRetrieval       Interface defining which unitConfig should be used to verify the execution of the action.
     * @param authorizationType     The type of authorization which are performed (read, write or access).
     * @return An AuthenticatedValue which should be send as a response.
     * @throws CouldNotPerformException If one step can not be done, e.g. ticket invalid or encryption failed.
     * @throws InterruptedException     It interrupted while checking permissions.
     */
    public static <RECEIVE extends Serializable, RETURN extends Serializable> AuthenticatedValue authenticatedAction(
            final AuthenticatedValue authenticatedValue,
            final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> authorizationGroupMap,
            final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locationMap,
            final Class<RECEIVE> internalClass,
            final TransactionIdProvider transactionIdProvider,
            final InternalProcessable<RECEIVE, RETURN> executable,
            final ConfigRetrieval<RECEIVE> configRetrieval,
            final AuthorizationHelper.Type authorizationType) throws CouldNotPerformException, InterruptedException {
        return AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, internalClass, transactionIdProvider, (InternalIdentifiedProcessable<RECEIVE, Serializable>) (message, ticketEvaluationWrapper) -> {
            try {
                if (JPService.getProperty(JPAuthentication.class).getValue()) {
                    UnitConfig unitConfig = configRetrieval.retrieve(message);
                    if (unitConfig.getUnitType() == UnitType.UNKNOWN) {
                        throw new InvalidStateException("Unit type of received unit config is unknown! Reject request because message seems to be broken.");
                    }

                    String userId = ticketEvaluationWrapper == null ? null : ticketEvaluationWrapper.getUserId();
                    // check for write permissions
                    if (!AuthorizationHelper.canDo(unitConfig, userId, authorizationGroupMap, locationMap, authorizationType)) {
                        throw new PermissionDeniedException("User[" + userId + "] has no rights to perform this action");
                    }
                }
            } catch (JPNotAvailableException ex) {
                throw new CouldNotPerformException("Could not check JPEnableAuthentication property", ex);
            }

            return executable.process(message);
        });
    }

    /**
     * Method used by the server which performs an authenticated action which needs write permissions.
     *
     * @param <RECEIVE>             The type of value that the server receives to perform its action,
     * @param <RETURN>              The type of value that the server responds with.
     * @param authenticatedValue    The authenticatedValue which is send with the request.
     * @param authorizationGroupMap Map of authorization groups to verify if this action can be performed.
     * @param locationMap           Map of locations to verify if this action can be performed.
     * @param internalClass         Class of type RECEIVE needed to decrypt the received type.
     * @param transactionIdProvider The object providing the transaction id to be set in the authenticated value.
     * @param executable            Interface defining the cation that the server performs.
     * @param configRetrieval       Interface defining which unitConfig should be used to verify the execution of the action.
     * @return An AuthenticatedValue which should be send as a response.
     * @throws CouldNotPerformException If one step can not be done, e.g. ticket invalid or encryption failed.
     * @throws InterruptedException     It interrupted while checking permissions.
     */
    public static <RECEIVE extends Serializable, RETURN extends Serializable> AuthenticatedValue authenticatedAction(
            final AuthenticatedValue authenticatedValue,
            final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> authorizationGroupMap,
            final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locationMap,
            final Class<RECEIVE> internalClass,
            final TransactionIdProvider transactionIdProvider,
            final InternalProcessable<RECEIVE, RETURN> executable,
            final ConfigRetrieval<RECEIVE> configRetrieval) throws CouldNotPerformException, InterruptedException {
        return authenticatedAction(authenticatedValue, authorizationGroupMap, locationMap, internalClass, transactionIdProvider, executable, configRetrieval, Type.WRITE);
    }

    /**
     * Method used by the remote to request an authenticated action from a server.
     *
     * @param <SEND>              The type which is send to server for this request.
     * @param <RESPONSE>          The type with which the server should respond.
     * @param <REMOTE>            A combination of a data provider and a transaction id provider. Usually a remote.
     * @param message             The message which is encrypted and send to the server.
     * @param responseClass       Class of type RESPONSE to resolve internal types.
     * @param sessionManager      The session manager from which the ticket is used if a user it logged in.
     * @param remote              The remote providing a transaction id and a data provider which is updated and triggers transaction id verification.
     * @param internalRequestable Interface for the internal authenticated request which is called.
     * @return A future containing the response.
     * @throws CouldNotPerformException If a user is logged and a ticket for the request cannot be initialized or encryption of the send message fails.
     */
    public static <SEND extends Serializable, RESPONSE, REMOTE extends DataProvider<?> & TransactionIdProvider> Future<RESPONSE> requestAuthenticatedAction(
            final SEND message,
            final Class<RESPONSE> responseClass,
            final SessionManager sessionManager,
            final REMOTE remote,
            final InternalRequestable internalRequestable) throws CouldNotPerformException {
        return new AuthenticatedSynchronizationFuture<>(requestAuthenticatedActionWithoutTransactionSynchronization(message, responseClass, sessionManager, internalRequestable), remote);
    }

    /**
     * Method used by the remote to request an authenticated action from a server.
     *
     * @param <SEND>              The type which is send to server for this request.
     * @param <RESPONSE>          The type with which the server should respond.
     * @param message             The message which is encrypted and send to the server.
     * @param responseClass       Class of type RESPONSE to resolve internal types.
     * @param sessionManager      The session manager from which the ticket is used if a user it logged in.
     * @param internalRequestable Interface for the internal authenticated request which is called.
     * @return A future containing the response.
     * @throws CouldNotPerformException If a user is logged and a ticket for the request cannot be initialized or encryption of the send message fails.
     */
    public static <SEND extends Serializable, RESPONSE> AuthenticationFuture<RESPONSE> requestAuthenticatedActionWithoutTransactionSynchronization(
            final SEND message,
            final Class<RESPONSE> responseClass,
            final SessionManager sessionManager,
            final InternalRequestable internalRequestable) throws CouldNotPerformException {
        if (sessionManager.isLoggedIn()) {
            // someone is logged in with the session manager
            try {
                // initialize a ticket for the request
                TicketAuthenticatorWrapper ticketAuthenticatorWrapper = sessionManager.initializeServiceServerRequest();
                AuthenticatedValue.Builder authenticatedValue = AuthenticatedValue.newBuilder();
                // add the ticket to the authenticated value which is send
                authenticatedValue.setTicketAuthenticatorWrapper(ticketAuthenticatorWrapper);

                // encrypt the message which is send with the session key
                authenticatedValue.setValue(EncryptionHelper.encryptSymmetric(message, sessionManager.getSessionKey()));
                // perform the internal request
                Future<AuthenticatedValue> future = internalRequestable.request(authenticatedValue.build());
                // wrap the response in an authenticated synchronization future
                return new AuthenticationFuture<>(future, responseClass, ticketAuthenticatorWrapper, sessionManager);
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not request authenticated Action!", ex);
            }
        } else {
            // no one is logged in so do not encrypt but convert message to a byte string, only if there is a parameter
            AuthenticatedValue.Builder authenticateValue = AuthenticatedValue.newBuilder();
            if (message != null) {
                if (!(message instanceof GeneratedMessage)) {
                    throw new CouldNotPerformException("Could not convert [" + message + "] to byte string");
                }

                authenticateValue.setValue(((GeneratedMessage) message).toByteString());
            }
            // perform the internal request
            Future<AuthenticatedValue> future = internalRequestable.request(authenticateValue.build());
            // wrap the response in an authenticated synchronization future
            return new AuthenticationFuture<>(future, responseClass, null, sessionManager);
        }
    }

    public interface InternalProcessable<RECEIVE, RETURN> {

        /**
         * Process which is executed for the call of an authenticated method.
         * When this interface is implemented the authorization is also already done.
         *
         * @param message the message which is received by the request.
         * @return A message which is the result of the process.
         * @throws CouldNotPerformException If the process cannot be executed.
         */
        RETURN process(final RECEIVE message) throws CouldNotPerformException;
    }

    public interface InternalIdentifiedProcessable<RECEIVE, RETURN> {

        /**
         * Process which is executed for the call of an authenticated method.
         * When this is called the authorization of the user has already been done.
         *
         * @param message                 the parameter with which the process is called
         * @param ticketEvaluationWrapper ticket evaluation wrapper for which is already authenticated
         * @return the result of this process which is send back to the user
         * @throws CouldNotPerformException if the process cannot be executed
         */
        RETURN process(final RECEIVE message, final AuthenticatedServerManager.TicketEvaluationWrapper ticketEvaluationWrapper) throws CouldNotPerformException;
    }

    public interface InternalRequestable {

        /**
         * Interface wrapping an authenticated request performed by a remote.
         *
         * @param authenticatedValue The authenticated value which is send with this request.
         * @return A future containing the authenticated value which is the response from the server.
         * @throws CouldNotPerformException If the request cannot be done.
         */
        Future<AuthenticatedValue> request(AuthenticatedValue authenticatedValue) throws CouldNotPerformException;
    }

    public interface ConfigRetrieval<RECEIVE> {

        /**
         * Provides a UnitConfig, using the decrypted value from the AuthenticatedValue.
         *
         * @param receive Decrypted object that might lead to the UnitConfig.
         * @return UnitConfig that allows to decide whether the user has the permission to perform an action.
         * @throws CouldNotPerformException if the unit config could not be retrieved for the decrypted object.
         */
        UnitConfig retrieve(final RECEIVE receive) throws CouldNotPerformException;
    }
}
