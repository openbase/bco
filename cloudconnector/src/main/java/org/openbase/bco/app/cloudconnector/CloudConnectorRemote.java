package org.openbase.bco.app.cloudconnector;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import org.openbase.bco.authentication.lib.AuthenticatedServiceProcessor;
import org.openbase.bco.authentication.lib.AuthorizationHelper;
import org.openbase.bco.authentication.lib.AuthorizationHelper.PermissionType;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.communication.controller.RPCHelper;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthorizationTokenType.AuthorizationToken;
import org.openbase.type.domotic.authentication.AuthorizationTokenType.AuthorizationToken.PermissionRule;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.app.AppClassType.AppClass;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class CloudConnectorRemote extends AppRemoteAdapter implements CloudConnector {

    static UnitConfig getCloudConnectorUnitConfig() throws CouldNotPerformException {
        String appClassId = "";
        for (final AppClass appClass : Registries.getClassRegistry().getAppClasses()) {
            if (LabelProcessor.contains(appClass.getLabel(), "Cloud Connector")) {
                appClassId = appClass.getId();
                break;
            }
        }

        if (appClassId.isEmpty()) {
            throw new NotAvailableException("Cloud Connector App Class");
        }

        UnitConfig cloudConnectorConfig = null;
        for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByUnitType(UnitType.APP)) {
            if (unitConfig.getAppConfig().getAppClassId().equals(appClassId)) {
                cloudConnectorConfig = unitConfig;
                break;
            }
        }

        if (cloudConnectorConfig == null) {
            throw new NotAvailableException("Cloud Connector Unit Config");
        }

        return cloudConnectorConfig;
    }

    public CloudConnectorRemote() throws CouldNotPerformException, InterruptedException {
        super(getCloudConnectorUnitConfig());
    }

    /**
     * Call to {@link #connect(AuthenticatedValue)} by using the default session test.
     *
     * @param connect flag determining if the socket connection for the user currently logged in at the default session
     *                test should be established or stopped.
     *
     * @return a future of the task created
     */
    public Future<Void> connect(final Boolean connect) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(connect, Void.class, SessionManager.getInstance(), this::connect);
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> connect(final AuthenticatedValue authenticatedValue) {
        return RPCHelper.callRemoteMethod(authenticatedValue, getAppRemote(), AuthenticatedValue.class);
    }

    /**
     * Register the user logged in at the default session test at the cloud connector.
     * This method will generate an authorization token for the user with the same permissions he posseses.
     *
     * @param password a password for the account of the user at the BCO Cloud
     *
     * @return a future of the task
     */
    public Future<Void> register(final String password) {
        try {
            return register(password, generateDefaultAuthorizationToken());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(Void.class, ex);
        }
    }

    /**
     * Register the user logged in at the default session manager at the cloud connector.
     *
     * @param password           a password for the account of the user at the BCO Cloud
     * @param authorizationToken an authorization token which will be used by the cloud connector to apply actions in the
     *                           users name.
     *
     * @return a future of the task
     */
    public Future<Void> register(final String password, final String authorizationToken) {
        final String params = CloudConnector.createRegistrationData(password, authorizationToken);
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(params, Void.class, SessionManager.getInstance(), authenticatedValue -> register(authenticatedValue));
    }


    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> register(final AuthenticatedValue authenticatedValue) {
        return RPCHelper.callRemoteMethod(authenticatedValue, getAppRemote(), AuthenticatedValue.class);
    }

    /**
     * Call {@link #remove(AuthenticatedValue)} for the user logged in at the default session manager.
     *
     * @return a future of the task
     */
    public Future<Void> remove() {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(null, Void.class, SessionManager.getInstance(), this::remove);
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> remove(final AuthenticatedValue authenticatedValue) {
        return RPCHelper.callRemoteMethod(authenticatedValue, getAppRemote(), AuthenticatedValue.class);
    }

    /**
     * Call {@link #setAuthorizationToken(AuthenticatedValue)} for the user logged in at the default session manager.
     *
     * @param authorizationToken the new authorization token for the user logged in at the default session manager.
     *
     * @return a future of the task
     */
    public Future<Void> setAuthorizationToken(final String authorizationToken) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(authorizationToken, Void.class, SessionManager.getInstance(), authenticatedValue -> setAuthorizationToken(authenticatedValue));
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> setAuthorizationToken(final AuthenticatedValue authenticatedValue) {
        return RPCHelper.callRemoteMethod(authenticatedValue, getAppRemote(), AuthenticatedValue.class);
    }

    /**
     * Generate a default authorization token for the cloud connector from the currently logged in user.
     * This token will grant the same permissions as the currently logged in user.
     *
     * @return an authorization token as described above
     *
     * @throws CouldNotPerformException if the token could not be generated
     */
    public String generateDefaultAuthorizationToken() throws CouldNotPerformException {
        try {
            // retrieve the current user id
            final String userId = SessionManager.getInstance().getUserClientPair().getUserId();
            // create a new token and set the user id
            final AuthorizationToken.Builder authorizationToken = AuthorizationToken.newBuilder().setUserId(userId);
            // generate a minimal set of units ids needed for the same access permissions as the user
            final Set<String> accessPermissionSet = new HashSet<>();
            final Set<String> readPermissionSet = new HashSet<>();
            final Set<String> writePermissionSet = new HashSet<>();
            getUnitsWithPermission(accessPermissionSet, Registries.getUnitRegistry().getRootLocationConfig(), userId, PermissionType.ACCESS);
            getUnitsWithPermission(readPermissionSet, Registries.getUnitRegistry().getRootLocationConfig(), userId, PermissionType.READ);
            getUnitsWithPermission(writePermissionSet, Registries.getUnitRegistry().getRootLocationConfig(), userId, PermissionType.WRITE);

            final Set<String> unitIds = new HashSet<>();
            unitIds.addAll(accessPermissionSet);
            unitIds.addAll(readPermissionSet);
            unitIds.addAll(writePermissionSet);

            for (final String unitId : unitIds) {
                final PermissionRule.Builder builder = authorizationToken.addPermissionRuleBuilder();
                builder.setUnitId(unitId);
                builder.getPermissionBuilder().
                        setAccess(accessPermissionSet.contains(unitId)).
                        setRead(readPermissionSet.contains(unitId)).
                        setWrite(writePermissionSet.contains(unitId));
            }

            // give permissions for own user
            final PermissionRule.Builder builder = authorizationToken.addPermissionRuleBuilder();
            builder.setUnitId(userId);
            builder.getPermissionBuilder().setRead(true).setAccess(true).setWrite(true);


            for (PermissionRule permissionRule : authorizationToken.getPermissionRuleList()) {
                UnitConfig unitConfigById = Registries.getUnitRegistry().getUnitConfigById(permissionRule.getUnitId());
                //System.out.println(LabelProcessor.getBestMatch(unitConfigById.getLabel()) + " - " + unitConfigById.getAlias(0) + " [" + permissionRule.getPermission().getRead() + ", " + permissionRule.getPermission().getWrite() + ". " + permissionRule.getPermission().getWrite() + "]");
            }
            // request such a token from the unit registry
            return Registries.getUnitRegistry().requestAuthorizationToken(authorizationToken.build()).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Interrupted while requesting authorization token", ex);
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not request authorization token", ex);
        }
    }

    /**
     * This method is a helper method for generating an authorization token with the same permissions of a certain type
     * as a user.
     * <p>
     * Traverse the location hierarchy recursively and add all ids of units the currently logged in user has
     * permissions for to a set.
     * The method will only add the topmost locations to the set. This means that if the user has permissions
     * for a location its id will be added and its children will be ignored. However, if the user does not have
     * permission for a locations its children are checked recursively.
     *
     * @param unitIdSet      an empty set which will be filled with unit ids by this method
     * @param location       the location where the recursive traversal is started
     * @param userId         the id of the user for whom permissions are checked
     * @param permissionType the permission type checked
     *
     * @throws CouldNotPerformException if the process fails
     */
    public void getUnitsWithPermission(final Set<String> unitIdSet, final UnitConfig location, final String userId, final PermissionType permissionType) throws CouldNotPerformException {
        try {
            if (AuthorizationHelper.canDo(location, userId,
                    Registries.getUnitRegistry().getAuthorizationGroupMap(),
                    Registries.getUnitRegistry().getLocationMap(), permissionType)) {
                // if the user has access permissions for the given location just add it and do nothing more
                unitIdSet.add(location.getId());
            } else {
                // create a set of unit ids which are directly found in this location and not one of its children
                // so add all unit ids first and remove all ones that are contained in its children in the loop
                final Set<String> internalUnitIdSet = new HashSet<>(location.getLocationConfig().getUnitIdList());
                // recursively add ids for child locations
                for (final String childId : location.getLocationConfig().getChildIdList()) {
                    final UnitConfig locationUnitConfig = Registries.getUnitRegistry().getUnitConfigById(childId);
                    getUnitsWithPermission(unitIdSet, Registries.getUnitRegistry().getUnitConfigById(childId), userId, permissionType);
                    for (String unitId : locationUnitConfig.getLocationConfig().getUnitIdList()) {
                        internalUnitIdSet.remove(unitId);
                    }
                }

                // iterate over all unit ids directly placed in this location and add them if the user has access permissions
                for (final String unitId : internalUnitIdSet) {
                    if (!unitIdSet.contains(unitId)) {
                        final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);
                        if (AuthorizationHelper.canDo(unitConfig, userId,
                                Registries.getUnitRegistry().getAuthorizationGroupMap(),
                                Registries.getUnitRegistry().getLocationMap(), permissionType)) {
                            unitIdSet.add(unitId);
                        }
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate minimal set of units ids with [" + permissionType.name() + "] permissions", ex);
        }
    }
}
