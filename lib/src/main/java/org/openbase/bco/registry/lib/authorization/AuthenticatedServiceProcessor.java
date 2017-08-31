package org.openbase.bco.registry.lib.authorization;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Future;
import javax.crypto.BadPaddingException;
import org.openbase.bco.authentication.lib.AuthenticationClientHandler;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.ServiceServerManager;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.PermissionDeniedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public abstract class AuthenticatedServiceProcessor {

    public static <M extends GeneratedMessage> AuthenticatedValue authenticatedAction(final AuthenticatedValue authenticatedValue, final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> authorizationGroupMap, final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> locationMap, final Class<M> internalClass, final InternalProcessable<M> executable, final ConfigRetrieval<M> configRetrieval) throws CouldNotPerformException, InterruptedException {
        try {
            AuthenticatedValue.Builder response = AuthenticatedValue.newBuilder();
            if (authenticatedValue.hasTicketAuthenticatorWrapper()) {
                try {
                    ServiceServerManager.TicketEvaluationWrapper ticketEvaluationWrapper = ServiceServerManager.getInstance().evaluateClientServerTicket(authenticatedValue.getTicketAuthenticatorWrapper());

                    M decrypted = EncryptionHelper.decryptSymmetric(authenticatedValue.getValue(), ticketEvaluationWrapper.getSessionKey(), internalClass);
                    UnitConfig unitConfig = configRetrieval.retrieve(decrypted);

                    if (!AuthorizationHelper.canWrite(unitConfig, ticketEvaluationWrapper.getId(), authorizationGroupMap, locationMap)) {
                        throw new PermissionDeniedException("User[" + ticketEvaluationWrapper.getId() + "] has not rights to register a unitConfig");
                    }

                    M result = executable.process(decrypted);
                    response.setValue(EncryptionHelper.encryptSymmetric(result, ticketEvaluationWrapper.getSessionKey()));
                    response.setTicketAuthenticatorWrapper(ticketEvaluationWrapper.getTicketAuthenticatorWrapper());
                } catch (IOException | BadPaddingException ex) {
                    throw new CouldNotPerformException("Encryption/Decryption of internal value has failed", ex);
                }
            } else {
                try {
                    Method parseFrom = internalClass.getMethod("parseFrom", ByteString.class);
                    M message = (M) parseFrom.invoke(null, authenticatedValue.getValue());

                    UnitConfig unitConfig = configRetrieval.retrieve(message);

                    if (!AuthorizationHelper.canWrite(unitConfig, null, null, locationMap)) {
                        throw new PermissionDeniedException("Other has not rights to register a unitConfig");
                    }

                    M result = executable.process(message);
                    response.setValue(result.toByteString());
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw new CouldNotPerformException("Could not invoke parseFrom method on [" + internalClass.getSimpleName() + "]", ex);
                }
            }
            return response.build();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not execute authenticated action", ex);
        }
    }

    public static <M extends GeneratedMessage> Future<M> requestAuthenticatedAction(final M message, final Class<M> messageClass, final SessionManager sessionManager, final InternalRequestable internalRequestable) throws CouldNotPerformException {
        if (sessionManager.isLoggedIn()) {
            try {
                try {
                    TicketAuthenticatorWrapper ticketAuthenticatorWrapper = AuthenticationClientHandler.initServiceServerRequest(sessionManager.getSessionKey(), sessionManager.getTicketAuthenticatorWrapper());
                    AuthenticatedValue.Builder authenticatedValue = AuthenticatedValue.newBuilder();
                    authenticatedValue.setTicketAuthenticatorWrapper(ticketAuthenticatorWrapper);

                    try {
                        authenticatedValue.setValue(EncryptionHelper.encryptSymmetric(message, sessionManager.getSessionKey()));
                    } catch (IOException ex) {
                        throw new CouldNotPerformException("Could not encrypt userConfig", ex);
                    }
                    Future<AuthenticatedValue> future = internalRequestable.request(authenticatedValue.build());
                    return new AuthenticatedValueFuture<>(future, messageClass, ticketAuthenticatorWrapper, sessionManager);
                } catch (IOException | BadPaddingException ex) {
                    throw new CouldNotPerformException("Could not initialize service server request", ex);
                }
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not request authenticated Action!", ex);
            }
        } else {
            AuthenticatedValue.Builder authenticateValue = AuthenticatedValue.newBuilder();
            authenticateValue.setValue(message.toByteString());
            Future<AuthenticatedValue> future = internalRequestable.request(authenticateValue.build());
            return new AuthenticatedValueFuture<>(future, messageClass, null, sessionManager);
        }
    }

    public interface InternalProcessable<M extends GeneratedMessage> {

        public M process(final M message) throws CouldNotPerformException;
    }

    public interface InternalRequestable {

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
