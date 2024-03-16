package org.openbase.bco.registry.message.remote

import org.openbase.bco.authentication.lib.SessionManager
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture
import org.openbase.type.domotic.authentication.AuthTokenType
import org.openbase.type.domotic.authentication.AuthenticatedValueType
import org.openbase.type.domotic.communication.UserMessageType
import java.io.Serializable
import java.util.concurrent.Future

fun MessageRegistryRemote.updateUserMessageAuthenticated(
    userMessage: UserMessageType.UserMessage?,
    auth: AuthTokenType.AuthToken?,
): Future<UserMessageType.UserMessage> = authRequest(userMessage, ::updateUserMessageAuthenticated, auth)

fun MessageRegistryRemote.removeUserMessageAuthenticated(
    userMessage: UserMessageType.UserMessage?,
    auth: AuthTokenType.AuthToken?,
): Future<UserMessageType.UserMessage> = authRequest(userMessage, ::removeUserMessageAuthenticated, auth)

fun MessageRegistryRemote.registerUserMessageAuthenticated(
    userMessage: UserMessageType.UserMessage?,
    auth: AuthTokenType.AuthToken?,
): Future<UserMessageType.UserMessage> = authRequest(userMessage, ::registerUserMessageAuthenticated, auth)

inline fun <reified T : Serializable> MessageRegistryRemote.authRequest(
    value: T?,
    origin: (AuthenticatedValueType.AuthenticatedValue) -> Future<AuthenticatedValueType.AuthenticatedValue>,
    auth: AuthTokenType.AuthToken?,
): Future<T> = with(SessionManager.getInstance()) {
    initializeRequest(value, auth).let { value ->
        AuthenticatedValueFuture(
            origin(value),
            T::class.java,
            value.ticketAuthenticatorWrapper,
            this
        )
    }
}
