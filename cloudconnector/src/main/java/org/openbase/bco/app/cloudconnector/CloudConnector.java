package org.openbase.bco.app.cloudconnector;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.openbase.bco.dal.lib.layer.unit.app.App;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Future;

/**
 * Interface for the cloud connector app.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface CloudConnector extends App {

    Integer SALT_LENGTH = 16;
    String HASH_ALGORITHM = "SHA-512";

    String AUTHORIZATION_TOKEN_KEY = "authorization_token";
    String EMAIL_HASH_KEY = "email_hash";
    String PASSWORD_HASH_KEY = "password_hash";
    String PASSWORD_SALT_KEY = "password_salt";
    String USERNAME_KEY = "username";

    /**
     * Connect or disconnect the socket connection of a user to the BCO Cloud.
     * The internal value of the authenticated value has to be a boolean value.
     * True will establish a connection if not yet connected and false will disconnect if connected.
     *
     * @param authenticatedValue the authenticated value authenticating a user and containing a value as described above
     * @return a future of the created task
     */
    @RPCMethod
    Future<AuthenticatedValue> connect(final AuthenticatedValue authenticatedValue);

    /**
     * Register a user at the BCO Cloud. This enables usage of the Google Assistant with BCO.
     * To do this a password for the user is needed. It needs to be hashed together with a salt and encoded using
     * Base64. Additionally the cloud connector needs an authorization token to perform actions with the user's
     * permissions.
     * Therefore the internal value of the authenticated value should be the string of a json object containing the
     * following values:
     * <ul>
     * <li>String: password_hash</li>
     * <li>String: password_salt</li>
     * <li>String: authorization_token</li>
     * </ul>
     * To generate such a parameter refer to {@link #createRegistrationData(String, String)} so that a valid
     * salt is generated and the correct hashing algorithm is used.
     * The password hash and salt will be supplemented with the username and the email hash for the authenticated user
     * and send to the BCO Cloud for the registration.
     * After calling this method a socket connection for the user to the BCO Cloud is established. When the cloud
     * connector app is restarted a connection for each registered user will be established automatically.
     *
     * @param authenticatedValue the authenticated value authenticating a user and containing a value as described above
     * @return a future of the created task
     */
    @RPCMethod
    Future<AuthenticatedValue> register(final AuthenticatedValue authenticatedValue);

    /**
     * Remove a user from the BCO Cloud and from the cloud connector. This method will delete the account at the
     * Cloud and delete all tokens for the user saved from the authenticator.
     * The authenticated value only needs to authenticate the user which should be deleted.
     * Obviously this method will do nothing if the user is not yet registered.
     *
     * @param authenticatedValue the authenticated value authenticating a user
     * @return a future of the created task
     */
    @RPCMethod
    Future<AuthenticatedValue> remove(final AuthenticatedValue authenticatedValue);

    /**
     * Set the authorization token used by the cloud connector to perform actions for the user.
     *
     * @param authenticatedValue the authenticated value authenticating a user and containing a new authorization token
     * @return a future of the created task
     */
    @RPCMethod
    Future<AuthenticatedValue> setAuthorizationToken(final AuthenticatedValue authenticatedValue);

    /**
     * Create parameters for registration, see {@link #register(AuthenticatedValue)}.
     *
     * @param password           the password for the user account at the BCO Cloud.
     * @param authorizationToken the token used by the cloud connector to perform actions for the user
     * @return a string that can be send to the cloud connector for registration
     */
    static String createRegistrationData(final String password, final String authorizationToken) {
        // generate salt
        final byte[] saltBytes = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(saltBytes);
        final String passwordSalt = Base64.getEncoder().encodeToString(saltBytes);

        // generate password hash
        final String passwordHash = hash(password, passwordSalt);

        // generate JsonObject
        final JsonObject loginData = new JsonObject();
        loginData.addProperty(PASSWORD_HASH_KEY, passwordHash);
        loginData.addProperty(PASSWORD_SALT_KEY, passwordSalt);
        loginData.addProperty(AUTHORIZATION_TOKEN_KEY, authorizationToken);
        return new Gson().toJson(loginData);
    }

    /**
     * Hash a set of strings using {@link #HASH_ALGORITHM} and encode the resulting hash with Base64.
     *
     * @param values the strings which should be hashed together
     * @return the hashed and encoded value
     */
    static String hash(final String... values) {
        try {
            final MessageDigest hashGenerator = MessageDigest.getInstance(HASH_ALGORITHM);
            for (final String value : values) {
                hashGenerator.update(value.getBytes());
            }
            return Base64.getEncoder().encodeToString(hashGenerator.digest());
        } catch (NoSuchAlgorithmException ex) {
            ExceptionPrinter.printHistory(new FatalImplementationErrorException(CloudConnector.class, ex), LoggerFactory.getLogger(CloudConnector.class));
            return null;
        }
    }
}
