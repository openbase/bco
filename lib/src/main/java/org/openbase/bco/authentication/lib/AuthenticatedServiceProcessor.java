package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 openbase.org
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
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.PermissionDeniedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.PermissionConfigType.PermissionConfig;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public abstract class AuthenticatedServiceProcessor {

    public static <M extends GeneratedMessage> AuthenticatedValue authenticatedAction(final AuthenticatedValue authenticatedValue, final PermissionConfig permissionConfig, final Map<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>> authorizationGroupMap, final Class<M> internalClass, final InternalProcessable<M> executable) throws CouldNotPerformException, InterruptedException {
        try {
            AuthenticatedValue.Builder response = AuthenticatedValue.newBuilder();
            if (authenticatedValue.hasTicketAuthenticatorWrapper()) {
                response.setTicketAuthenticatorWrapper(authenticatedValue.getTicketAuthenticatorWrapper());
                try {
                    ServiceServerManager.TicketEvaluationWrapper ticketEvaluationWrapper = ServiceServerManager.getInstance().evaluateClientServerTicket(response.getTicketAuthenticatorWrapper());

                    if (!AuthorizationHelper.canWrite(permissionConfig, ticketEvaluationWrapper.getId(), authorizationGroupMap)) {
                        throw new PermissionDeniedException("User[" + ticketEvaluationWrapper.getId() + "] has not rights to register a unitConfig");
                    }
                    M decrypted = EncryptionHelper.decryptSymmetric(authenticatedValue.getValue(), ticketEvaluationWrapper.getSessionKey(), internalClass);
                    M result = executable.process(decrypted);
                    response.setValue(EncryptionHelper.encryptSymmetric(result, ticketEvaluationWrapper.getSessionKey()));
                } catch (IOException | BadPaddingException ex) {
                    throw new CouldNotPerformException("Encryption/Decryption of internal value has failed", ex);
                }
            } else {
                if (!AuthorizationHelper.canWrite(permissionConfig, null, null)) {
                    throw new PermissionDeniedException("Other has not rights to register a unitConfig");
                }
                try {
                    Method parseFrom = internalClass.getMethod("parseFrom", ByteString.class);
                    M message = (M) parseFrom.invoke(null, authenticatedValue.getValue());
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
                sessionManager.initializeServiceServerRequest();
                TicketAuthenticatorWrapper ticketAuthenticatorWrapper = sessionManager.getTicketAuthenticatorWrapper();

                AuthenticatedValue.Builder authenticatedValue = AuthenticatedValue.newBuilder();
                authenticatedValue.setTicketAuthenticatorWrapper(ticketAuthenticatorWrapper);
                try {
                    authenticatedValue.setValue(EncryptionHelper.encryptSymmetric(message, sessionManager.getSessionKey()));
                } catch (IOException ex) {
                    throw new CouldNotPerformException("Could not encrypt userConfig", ex);
                }

                Future<AuthenticatedValue> future = internalRequestable.request(authenticatedValue.build());
                return new AuthenticatedValueFuture<>(future, messageClass, ticketAuthenticatorWrapper, sessionManager);
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not register unit config!", ex);
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
}
