package org.openbase.bco.app.cloud.connector;

import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface CloudConnectorInterface {

    /**
     * Create a connection for a user to the BCO Cloud or stop it. This enables usage of the Google Assistant with BCO.
     * If the user connects for the first time the cloud connector will attempt to register the user at the BCO Cloud.
     * Additionally the cloud connector needs an authorization token from the user giving him permissions
     * needed to apply actions in the users name.
     * Therefore the internal value of the authenticated value should be the string of a json object containing the
     * following values:
     * <ul>
     * <li>String: password_hash</li>
     * <li>String: password_salt</li>
     * <li>String: authorization_token</li>
     * <li>Boolean: auto_start</li>
     * </ul>
     * The password hash and salt will be supplemented with the username and the email hash for the authenticated user
     * and send to the BCO Cloud for the registration. The auto start value is optional and can be used to connect
     * the user automatically if the cloud connector is restarted. Its default value is true.
     * The cloud connector will save the authorization token and an access token received after logging in at the
     * BCO Cloud.
     * Therefore subsequent calls only need a json object containing a boolean telling if the user wishes to connect
     * or disconnect:
     * <ul>
     * <li>Boolean: connect</li>
     * </ul>
     *
     * @param authenticatedValue the authenticated value authenticating a user and containing a value as described above
     * @return a future of the created task
     * @throws CouldNotPerformException if the task could not be created
     */
    @RPCMethod
    Future<AuthenticatedValue> connect(final AuthenticatedValue authenticatedValue) throws CouldNotPerformException;
}
