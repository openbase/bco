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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LoginDataHelper {

    public static final Integer SALT_LENGTH = 16;
    public static final String HASH_ALGORITHM = "SHA-512";

    public static final String EMAIL_HASH_KEY = "email_hash";
    public static final String PASSWORD_HASH_KEY = "password_hash";
    public static final String PASSWORD_SALT_KEY = "password_salt";

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginDataHelper.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Gson gson = new Gson();


    public static String createLoginData(final String password, final String email) {
        try {
            // generate salt
            final byte[] saltBytes = new byte[SALT_LENGTH];
            SECURE_RANDOM.nextBytes(saltBytes);
            final String passwordSalt = Base64.getEncoder().encodeToString(saltBytes);

            // generate password hash
            final MessageDigest hashGenerator = MessageDigest.getInstance(HASH_ALGORITHM);
            hashGenerator.update(password.getBytes());
            hashGenerator.update(passwordSalt.getBytes());
            byte[] passwordHashBytes = hashGenerator.digest();
            final String passwordHash = Base64.getEncoder().encodeToString(passwordHashBytes);

            // generate email hash
            hashGenerator.update(email.getBytes());
            byte[] emailHashBytes = hashGenerator.digest();
            final String emailHash = Base64.getEncoder().encodeToString(emailHashBytes);

            // generate JsonObject
            final JsonObject loginData = new JsonObject();
            loginData.addProperty(EMAIL_HASH_KEY, emailHash);
            loginData.addProperty(PASSWORD_HASH_KEY, passwordHash);
            loginData.addProperty(PASSWORD_SALT_KEY, passwordSalt);
            return gson.toJson(loginData);
        } catch (NoSuchAlgorithmException ex) {
            ExceptionPrinter.printHistory(new FatalImplementationErrorException(LoginDataHelper.class, ex), LOGGER);
            return null;
        }
    }
}
