package org.openbase.bco.registry.unit.lib.auth

import org.openbase.bco.authentication.lib.AuthPair
import org.openbase.bco.authentication.lib.AuthenticationBaseData
import org.openbase.bco.authentication.lib.AuthorizationHelper
import org.openbase.bco.registry.lib.util.UnitConfigProcessor
import org.openbase.bco.registry.template.lib.TemplateRegistry
import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote
import org.openbase.bco.registry.unit.lib.UnitRegistry
import org.openbase.jul.exception.*
import org.openbase.jul.exception.MultiException.ExceptionStack
import org.openbase.type.domotic.authentication.AuthorizationTokenType
import org.openbase.type.domotic.authentication.UserClientPairType
import org.openbase.type.domotic.communication.UserMessageType
import org.openbase.type.domotic.service.ServiceTemplateType
import org.openbase.type.domotic.unit.UnitConfigType
import org.openbase.type.domotic.unit.UnitTemplateType
import org.slf4j.LoggerFactory
import java.util.*

/*-
* #%L
* BCO Registry Unit Library
* %%
* Copyright (C) 2014 - 2021 openbase.org
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
*/ /**
 * @author [Tamino Huxohl](mailto:pleminoq@openbase.org)
 */
object AuthorizationWithTokenHelper {
    private val LOGGER = LoggerFactory.getLogger(AuthorizationWithTokenHelper::class.java)

    /**
     * Verify an authorization token. This is done in two steps.
     * By checking if all entered ids match either a UnitConfig, ServiceTemplate or UnitTemplate.
     * If the id matches a unit config it is checked if the user defined in the token has at least as many permissions
     * for the unit as the token would grant.
     *
     * @param authorizationToken the authorization token that is checked
     * @param unitRegistry       registry used to resolve authorization groups and locations to check permissions
     *
     * @throws CouldNotPerformException thrown if the token is invalid
     */
    @JvmStatic
    @Throws(CouldNotPerformException::class)
    fun verifyAuthorizationToken(
        authorizationToken: AuthorizationTokenType.AuthorizationToken,
        unitRegistry: UnitRegistry,
    ) {
        try {
            for (permissionRule in authorizationToken.permissionRuleList) {
                // make sure the unit with the given id exists
                val unitConfig: UnitConfigType.UnitConfig
                try {
                    unitConfig = unitRegistry.getUnitConfigById(permissionRule.getUnitId())

                    // make sure the unit template with the given id exists
                    if (permissionRule.hasUnitTemplateId()) {
                        CachedTemplateRegistryRemote.getRegistry()
                            .getUnitTemplateById(permissionRule.getUnitTemplateId())
                    }

                    // make sure the service template with the given id exists
                    if (permissionRule.hasServiceTemplateId()) {
                        CachedTemplateRegistryRemote.getRegistry()
                            .getServiceTemplateById(permissionRule.getServiceTemplateId())
                    }
                } catch (ex: CouldNotPerformException) {
                    throw RejectedException("Invalid unit id, service template id or unit template id", ex)
                }

                // a filter reduces permissions so it can be granted anyways
                if (permissionRule.filter) {
                    continue
                }

                // evaluate the permissions the given user has for the unit defined in the token
                val permission = AuthorizationHelper.getPermission(
                    unitConfig,
                    authorizationToken.getUserId(),
                    unitRegistry.getAuthorizationGroupMap(),
                    unitRegistry.getLocationMap()
                )

                // reject the token if the user tries to give more permissions than he has
                if (!AuthorizationHelper.isSubPermission(permission, permissionRule.permission)) {
                    throw RejectedException(
                        "User[" + authorizationToken.getUserId() + "] has not enough permissions to create an authorizationToken with permissions[" + permissionRule.permission + "] for unit[" + UnitConfigProcessor.getDefaultAlias(
                            unitConfig,
                            "?"
                        ) + "]"
                    )
                }
            }
        } catch (ex: CouldNotPerformException) {
            throw CouldNotPerformException("Could verify access token", ex)
        }
    }

    /**
     * Perform a permission check by validating that the actor is either the receiver or the sender of the message.
     *
     * @param authenticationBaseData the authentication data including who is authenticated and tokens used for authorization
     * @param userMessage            the user message for which permissions are checked
     * @param permissionType         the permission type which is checked
     * @param unitRegistry           unit registry used to resolve ids
     *
     * @return a string representing the authorized user, this is either just the username of the authenticated user
     * or the username of the authenticated user followed by the username of the issuer of the authorization token
     *
     * @throws CouldNotPerformException thrown if the user does not have permissions or if the check fails
     */
    @JvmStatic
    @Throws(CouldNotPerformException::class)
    fun canDo(
        authenticationBaseData: AuthenticationBaseData?,
        userMessage: UserMessageType.UserMessage,
        permissionType: AuthorizationHelper.PermissionType,
        unitRegistry: UnitRegistry,
    ): AuthPair {
        try {
            // validate sender
            return canDo(
                authenticationBaseData,
                unitRegistry.getUnitConfigById(userMessage.senderId),
                permissionType,
                unitRegistry,
                null,
                null
            )
        } catch (ex: CouldNotPerformException) {
            var exceptionStack: ExceptionStack? = null
            exceptionStack = MultiException.push(AuthorizationWithTokenHelper::class.java, ex, exceptionStack)

            // validate receiver if sender validation failed.
            try {
                canDo(
                    authenticationBaseData,
                    unitRegistry.getUnitConfigById(userMessage.recipientId),
                    permissionType,
                    unitRegistry,
                    null,
                    null
                )
            } catch (exx: CouldNotPerformException) {
                exceptionStack = MultiException.push(AuthorizationWithTokenHelper::class.java, exx, exceptionStack)

                userMessage.conditionList.forEach { condition ->
                    try {
                        return canDo(
                            authenticationBaseData,
                            unitRegistry.getUnitConfigById(condition.unitId),
                            permissionType,
                            unitRegistry,
                            null,
                            null
                        )
                    } catch (exxx: CouldNotPerformException) {
                        exceptionStack =
                            MultiException.push(AuthorizationWithTokenHelper::class.java, exxx, exceptionStack)
                    }
                }

                MultiException.checkAndThrow({ "Permission denied!" }, exceptionStack)
            }
        }
        throw FatalImplementationErrorException(
            "ExceptionStack empty in error case.",
            AuthorizationWithTokenHelper::class.java
        )
    }

    /**
     * Perform a permission check for authentication data including tokens.
     *
     * @param authenticationBaseData the authentication data including who is authenticated and tokens used for authorization
     * @param unitConfig             the unit config for which permissions are checked
     * @param permissionType         the permission type which is checked
     * @param unitRegistry           unit registry used to resolve ids
     * @param unitType               the unit type for which is checked if the authorization tokens gives permissions for it, if it is null
     * it will be ignored
     * @param serviceType            the service type for which is checked if the authorization tokens gives permissions for it, if it is null
     * it will be ignored
     *
     * @return a string representing the authorized user, this is either just the username of the authenticated user
     * or the username of the authenticated user followed by the username of the issuer of the authorization token
     *
     * @throws CouldNotPerformException thrown if the user does not have permissions or if the check fails
     */
    @JvmStatic
    @JvmOverloads
    @Throws(CouldNotPerformException::class)
    fun canDo(
        authenticationBaseData: AuthenticationBaseData?,
        unitConfig: UnitConfigType.UnitConfig,
        permissionType: AuthorizationHelper.PermissionType,
        unitRegistry: UnitRegistry,
        unitType: UnitTemplateType.UnitTemplate.UnitType? = null,
        serviceType: ServiceTemplateType.ServiceTemplate.ServiceType? = null,
    ): AuthPair {
        try {
            // resolve the responsible user
            val userClientPair: UserClientPairType.UserClientPair
            userClientPair = if (authenticationBaseData == null) {
                // authentication data is not given so use null to check for other permissions
                UserClientPairType.UserClientPair.getDefaultInstance()
            } else {
                if (authenticationBaseData.authenticationToken != null) {
                    // authentication token is set so use it as the responsible user
                    UserClientPairType.UserClientPair.newBuilder()
                        .setUserId(authenticationBaseData.authenticationToken.getUserId()).build()
                } else {
                    // use the user that is authenticated for the request
                    authenticationBaseData.userClientPair
                }
            }

            // check if authenticated user has needed permissions
            if (AuthorizationHelper.canDo(
                    unitConfig,
                    userClientPair.getUserId(),
                    unitRegistry.getAuthorizationGroupMap(),
                    unitRegistry.getLocationMap(),
                    permissionType
                )
            ) {
                return AuthPair(userClientPair, userClientPair.getUserId())
            }
            if (AuthorizationHelper.canDo(
                    unitConfig,
                    userClientPair.getClientId(),
                    unitRegistry.getAuthorizationGroupMap(),
                    unitRegistry.getLocationMap(),
                    permissionType
                )
            ) {
                return AuthPair(userClientPair, userClientPair.getClientId())
            }
            try {
                // test if user is part of the admin group
                val memberIdList =
                    unitRegistry.getUnitConfigByAlias(UnitRegistry.ADMIN_GROUP_ALIAS).authorizationGroupConfig.memberIdList
                if (memberIdList.contains(userClientPair.getUserId())) {
                    return AuthPair(userClientPair, userClientPair.getUserId())
                }
                if (memberIdList.contains(userClientPair.getClientId())) {
                    return AuthPair(userClientPair, userClientPair.getClientId())
                }
            } catch (ex: NotAvailableException) {
                // continue with the checks, admin group is not available
            }

            // authenticated user does not have permissions so check if the authorization token grants them
            if (authenticationBaseData != null && authenticationBaseData.authorizationToken != null) {
                val authorizationToken = authenticationBaseData.authorizationToken
                // verify that the authorization token is valid
                verifyAuthorizationToken(authorizationToken, unitRegistry)

                // verify if the token grants the necessary permissions
                return authorizedByToken(
                    authorizationToken,
                    userClientPair,
                    unitConfig,
                    permissionType,
                    unitRegistry,
                    unitType,
                    serviceType
                )
            }
            var userRepresentation = userClientPair.getUserId()
            if (!userRepresentation.isEmpty()) {
                userRepresentation += "@"
            }
            userRepresentation += userClientPair.getClientId()
            if (userRepresentation.isEmpty()) {
                userRepresentation = "Other"
            }
            throw PermissionDeniedException("User[" + userRepresentation + "] " + permissionType.name.lowercase(Locale.getDefault()) + " permission denied!")
        } catch (ex: CouldNotPerformException) {
            throw CouldNotPerformException(
                "Could not verify permissions for unit[" + UnitConfigProcessor.getDefaultAlias(
                    unitConfig,
                    "?"
                ) + "]", ex
            )
        }
    }

    @Throws(CouldNotPerformException::class)
    private fun authorizedByToken(
        authorizationToken: AuthorizationTokenType.AuthorizationToken,
        userClientPair: UserClientPairType.UserClientPair,
        unitConfig: UnitConfigType.UnitConfig,
        permissionType: AuthorizationHelper.PermissionType,
        unitRegistry: UnitRegistry,
        unitType: UnitTemplateType.UnitTemplate.UnitType?,
        serviceType: ServiceTemplateType.ServiceTemplate.ServiceType?,
    ): AuthPair {
        // verify if the token grants the necessary permissions
        val templateRegistry: TemplateRegistry = CachedTemplateRegistryRemote.getRegistry()
        val grantingPermissionSet: MutableSet<AuthorizationTokenType.AuthorizationToken.PermissionRule> = HashSet()
        val filteringPermissionSet: MutableSet<AuthorizationTokenType.AuthorizationToken.PermissionRule> = HashSet()
        for (permissionRule in authorizationToken.permissionRuleList) {
            if (permissionRule.filter) {
                filteringPermissionSet.add(permissionRule)
            } else {
                grantingPermissionSet.add(permissionRule)
            }
        }
        var granted = false
        for (permissionRule in grantingPermissionSet) {
            if (permitted(
                    unitConfig,
                    permissionRule,
                    unitRegistry,
                    templateRegistry,
                    serviceType,
                    unitType,
                    permissionType
                )
            ) {
                granted = true
                break
            }
        }
        if (!granted) {
            throw PermissionDeniedException("Authorization token does not grant the necessary permissions")
        }
        for (permissionRule in filteringPermissionSet) {
            if (permitted(
                    unitConfig,
                    permissionRule,
                    unitRegistry,
                    templateRegistry,
                    serviceType,
                    unitType,
                    permissionType
                )
            ) {
                throw PermissionDeniedException("Authorization token does not grant the necessary permissions")
            }
        }

        // build the auth pair
        return AuthPair(userClientPair, authorizationToken.getUserId())
    }

    @Throws(CouldNotPerformException::class)
    private fun permitted(
        unitConfig: UnitConfigType.UnitConfig,
        permissionRule: AuthorizationTokenType.AuthorizationToken.PermissionRule,
        unitRegistry: UnitRegistry,
        templateRegistry: TemplateRegistry,
        serviceType: ServiceTemplateType.ServiceTemplate.ServiceType?,
        unitType: UnitTemplateType.UnitTemplate.UnitType?,
        permissionType: AuthorizationHelper.PermissionType,
    ): Boolean {
        // the permission would not grant these permissions anyway so return false
        // this is done first so that rejections are handled fast
        if (!AuthorizationHelper.permitted(permissionRule.permission, permissionType)) {
            return false
        }
        // test if the unit id in the permission rule matches the unit config
        if (permissionRule.getUnitId() != unitConfig.getId()) {
            // it does not so test if the unit id belongs to a location
            val locationToCheck = unitRegistry.getUnitConfigById(permissionRule.getUnitId())
            // if it is not a location then the permission rule do not permit what is asked
            if (locationToCheck.getUnitType() != UnitTemplateType.UnitTemplate.UnitType.LOCATION) {
                return false
            }
            // if the location does not contain the given unit config the permission rule does not permit it
            if (!containsUnit(unitConfig, locationToCheck, unitRegistry)) {
                return false
            }
        }
        // if the given service type is defined and the rule contains a service type which does not match return false
        return if (serviceType != null && serviceType != ServiceTemplateType.ServiceTemplate.ServiceType.UNKNOWN && permissionRule.hasServiceTemplateId() && templateRegistry.getServiceTemplateById(
                permissionRule.getServiceTemplateId()
            )
                .getServiceType() != serviceType
        ) {
            false
        } else unitType == null || unitType == UnitTemplateType.UnitTemplate.UnitType.UNKNOWN || !permissionRule.hasUnitTemplateId() || templateRegistry.getUnitTemplateById(
            permissionRule.getUnitTemplateId()
        ).getUnitType() == unitType
        // if the given unit type is defined and the rule contains a unit type which does not match return false
    }

    @Throws(CouldNotPerformException::class)
    private fun containsUnit(
        unitConfig: UnitConfigType.UnitConfig,
        locationToCheck: UnitConfigType.UnitConfig,
        unitRegistry: UnitRegistry,
    ): Boolean {
        var location = unitRegistry.getUnitConfigById(unitConfig.placementConfig.getLocationId())
        if (location.getId() == locationToCheck.getId()) {
            return true
        }
        while (!location.locationConfig.root) {
            location = unitRegistry.getUnitConfigById(location.placementConfig.getLocationId())
            if (location.getId() == locationToCheck.getId()) {
                return true
            }
        }
        return false
    }
}
