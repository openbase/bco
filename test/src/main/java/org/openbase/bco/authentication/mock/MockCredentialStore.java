package org.openbase.bco.authentication.mock;

/*-
 * #%L
 * BCO Authentication Test
 * %%
 * Copyright (C) 2017 - 2018 openbase.org
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
import java.security.KeyPair;
import java.util.HashMap;
import org.openbase.bco.authentication.lib.CredentialStore;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;

/**
 *
 * @author <a href="mailto:cromankiewicz@techfak.uni-bielefeld.de">Constantin Romankiewicz</a>
 */
public class MockCredentialStore extends CredentialStore {

    public static final String USER_ID = "user";
    public static final String USER_PASSWORD = "password";
    public static final byte[] USER_PASSWORD_HASH = EncryptionHelper.hash(USER_PASSWORD);

    public static final String ADMIN_ID = "admin";
    public static final String ADMIN_PASSWORD = "password";
    public static final byte[] ADMIN_PASSWORD_HASH = EncryptionHelper.hash(USER_PASSWORD);

    public static final String CLIENT_ID = "client";

    public static final KeyPair SERVICE_SERVER_KEY_PAIR = EncryptionHelper.generateKeyPair();

    public MockCredentialStore() throws InitializationException {
        super("mock_server_store.json");
    }
    
    @Override
    public void init() throws InitializationException {
        this.setCredentials(ADMIN_ID, ADMIN_PASSWORD_HASH);
        this.setCredentials(USER_ID, USER_PASSWORD_HASH);
        this.setCredentials(SERVICE_SERVER_ID, SERVICE_SERVER_KEY_PAIR.getPublic().getEncoded());
        try {
            this.setAdmin(ADMIN_ID, true);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void saveStore() {
    }
}
