package org.openbase.bco.app.cloud.connector;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 openbase.org
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

import com.google.gson.JsonObject;
import org.openbase.bco.authentication.lib.AuthorizationHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.AuthorizationTokenType.AuthorizationToken;
import rst.domotic.authentication.AuthorizationTokenType.AuthorizationToken.PermissionRule;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.app.AppClassType.AppClass;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class CloudConnectorAppRemote extends AppRemoteAdapter implements CloudConnectorApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudConnectorAppRemote.class);

    public static UnitConfig getCloudConnectorUnitConfig() throws CouldNotPerformException {
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
        for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.APP)) {
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

    public CloudConnectorAppRemote() throws CouldNotPerformException, InterruptedException {
        super(getCloudConnectorUnitConfig());
    }

    /**
     * This method can be used to tell the cloud connector to connect or disconnect for a user that has already been
     * registered. For registration have a look at one of the other connect methods of this class.
     *
     * @param connect flag telling the cloud connector to connect or disconnect from the cloud
     * @return a future of the task
     * @throws CouldNotPerformException if the task could not be created
     */
    public Future<String> connect(final boolean connect) throws CouldNotPerformException {
        final JsonObject params = new JsonObject();
        params.addProperty("connect", connect);
        final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(params.toString(), null, null);
        final Future<AuthenticatedValue> internalFuture = connect(authenticatedValue);
        return new AuthenticatedValueFuture<>(internalFuture, String.class, authenticatedValue.getTicketAuthenticatorWrapper(), SessionManager.getInstance());
    }

    /**
     * Create a connection from the cloud connector to the BCO Cloud for the currently logger in user.
     * This method will set the auto start flag to true per default and will automatically generate an authorization
     * token for the user granting the cloud connector the same access permissions he has.
     *
     * @param password a password for the account of the user at the BCO Cloud
     * @return a future of the task
     * @throws CouldNotPerformException if the task could not be created
     */
    public Future<String> connect(final String password) throws CouldNotPerformException {
        return connect(password, true);
    }

    /**
     * Create a connection from the cloud connector to the BCO Cloud for the currently logger in user.
     * This method will set the auto start flag to true per default.
     *
     * @param password           a password for the account of the user at the BCO Cloud
     * @param authorizationToken an authorization token which will be used by the cloud connector to apply actions in the
     *                           users name.
     * @return a future of the task
     * @throws CouldNotPerformException if the task could not be created
     */
    public Future<String> connect(final String password, final String authorizationToken) throws CouldNotPerformException {
        return connect(password, authorizationToken, true);
    }

    /**
     * Create a connection from the cloud connector to the BCO Cloud for the currently logger in user.
     * This method will automatically generate an authorization token for the user granting the cloud connector
     * the same access permissions he has.
     *
     * @param password  a password for the account of the user at the BCO Cloud
     * @param autoStart flag determining if the cloud connector should automatically create a socket connection
     *                  for the user when started
     * @return a future of the task
     * @throws CouldNotPerformException if the task could not be created
     */
    public Future<String> connect(final String password, final boolean autoStart) throws CouldNotPerformException {
        return connect(password, generateDefaultAuthorizationToken(), autoStart);
    }

    /**
     * Create a connection from the cloud connector to the BCO Cloud for the currently logger in user.
     *
     * @param password           a password for the account of the user at the BCO Cloud
     * @param authorizationToken an authorization token which will be used by the cloud connector to apply actions in the
     *                           users name.
     * @param autoStart          flag determining if the cloud connector should automatically create a socket connection
     *                           for the user when started
     * @return a future of the task
     * @throws CouldNotPerformException if the task could not be created
     */
    public Future<String> connect(final String password, final String authorizationToken, final boolean autoStart) throws CouldNotPerformException {
        final String params = RegistrationHelper.createRegistrationData(password, authorizationToken, autoStart);
        LOGGER.info("Send params [" + params + "]");
        final AuthenticatedValue authenticatedValue = SessionManager.getInstance().initializeRequest(params, null, null);
        final Future<AuthenticatedValue> internalFuture = connect(authenticatedValue);
        return new AuthenticatedValueFuture<>(internalFuture, String.class, authenticatedValue.getTicketAuthenticatorWrapper(), SessionManager.getInstance());
    }

    /**
     * {@inheritDoc}
     *
     * @param authenticatedValue {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<AuthenticatedValue> connect(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(authenticatedValue, getAppRemote(), AuthenticatedValue.class);
    }

    @Override
    public Future<AuthenticatedValue> register(AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(authenticatedValue, getAppRemote(), AuthenticatedValue.class);
    }

    @Override
    public Future<AuthenticatedValue> remove(AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(authenticatedValue, getAppRemote(), AuthenticatedValue.class);
    }

    @Override
    public Future<AuthenticatedValue> setAuthorizationToken(AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(authenticatedValue, getAppRemote(), AuthenticatedValue.class);
    }

    @Override
    public Future<AuthenticatedValue> setAutoStart(AuthenticatedValue authenticatedValue) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(authenticatedValue, getAppRemote(), AuthenticatedValue.class);
    }

    /**
     * Generate a default authorization token for the cloud connector from the currently logged in user.
     * This token will grant all access permission of the currently logged in user.
     *
     * @return an authorization token as described above
     * @throws CouldNotPerformException if the token could not be generated
     */
    private String generateDefaultAuthorizationToken() throws CouldNotPerformException {
        try {
            // retrieve the current user id
            final String userId = SessionManager.getInstance().getUserId();
            // create a new token and set the user id
            final AuthorizationToken.Builder authorizationToken = AuthorizationToken.newBuilder().setUserId(userId);
            // generate a minimal set of units ids needed for the same access permissions as the user
            final Set<String> accessPermissionSet = new HashSet<>();
            getUnitsWithAccessPermissions(accessPermissionSet, Registries.getUnitRegistry().getRootLocationConfig(), userId);
            // add permissions rules for all resolved unit ids with access permissions
            for (final String unitId : accessPermissionSet) {
                PermissionRule.Builder builder = authorizationToken.addPermissionRuleBuilder();
                builder.setUnitId(unitId);
                builder.getPermissionBuilder().setAccess(true).setRead(false).setWrite(false);
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
     * This method is a helper method for generating an authorization token with the same access permissions
     * as a user.
     * <p>
     * Traverse the location hierarchy recursively and add all ids of units the currently logged in user has
     * access permissions for to a set.
     * The method will only add the topmost locations to the set. This means that if the user has access permissions
     * for a location its id will be added and its children will be ignored. However, if the user does not have
     * access permission for a locations its children are checked recursively.
     *
     * @param unitIdSet an empty set which will be filled with unit ids by this method
     * @param location  the location where the recursive traversal is started
     * @param userId    the id of the user for whom access permissions are checked
     * @throws CouldNotPerformException if the process fails
     */
    private void getUnitsWithAccessPermissions(final Set<String> unitIdSet, final UnitConfig location, final String userId) throws CouldNotPerformException {
        try {
            if (AuthorizationHelper.canAccess(location, userId,
                    Registries.getUnitRegistry().getAuthorizationGroupMap(),
                    Registries.getUnitRegistry().getLocationMap())) {
                // if the user has access permissions for the given location just add it and do nothing more
                unitIdSet.add(location.getId());
            } else {
                // create a set of unit ids which are directly found in this location and not one of its children
                // so add all unit ids first and remove all ones that are contained in its children in the loop
                final Set<String> internalUnitIdSet = new HashSet<>(location.getLocationConfig().getUnitIdList());
                // recursively add ids for child locations
                for (final String childId : location.getLocationConfig().getChildIdList()) {
                    final UnitConfig locationUnitConfig = Registries.getUnitRegistry().getUnitConfigById(childId);
                    getUnitsWithAccessPermissions(unitIdSet, Registries.getUnitRegistry().getUnitConfigById(childId), userId);
                    for (String unitId : locationUnitConfig.getLocationConfig().getUnitIdList()) {
                        internalUnitIdSet.remove(unitId);
                    }
                }

                // iterate over all unit ids directly placed in this location and add them if the user has access permissions
                for (final String unitId : internalUnitIdSet) {
                    if (!unitIdSet.contains(unitId)) {
                        final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);
                        if (AuthorizationHelper.canAccess(unitConfig, userId,
                                Registries.getUnitRegistry().getAuthorizationGroupMap(),
                                Registries.getUnitRegistry().getLocationMap())) {
                            unitIdSet.add(unitId);
                        }
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate minimal set of units ids with access permissions", ex);
        }
    }
}
