package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2019 openbase.org
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
import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.type.iface.TransactionIdProvider;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;

/**
 * Helper class which should be used to implement an authenticated service.
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticatedServiceProcessor {

    /**
     * Method used by the server which performs an authenticated action.
     *
     * @param <RECEIVE>             The type of value that the server receives to perform its action,
     * @param <RETURN>              The type of value that the server responds with.
     * @param authenticatedValue    The authenticatedValue which is send with the request.
     * @param internalClass         Class of type RECEIVE needed to decrypt the received type.
     * @param transactionIdProvider The object providing the transaction id to be set in the authenticated value.
     * @param executable            Interface defining the action that the server performs. This executable should
     *                              also perform authorization if needed.
     *
     * @return An AuthenticatedValue which should be send as a response.
     *
     * @throws CouldNotPerformException If one step can not be done, e.g. ticket invalid or encryption failed.
     */
    public static <RECEIVE extends Serializable, RETURN extends Serializable> AuthenticatedValue authenticatedAction(
            final AuthenticatedValue authenticatedValue,
            final Class<RECEIVE> internalClass,
            final TransactionIdProvider transactionIdProvider,
            final InternalIdentifiedProcessable<RECEIVE, RETURN> executable) throws CouldNotPerformException {
        return authenticatedAction(authenticatedValue, internalClass, executable).toBuilder().setTransactionId(transactionIdProvider.getTransactionId()).build();
    }

    /**
     * Method used by the server which performs an authenticated action.
     *
     * @param <RECEIVE>          The type of value that the server receives to perform its action,
     * @param <RETURN>           The type of value that the server responds with.
     * @param authenticatedValue The authenticatedValue which is send with the request.
     * @param internalClass      Class of type RECEIVE needed to decrypt the received type.
     * @param executable         Interface defining the action that the server performs. This executable should
     *                           also perform authorization if needed.
     *
     * @return An AuthenticatedValue which should be send as a response.
     *
     * @throws CouldNotPerformException If one step can not be done, e.g. ticket invalid or encryption failed.
     */
    public static <RECEIVE extends Serializable, RETURN extends Serializable> AuthenticatedValue authenticatedAction(
            final AuthenticatedValue authenticatedValue,
            final Class<RECEIVE> internalClass,
            final InternalIdentifiedProcessable<RECEIVE, RETURN> executable) throws CouldNotPerformException {
        return authenticatedAction(authenticatedValue, internalClass, value -> AuthenticatedServerManager.getInstance().verifyClientServerTicket(value), executable);
    }

    /**
     * Method used by the server which performs an authenticated action.
     *
     * @param <RECEIVE>          The type of value that the server receives to perform its action,
     * @param <RETURN>           The type of value that the server responds with.
     * @param authenticatedValue The authenticatedValue which is send with the request.
     * @param internalClass      Class of type RECEIVE needed to decrypt the received type.
     * @param executable         Interface defining the action that the server performs. This executable should
     *                           also perform authorization if needed.
     * @param ticketValidator    Interface defining how the authenticated value is verified and authentication as well
     *                           as authorization information is extracted.
     *
     * @return An AuthenticatedValue which should be send as a response.
     *
     * @throws CouldNotPerformException If one step can not be done, e.g. ticket invalid or encryption failed.
     */
    public static <RECEIVE extends Serializable, RETURN extends Serializable> AuthenticatedValue authenticatedAction(
            final AuthenticatedValue authenticatedValue,
            final Class<RECEIVE> internalClass,
            final TicketValidator ticketValidator,
            final InternalIdentifiedProcessable<RECEIVE, RETURN> executable) throws CouldNotPerformException {
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
                // verify ticket in encrypt tokens if they were send
                final AuthenticationBaseData authenticationBaseData = ticketValidator.verifyClientServerTicket(authenticatedValue);

                RECEIVE decrypted = null;
                // decrypt the send type from the AuthenticatedValue
                if (authenticatedValue.hasValue()) {
                    decrypted = EncryptionHelper.decryptSymmetric(authenticatedValue.getValue(), authenticationBaseData.getSessionKey(), internalClass);
                }

                // execute the action of the server
                RETURN result = executable.process(decrypted, authenticationBaseData);

                if (result != null) {
                    // encrypt the result and add it to the response
                    response.setValue(EncryptionHelper.encryptSymmetric(result, authenticationBaseData.getSessionKey()));
                }
                // add updated ticket to response
                response.setTicketAuthenticatorWrapper(authenticationBaseData.getTicketAuthenticatorWrapper());
            } else {
                // ticket no available so request without login
                try {
                    RECEIVE message = null;

                    if (authenticatedValue.hasValue() && !authenticatedValue.getValue().isEmpty()) {
                        System.out.println("Internal class: " + internalClass.getName());
                        if (!Message.class.isAssignableFrom(internalClass)) {
                            throw new CouldNotPerformException("Authenticated value has a value but the method implemented by the server did not expect one!");
                        }
                        // when not logged in the received value is not encrypted but just send as a byte string
                        // so get the received message by calling parseFrom which is supported by every message
                        Method parseFrom = internalClass.getMethod("parseFrom", ByteString.class);
                        message = (RECEIVE) parseFrom.invoke(null, authenticatedValue.getValue());
                        System.out.println("Extracted message: " + message);
                    }

                    // execute the action of the server
                    RETURN result = executable.process(message, null);
                    if (result != null) {
                        if (!(result instanceof Message)) {
                            throw new CouldNotPerformException("Result[" + result + "] of authenticated action is not a message or not null and therefore not supported!");
                        }

                        // add result as a byte string to the response
                        response.setValue(((Message) result).toByteString());
                    }
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw new CouldNotPerformException("Could not invoke parseFrom method on [" + internalClass.getSimpleName() + "]", ex);
                }
            }
            // return the response
            return response.build();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not execute authenticated action!", ex);
        }
    }

    /**
     * Method used by the remote to request an authenticated action from a server.
     * <p>
     * Note: The future object is canceled if a user is logged and a ticket for the request cannot be initialized or encryption of the send message fails.
     *
     * @param <SEND>              The type which is send to server for this request.
     * @param <RESPONSE>          The type with which the server should respond.
     * @param message             The message which is encrypted and send to the server.
     * @param responseClass       Class of type RESPONSE to resolve internal types.
     * @param sessionManager      The session manager from which the ticket is used if a user it logged in.
     * @param internalRequestable Interface for the internal authenticated request which is called.
     *
     * @return A future containing the response.
     */
    public static <SEND extends Serializable, RESPONSE> Future<RESPONSE> requestAuthenticatedAction(
            final SEND message,
            final Class<RESPONSE> responseClass,
            final SessionManager sessionManager,
            final InternalRequestable internalRequestable) {
        try {
            if (sessionManager.isLoggedIn()) {
                // someone is logged in with the session manager
                try {
                    // initialize a ticket for the request
                    TicketAuthenticatorWrapper ticketAuthenticatorWrapper = sessionManager.initializeServiceServerRequest();
                    AuthenticatedValue.Builder authenticatedValue = AuthenticatedValue.newBuilder();
                    // add the ticket to the authenticated value which is send
                    authenticatedValue.setTicketAuthenticatorWrapper(ticketAuthenticatorWrapper);

                    if (message != null) {
                        // encrypt the message which is send with the session key
                        authenticatedValue.setValue(EncryptionHelper.encryptSymmetric(message, sessionManager.getSessionKey()));
                    }
                    // perform the internal request
                    Future<AuthenticatedValue> future = internalRequestable.request(authenticatedValue.build());
                    // wrap the response in an authenticated synchronization future
                    return new AuthenticatedValueFuture<>(future, responseClass, ticketAuthenticatorWrapper, sessionManager);
                } catch (CouldNotPerformException ex) {
                    throw new CouldNotPerformException("Could not request authenticated Action!", ex);
                }
            } else {
                // no one is logged in so do not encrypt but convert message to a byte string, only if there is a parameter
                AuthenticatedValue.Builder authenticateValue = AuthenticatedValue.newBuilder();
                if (message != null) {
                    if (!(message instanceof Message)) {
                        throw new CouldNotPerformException("Could not convert [" + message + "] to byte string");
                    }

                    authenticateValue.setValue(((Message) message).toByteString());
                }
                // perform the internal request
                Future<AuthenticatedValue> future = internalRequestable.request(authenticateValue.build());
                // wrap the response in an authenticated synchronization future
                return new AuthenticatedValueFuture<>(future, responseClass, null, sessionManager);
            }
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ex);
        }
    }

    public interface InternalIdentifiedProcessable<RECEIVE, RETURN> {

        /**
         * Process which is executed for the call of an authenticated method.
         * When this is called the authentication of the user has already been done but the authorization still
         * has to be performed.
         *
         * @param message                the parameter with which the process is called
         * @param authenticationBaseData data object containing information about the authentication/authorization of the request
         *
         * @return the result of this process which is send back to the user
         *
         * @throws CouldNotPerformException if the process cannot be executed
         */
        RETURN process(final RECEIVE message, final AuthenticationBaseData authenticationBaseData) throws CouldNotPerformException;
    }

    public interface InternalRequestable {

        /**
         * Interface wrapping an authenticated request performed by a remote.
         *
         * @param authenticatedValue The authenticated value which is send with this request.
         *
         * @return A future containing the authenticated value which is the response from the server.
         *
         * @throws CouldNotPerformException if the request cannot be made.
         */
        Future<AuthenticatedValue> request(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException;
    }

    public interface TicketValidator {

        /**
         * Verify the ticket from an authenticated value. This is done by verifying the internal ticket wrapper
         * and decrypting tokens and so on into and {@link AuthenticationBaseData} type.
         *
         * @return data comprising all authentication and authorization information.
         *
         * @throws if the ticket could not be validated.
         */
        AuthenticationBaseData verifyClientServerTicket(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException;
    }
}
