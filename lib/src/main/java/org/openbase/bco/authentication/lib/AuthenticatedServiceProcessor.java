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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Future;
import javax.crypto.BadPaddingException;

import org.omg.CORBA.DynAnyPackage.Invalid;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * Helper class which should be used to implement an authenticated service.
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticatedServiceProcessor {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatedServiceProcessor.class);

    /**
     * Method used by the server which performs an authenticated action which needs write permissions.
     *
     * @param <RECEIVE>             The type of value that the server receives to perform its action,
     * @param <RETURN>              The type of value that the server responds with.
     * @param authenticatedValue    The authenticatedValue which is send with the request.
     * @param authorizationGroupMap Map of authorization groups to verify if this action can be performed.
     * @param locationMap           Map of locations to verify if this action can be performed.
     * @param internalClass         Class of type RECEIVE needed to decrypt the received type.
     * @param executable            Interface defining the cation that the server performs.
     * @param configRetrieval       Interface defining which unitConfig should be used to verify the execution of the action.
     * @return An AuthenticatedValue which should be send as a response.
     * @throws CouldNotPerformException If one step can not be done, e.g. ticket invalid or encryption failed.
     * @throws InterruptedException     It interrupted while checking permissions.
     */
    public static <RECEIVE extends GeneratedMessage, RETURN extends GeneratedMessage> AuthenticatedValue authenticatedAction(final AuthenticatedValue authenticatedValue, final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> authorizationGroupMap, final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locationMap, final Class<RECEIVE> internalClass, final InternalProcessable<RECEIVE, RETURN> executable, final ConfigRetrieval<RECEIVE> configRetrieval) throws CouldNotPerformException, InterruptedException {
        try {
            // start to build the reponse
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
                try {
                    // evaluate the users ticket
                    AuthenticatedServerManager.TicketEvaluationWrapper ticketEvaluationWrapper = AuthenticatedServerManager.getInstance().evaluateClientServerTicket(authenticatedValue.getTicketAuthenticatorWrapper());

                    // decrypt the send type from the AuthenticatedValue
                    RECEIVE decrypted = EncryptionHelper.decryptSymmetric(authenticatedValue.getValue(), ticketEvaluationWrapper.getSessionKey(), internalClass);
                    // retrieve the unit config which is used to check for permissions
                    UnitConfig unitConfig = configRetrieval.retrieve(decrypted);

                    // check for write permissions
                    if (!AuthorizationHelper.canWrite(unitConfig, ticketEvaluationWrapper.getUserId(), authorizationGroupMap, locationMap)) {
                        throw new PermissionDeniedException("User[" + ticketEvaluationWrapper.getUserId() + "] has not rights to register a unitConfig");
                    }

                    // execute the action of the server
                    RETURN result = executable.process(decrypted);
                    // encrypt the result and add it to the response
                    response.setValue(EncryptionHelper.encryptSymmetric(result, ticketEvaluationWrapper.getSessionKey()));
                    // add updated ticket to reponse
                    response.setTicketAuthenticatorWrapper(ticketEvaluationWrapper.getTicketAuthenticatorWrapper());
                } catch (IOException | BadPaddingException ex) {
                    throw new CouldNotPerformException("Encryption/Decryption of internal value has failed", ex);
                }
            } else {
                // ticket no available so request without login
                try {
                    // when not logged in the received value is not encrypted but just send as a byte string
                    // so get the received message by calling parseFrom which is supported by every message
                    Method parseFrom = internalClass.getMethod("parseFrom", ByteString.class);
                    RECEIVE message = (RECEIVE) parseFrom.invoke(null, authenticatedValue.getValue());

                    // retrieve the unit config which is used to check for permissions 
                    UnitConfig unitConfig = configRetrieval.retrieve(message);

// todo enable again after fixing openbase/bco.authentication#61
//                    if (unitConfig.getType() == UnitType.UNKNOWN) {
//                        throw new InvalidStateException("Unit type of received unit config is unknown! Reject request because message seems to be broken.");
//                    }

                    try {
                        if (JPService.getProperty(JPAuthentication.class).getValue()) {
                            // check for write permissions for other
                            if (!AuthorizationHelper.canWrite(unitConfig, null, null, locationMap)) {
                                throw new PermissionDeniedException("Other has not rights to perform this action");
                            }
                        }
                    } catch (JPNotAvailableException ex) {
                        throw new CouldNotPerformException("Could not check JPEnableAuthentication property", ex);
                    }

                    // execute the action of the server
                    RETURN result = executable.process(message);
                    // add result as a byte string to the response
                    response.setValue(result.toByteString());
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
    public static <SEND extends GeneratedMessage, RESPONSE extends GeneratedMessage> Future<RESPONSE> requestAuthenticatedAction(final SEND message, final Class<RESPONSE> responseClass, final SessionManager sessionManager, final InternalRequestable internalRequestable) throws CouldNotPerformException {
        if (sessionManager.isLoggedIn()) {
            // check if login is still valid
            sessionManager.isAuthenticated();
            // someone is logged in with the session manager
            try {
                try {
                    // initialize a ticket for the request
                    TicketAuthenticatorWrapper ticketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(sessionManager.getSessionKey(), sessionManager.getTicketAuthenticatorWrapper());
                    AuthenticatedValue.Builder authenticatedValue = AuthenticatedValue.newBuilder();
                    // add the ticket to the authenticated value which is send
                    authenticatedValue.setTicketAuthenticatorWrapper(ticketAuthenticatorWrapper);

                    // encrypt the message which is send with the session key
                    try {
                        authenticatedValue.setValue(EncryptionHelper.encryptSymmetric(message, sessionManager.getSessionKey()));
                    } catch (IOException ex) {
                        throw new CouldNotPerformException("Could not encrypt userConfig", ex);
                    }
                    // perform the internal request
                    Future<AuthenticatedValue> future = internalRequestable.request(authenticatedValue.build());
                    // wrap the response in an authenticated value future
                    return new AuthenticatedValueFuture<>(future, responseClass, ticketAuthenticatorWrapper, sessionManager);
                } catch (IOException | BadPaddingException ex) {
                    throw new CouldNotPerformException("Could not initialize service server request", ex);
                }
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not request authenticated Action!", ex);
            }
        } else {
            // no one is logged in so do not encrypt but convert message to a byte string
            AuthenticatedValue.Builder authenticateValue = AuthenticatedValue.newBuilder();
            authenticateValue.setValue(message.toByteString());
            // perform the internal request
            Future<AuthenticatedValue> future = internalRequestable.request(authenticateValue.build());
            // wrap the response in an authenticated value future
            return new AuthenticatedValueFuture<>(future, responseClass, null, sessionManager);
        }
    }

    public interface InternalProcessable<RECEIVE extends GeneratedMessage, RETURN extends GeneratedMessage> {

        /**
         * The process an authenticated value executes for a request.
         *
         * @param message the message which is received by the request.
         * @return A message which is the result of the process.
         * @throws CouldNotPerformException If the process cannot be executed.
         */
        public RETURN process(final RECEIVE message) throws CouldNotPerformException;
    }

    public interface InternalRequestable {

        /**
         * Interface wrapping an authenticated request performed by a remote.
         *
         * @param authenticatedValue The authenticated value which is send with this request.
         * @return A future containing the authenticated value which is the response from the server.
         * @throws CouldNotPerformException If the request cannot be done.
         */
        public Future<AuthenticatedValue> request(AuthenticatedValue authenticatedValue) throws CouldNotPerformException;
    }

    public interface ConfigRetrieval<M extends GeneratedMessage> {

        /**
         * Provides a UnitConfig, using the decrypted value from the AuthenticatedValue.
         *
         * @param message Decrypted object that might lead to the UnitConfig.
         * @return UnitConfig that allows to decide whether the user has the permission to perform an action.
         * @throws CouldNotPerformException
         */
        public UnitConfig retrieve(final M message) throws CouldNotPerformException;
    }
}
