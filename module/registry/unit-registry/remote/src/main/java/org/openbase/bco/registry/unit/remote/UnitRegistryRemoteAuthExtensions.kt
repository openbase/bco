package org.openbase.bco.registry.unit.remote

import org.openbase.bco.authentication.lib.SessionManager
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture
import org.openbase.type.domotic.authentication.AuthTokenType
import org.openbase.type.domotic.authentication.AuthenticatedValueType
import org.openbase.type.domotic.unit.UnitConfigType
import java.io.Serializable
import java.util.concurrent.Future

fun UnitRegistryRemote.updateUnitConfigAuthenticated(
    unitConfig: UnitConfigType.UnitConfig?,
    auth: AuthTokenType.AuthToken?,
): Future<UnitConfigType.UnitConfig> = authRequest(unitConfig, ::updateUnitConfigAuthenticated, auth)

fun UnitRegistryRemote.removeUnitConfigAuthenticated(
    unitConfig: UnitConfigType.UnitConfig?,
    auth: AuthTokenType.AuthToken?,
): Future<UnitConfigType.UnitConfig> = authRequest(unitConfig, ::removeUnitConfigAuthenticated, auth)

fun UnitRegistryRemote.registerUnitConfigAuthenticated(
    unitConfig: UnitConfigType.UnitConfig?,
    auth: AuthTokenType.AuthToken?,
): Future<UnitConfigType.UnitConfig> = authRequest(unitConfig, ::registerUnitConfigAuthenticated, auth)

inline fun <reified T : Serializable> UnitRegistryRemote.authRequest(
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
