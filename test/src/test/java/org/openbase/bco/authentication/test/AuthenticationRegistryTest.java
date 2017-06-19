package org.openbase.bco.authentication.test;

/*-
 * #%L
 * BCO Authentication Test
 * %%
 * Copyright (C) 2017 openbase.org
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

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authentication.core.AuthenticationRegistry;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.bco.authentication.lib.jp.JPInitializeCredentials;
import org.openbase.bco.authentication.lib.jp.JPResetCredentials;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthenticationRegistryTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticationRegistryTest.class);

    public AuthenticationRegistryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();
        JPService.registerProperty(JPResetCredentials.class);
        JPService.registerProperty(JPInitializeCredentials.class);
        JPService.registerProperty(JPCredentialsDirectory.class);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * 
     * @throws Exception 
     */
    @Test
    public void testSavingAndLoading() throws Exception {
        System.out.println("testSavingAndLoading");

        // create initial instance which has to create a new file
        AuthenticationRegistry registry = new AuthenticationRegistry();
        registry.init();

        // add a client to this registry
        String clientId = "tamino";
        String password = "12345678";
        registry.setCredentials(clientId, EncryptionHelper.hash(password));

        // start a second registry which loads the file from the first one
        AuthenticationRegistry loadingRegistry = new AuthenticationRegistry();
        loadingRegistry.init();

        // test if they produce the same result
        try {
            byte[] passwordHash = loadingRegistry.getCredentials(clientId);
            assertArrayEquals(registry.getCredentials(clientId), passwordHash);
        } catch (NotAvailableException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }

        clientId = "123512";
        try {
            loadingRegistry.getCredentials(clientId);
        } catch (NotAvailableException ex) {
            return;
        }
        fail("NotAvailableException not thrown even though there is no user[" + clientId + "]");
    }
}
