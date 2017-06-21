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

import java.util.HashMap;
import org.openbase.bco.authentication.core.AuthenticationRegistry;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.jul.exception.InitializationException;

/**
 *
 * @author <a href="mailto:cromankiewicz@techfak.uni-bielefeld.de">Constantin Romankiewicz</a>
 */
public class MockAuthenticationRegistry extends AuthenticationRegistry {
    
    public static final String CLIENT_ID = "maxmustermann";
    public static final String PASSWORD = "password";
    public static final byte[] PASSWORD_HASH = EncryptionHelper.hash(PASSWORD);

    @Override
    public void setCredentials(String userId, byte[] credentials) {
        this.credentials.put(userId, credentials);
    }

    @Override
    public void init() throws InitializationException {
        credentials = new HashMap<>();
        credentials.put(CLIENT_ID, PASSWORD_HASH);
    }

}
