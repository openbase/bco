package org.openbase.bco.authentication.core.mock;

/*-
 * #%L
 * BCO Authentication Core
 * %%
 * Copyright (C) 2017 openbase.org
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
import com.google.protobuf.ByteString;
import java.security.KeyPair;
import java.util.HashMap;
import org.openbase.bco.authentication.core.AuthenticationRegistry;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import rst.domotic.authentication.LoginCredentialsType.LoginCredentials;

/**
 *
 * @author <a href="mailto:cromankiewicz@techfak.uni-bielefeld.de">Constantin Romankiewicz</a>
 */
public class MockAuthenticationRegistry extends AuthenticationRegistry {

    public static final String USER_ID = "maxmustermann";
    public static final String USER_PASSWORD = "password";
    public static final byte[] USER_PASSWORD_HASH = EncryptionHelper.hash(USER_PASSWORD);

    public static final String CLIENT_ID = "client";
    public static byte[] CLIENT_PRIVATE_KEY;
    public static byte[] CLIENT_PUBLIC_KEY;

    @Override
    public void setCredentials(String userId, byte[] credentials) throws CouldNotPerformException {
        if (!this.credentials.containsKey(userId)) {
            this.setCredentials(userId, credentials, false);
        } else {
            this.setCredentials(userId, credentials, this.credentials.get(userId).getAdmin());
        }
    }

    @Override
    public void setCredentials(String userId, byte[] credentials, boolean admin) throws CouldNotPerformException {
        LoginCredentials loginCredentials = LoginCredentials.newBuilder()
                .setId(userId)
                .setCredentials(ByteString.copyFrom(credentials))
                .setAdmin(admin)
                .build();

        this.credentials.put(userId, loginCredentials);
    }

    @Override
    public void init() throws InitializationException {
        credentials = new HashMap<>();

        try {
            this.setCredentials(USER_ID, USER_PASSWORD_HASH);

            // TODO: Add private key to credentials
            KeyPair keyPair = EncryptionHelper.generateKeyPair();
            CLIENT_PRIVATE_KEY = keyPair.getPrivate().getEncoded();
            CLIENT_PUBLIC_KEY = keyPair.getPublic().getEncoded();
            this.setCredentials(CLIENT_ID, CLIENT_PUBLIC_KEY);

            // add new user to the mock authentication registry
            this.setCredentials("example_client_id", EncryptionHelper.hash("example_password"));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void load() throws CouldNotPerformException {
        // do nothing
    }

    @Override
    protected void save() {
        // do nothing
    }
}
