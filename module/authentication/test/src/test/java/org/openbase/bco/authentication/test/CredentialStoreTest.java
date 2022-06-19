package org.openbase.bco.authentication.test;

/*-
 * #%L
 * BCO Authentication Test
 * %%
 * Copyright (C) 2017 - 2021 openbase.org
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openbase.bco.authentication.lib.CredentialStore;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.bco.authentication.lib.jp.JPResetCredentials;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.LoggerFactory;
import java.security.KeyPair;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class CredentialStoreTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CredentialStoreTest.class);

    public CredentialStoreTest() {
    }

    @BeforeAll
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();
        JPService.registerProperty(JPResetCredentials.class);
        JPService.registerProperty(JPCredentialsDirectory.class);
    }

    /**
     * @throws Exception
     */
    @Test
    public void testSavingAndLoading() throws Exception {
        System.out.println("testSavingAndLoading");

        String storeFileName = "credential_store.json";

        // create initial instance which has to create a new file
        CredentialStore credentialStore = new CredentialStore();
        credentialStore.init(storeFileName);

        // add a user to this registry
        String userId = "tamino";
        String password = "12345678";
        credentialStore.addCredentials(userId, EncryptionHelper.hash(password), false, true);
        // add a client to the registry
        String clientIdPrivate = "clientPrivate";
        String clientIdPublic = "clientPublic";
        KeyPair keyPair = EncryptionHelper.generateKeyPair();
        credentialStore.addCredentials(clientIdPrivate, keyPair.getPrivate().getEncoded(), false, false);
        credentialStore.addCredentials(clientIdPublic, keyPair.getPublic().getEncoded(), false, false);

        // start a second registry which loads the file from the first one
        CredentialStore loadingCredentialStore = new CredentialStore();
        loadingCredentialStore.init(storeFileName);
        
        // test if they produce the same result
        try {
            byte[] passwordHash = loadingCredentialStore.getCredentials(userId).toByteArray();
            assertArrayEquals(credentialStore.getCredentials(userId).toByteArray(), passwordHash);

            assertArrayEquals(keyPair.getPrivate().getEncoded(), loadingCredentialStore.getCredentials(clientIdPrivate).getCredentials().toByteArray());
            assertArrayEquals(keyPair.getPublic().getEncoded(), loadingCredentialStore.getCredentials(clientIdPublic).getCredentials().toByteArray());
        } catch (NotAvailableException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }

        userId = "123512";
        try {
            loadingCredentialStore.getCredentials(userId);
        } catch (NotAvailableException ex) {
            return;
        }
        fail("NotAvailableException not thrown even though there is no user[" + userId + "]");

        credentialStore.shutdown();
        loadingCredentialStore.shutdown();
    }
}
