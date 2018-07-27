package org.openbase.bco.registry.unit.lib.auth;

/*-
 * #%L
 * BCO Registry Unit Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import com.google.protobuf.ProtocolStringList;
import org.openbase.bco.authentication.lib.AuthenticationBaseData;
import org.openbase.bco.authentication.lib.AuthorizationHelper;
import org.openbase.bco.authentication.lib.AuthorizationHelper.PermissionType;
import org.openbase.bco.registry.template.lib.TemplateRegistry;
import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.PermissionDeniedException;
import org.openbase.jul.exception.RejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.AuthorizationTokenType.AuthorizationToken;
import rst.domotic.authentication.PermissionType.Permission;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.Map;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AuthorizationWithTokenHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationWithTokenHelper.class);

    /**
     * Verify an authorization token. This is done in two steps.
     * By checking if all entered ids match either a UnitConfig, ServiceTemplate or UnitTemplate.
     * If the id matches a unit config it is checked if the user defined in the token has at least as many permissions
     * for the unit as the token would grant.
     *
     * @param authorizationToken the authorization token that is checked
     * @param unitRegistry       registry used to resolve authorization groups and locations to check permissions
     * @throws CouldNotPerformException thrown if the token is invalid
     */
    public static void verifyAuthorizationToken(final AuthorizationToken authorizationToken, final UnitRegistry unitRegistry) throws CouldNotPerformException {
        try {
            for (final AuthorizationToken.MapFieldEntry entry : authorizationToken.getPermissionRuleList()) {
                // entry is for a unit
                if (unitRegistry.containsUnitConfigById(entry.getKey())) {
                    // evaluate the permissions the given user has for the unit defined in the token
                    final UnitConfig unitConfig = unitRegistry.getUnitConfigById(entry.getKey());
                    final Permission permission = AuthorizationHelper.getPermission(unitConfig, authorizationToken.getUserId(), unitRegistry.getAuthorizationGroupMap(), unitRegistry.getLocationMap());

                    // reject the token if the user tries to give more permissions than he has
                    if (!AuthorizationHelper.isSubPermission(permission, entry.getValue())) {
                        throw new RejectedException("User[" + authorizationToken.getUserId() + "] has not enough permissions to create an authorizationToken with permissions[" + entry.getValue() + "] for unit[" + unitConfig.getAlias(0) + "]");
                    }

                    continue;
                }

                // entry can also be for a service type or unit type
                // in this case just verify the id
                final TemplateRegistry templateRegistry = CachedTemplateRegistryRemote.getRegistry();
                if (templateRegistry.containsServiceTemplateById(entry.getKey())) {
                    continue;
                }

                if (templateRegistry.containsUnitTemplateById(entry.getKey())) {
                    continue;
                }

                // throw an exception that the id in the token is invalid
                throw new NotAvailableException("UnitConfig, ServiceTemplate or UnitTemplate with id [" + entry.getKey() + "]");
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could verify access token", ex);
        }
    }

    /**
     * Perform a permission check according to {@link #canDo(AuthenticationBaseData, UnitConfig, PermissionType, UnitRegistry, UnitType, ServiceType)}
     * by ignoring unit type and service type permissions.
     *
     * @param authenticationBaseData the authentication data including who is authenticated and tokens used for authorization
     * @param unitConfig             the unit config for which permissions are checked
     * @param permissionType         the permission type which is checked
     * @param unitRegistry           unit registry used to resolve ids
     * @return a string representing the authorized user, this is either just the username of the authenticated user
     * or the username of the authenticated user followed by the username of the issuer of the authorization token
     * @throws CouldNotPerformException thrown if the user does not have permissions or if the check fails
     */
    public static String canDo(
            final AuthenticationBaseData authenticationBaseData,
            final UnitConfig unitConfig,
            final PermissionType permissionType,
            final UnitRegistry unitRegistry) throws CouldNotPerformException {
        return canDo(authenticationBaseData, unitConfig, permissionType, unitRegistry, null, null);
    }


    /**
     * Perform a permission check for authentication data including tokens.
     *
     * @param authenticationBaseData the authentication data including who is authenticated and tokens used for authorization
     * @param unitConfig             the unit config for which permissions are checked
     * @param permissionType         the permission type which is checked
     * @param unitRegistry           unit registry used to resolve ids
     * @param unitType               the unit type for which is checked if the authorization tokens gives permissions for it, if it is null
     *                               or unknown it will be ignored
     * @param serviceType            the service type for which is checked if the authorization tokens gives permissions for it, if it is null
     *                               or unknown it will be ignored
     * @return a string representing the authorized user, this is either just the username of the authenticated user
     * or the username of the authenticated user followed by the username of the issuer of the authorization token
     * @throws CouldNotPerformException thrown if the user does not have permissions or if the check fails
     */
    public static String canDo(
            final AuthenticationBaseData authenticationBaseData,
            final UnitConfig unitConfig,
            final PermissionType permissionType,
            final UnitRegistry unitRegistry,
            final UnitType unitType,
            final ServiceType serviceType) throws CouldNotPerformException {
        try {
            // resolve the responsible user
            String userId;
            if (authenticationBaseData == null) {
                // authentication data is not given so use null to check for other permissions
                userId = null;
            } else {
                if (authenticationBaseData.getAuthenticationToken() != null) {
                    // authentication token is set so use it as the responsible user
                    userId = authenticationBaseData.getAuthenticationToken().getUserId();
                } else {
                    // use the user that is authenticated for the request
                    userId = authenticationBaseData.getUserId();
                }
            }

            // check if authenticated user has needed permissions
            if (AuthorizationHelper.canDo(unitConfig, userId, unitRegistry.getAuthorizationGroupMap(), unitRegistry.getLocationMap(), permissionType)) {
                return resolveUsername(userId, unitRegistry);
            }

            try {
                // user is part of the admin group so allow anything
                if (userId != null) {
                    final ProtocolStringList memberIdList = unitRegistry.getUnitConfigByAlias(UnitRegistry.ADMIN_GROUP_ALIAS).getAuthorizationGroupConfig().getMemberIdList();
                    for (final String id : userId.split("@")) {
                        if (memberIdList.contains(id)) {
                            return resolveUsername(userId, unitRegistry);
                        }
                    }
                }
            } catch (NotAvailableException ex) {
                // continue with the checks, admin group is not available
            }

            // authenticated user does not have permissions so check if the authorization token grants them
            if (authenticationBaseData != null && authenticationBaseData.getAuthorizationToken() != null) {
                final AuthorizationToken authorizationToken = authenticationBaseData.getAuthorizationToken();
                // verify that the authorization token is valid
                verifyAuthorizationToken(authorizationToken, unitRegistry);

                // verify if the token grants the necessary permissions
                return authorizedByToken(authorizationToken, userId, unitConfig, permissionType, unitRegistry, unitType, serviceType);
            }
            throw new PermissionDeniedException("User[" + userId + "] " + permissionType.name().toLowerCase() + " permission denied!");
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not verify permissions for unit[" + unitConfig.getAlias(0) + "]", ex);
        }
    }

    private static String authorizedByToken(
            final AuthorizationToken authorizationToken,
            final String userId,
            final UnitConfig unitConfig,
            final PermissionType permissionType,
            final UnitRegistry unitRegistry,
            final UnitType unitType,
            final ServiceType serviceType) throws CouldNotPerformException {
        // build authorization string: x authorized by y
        String result = resolveUsername(userId, unitRegistry);
        result += " authorized by ";
        result += resolveUsername(authorizationToken.getUserId(), unitRegistry);

        // verify if the token grants the necessary permissions
        final TemplateRegistry templateRegistry = CachedTemplateRegistryRemote.getRegistry();
        for (final AuthorizationToken.MapFieldEntry entry : authorizationToken.getPermissionRuleList()) {
            // check for service permissions
            if (templateRegistry.containsServiceTemplateById(entry.getKey())) {
                if (serviceType != null && serviceType != ServiceType.UNKNOWN) {
                    if (serviceType == templateRegistry.getServiceTemplateById(entry.getKey()).getType() && AuthorizationHelper.permitted(entry.getValue(), permissionType)) {
                        // token grants permissions for the needed service type
                        if (canDo(unitConfig, authorizationToken.getUserId(), unitRegistry, permissionType)) {
                            // token issuer also has the necessary permissions for the unit
                            return result;
                        }
                    }
                }
            }
            // check for unit permissions
            if (templateRegistry.containsUnitTemplateById(entry.getKey())) {
                if (unitType != null && unitType != UnitType.UNKNOWN) {
                    if (unitType == templateRegistry.getUnitTemplateById(entry.getKey()).getType() && AuthorizationHelper.permitted(entry.getValue(), permissionType)) {
                        // token grants permissions for the needed unit type
                        if (canDo(unitConfig, authorizationToken.getUserId(), unitRegistry, permissionType)) {
                            // token issuer also has the necessary permissions for the unit
                            return result;
                        }
                    }
                }
            }
            // check if the token directly grants permissions for the needed unit
            if (entry.getKey().equals(unitConfig.getId()) && AuthorizationHelper.permitted(entry.getValue(), permissionType)) {
                return result;
            }
            // check if the token grants permissions for a location the given unit is in
            if (unitRegistry.containsUnitConfigById(entry.getKey())) {
                final UnitConfig location = unitRegistry.getUnitConfigById(entry.getKey());
                if (location.getUnitType() == UnitType.LOCATION && location.getLocationConfig().getUnitIdList().contains(unitConfig.getId()) && AuthorizationHelper.permitted(entry.getValue(), permissionType)) {
                    return result;
                }
            }
        }

        throw new PermissionDeniedException("Authorization token does not grant the necessary permissions");
    }

    private static String resolveUsername(final String userId, final UnitRegistry unitRegistry) throws CouldNotPerformException {
        if (userId == null || userId.isEmpty()) {
            return "Other";
        } else {
            final String[] split = userId.split("@");
            if (split.length > 1) {
                String result = "";
                if (!split[0].isEmpty()) {
                    result += unitRegistry.getUnitConfigById(split[0]).getUserConfig().getUserName();
                }
                if (!split[1].isEmpty()) {
                    if (!result.isEmpty()) {
                        result += "@";
                    }
                    result += unitRegistry.getUnitConfigById(split[1]).getUserConfig().getUserName();
                }
                return result;
            } else {
                return unitRegistry.getUnitConfigById(userId.replace("@", "")).getUserConfig().getUserName();
            }
        }
    }

    /**
     * Check for a permission type of a unit for a user.
     * Helper method used to shorten calls to {@link AuthorizationHelper#canDo(UnitConfig, String, Map, Map, PermissionType)}
     * by resolving both maps from the given unit registry.
     *
     * @param unitConfig     the unit config for which permissions are checked
     * @param userId         the id of the user that is checked if he has the according permissions
     * @param unitRegistry   registry used to resolve authorization groups and locations for the check.
     * @param permissionType the permissions type which is checked
     * @return true if the user has permissions and else false
     * @throws CouldNotPerformException if the check could not be performed
     */
    private static boolean canDo(
            final UnitConfig unitConfig,
            final String userId,
            final UnitRegistry unitRegistry,
            final PermissionType permissionType) throws CouldNotPerformException {
        return AuthorizationHelper.canDo(unitConfig, userId, unitRegistry.getAuthorizationGroupMap(), unitRegistry.getLocationMap(), permissionType);
    }
}
