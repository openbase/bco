package org.openbase.bco.authentication.lib.classes;

/*-
 * #%L
 * BCO Authentication Library
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
import com.google.protobuf.ByteString;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openbase.bco.authentication.lib.AuthenticationClientHandlerImpl;
import org.openbase.bco.authentication.lib.AuthenticationServerHandlerImpl;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.AuthenticatorType.Authenticator;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;

/**
 *
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
 */
public class AuthenticationHandlerTest {

    private String clientId;
    private String clientPassword;
    private byte[] clientPasswordHash;
    private byte[] clientTGSSessionKey;
    private byte[] clientSSSessionKey;

    private String serverClientId;
    private String serverClientNetworkAddress;
    private byte[] serverTGSSessionKey;
    private byte[] serverTGSPrivateKey;
    private byte[] serverSSSessionKey;
    private byte[] serverSSPrivateKey;

    public AuthenticationHandlerTest() {

    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        clientId = "maxmustermann";
        clientPassword = "password";
        serverClientNetworkAddress = "123.123.123.123";
        serverTGSSessionKey = EncryptionHelper.generateKey();
        serverTGSPrivateKey = EncryptionHelper.generateKey();
        serverSSSessionKey = EncryptionHelper.generateKey();
        serverSSPrivateKey = EncryptionHelper.generateKey();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of encryptObject and decryptObject method, of class
     * AuthenticationHandler.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testEncryptDecryptObject() throws Exception {
        System.out.println("testEncryptDecryptObject");

        Authenticator.Builder authenticatorBuilder = Authenticator.newBuilder();
        authenticatorBuilder.setClientId(clientId);
        Authenticator authenticator = authenticatorBuilder.build();

        byte[] key = EncryptionHelper.generateKey();

        ByteString encrypted = EncryptionHelper.encrypt(authenticator, key);
        Authenticator decrypted = (Authenticator) EncryptionHelper.decrypt(encrypted, key);

        assertEquals(authenticator.getClientId(), decrypted.getClientId());
    }

    /**
     * Test a full authentication cycle
     *
     * @throws Exception
     */
    @Test
    public void testAuthentication() throws Exception {
        System.out.println("TestAuthentication");
        AuthenticationServerHandlerImpl serverHandler = new AuthenticationServerHandlerImpl();
        AuthenticationClientHandlerImpl clientHandler = new AuthenticationClientHandlerImpl();

        // init KDC request on client side
        clientPasswordHash = EncryptionHelper.hash(clientPassword);

        // handle KDC request on server side
        serverClientId = clientId;
        TicketSessionKeyWrapper ticketSessionKeyWrapper = serverHandler.handleKDCRequest(serverClientId, clientPasswordHash, serverClientNetworkAddress, serverTGSSessionKey, serverTGSPrivateKey);

        // handle KDC response on client side
        List<Object> list = clientHandler.handleKDCResponse(clientId, clientPasswordHash, ticketSessionKeyWrapper);
        TicketAuthenticatorWrapper clientTicketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        clientTGSSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

        Assert.assertArrayEquals(serverTGSSessionKey, clientTGSSessionKey);

        // handle TGS request on server side
        ticketSessionKeyWrapper = serverHandler.handleTGSRequest(serverTGSSessionKey, serverTGSPrivateKey, serverSSSessionKey, serverSSPrivateKey, clientTicketAuthenticatorWrapper);

        // handle TGS response on client side
        list = clientHandler.handleTGSResponse(clientId, clientTGSSessionKey, ticketSessionKeyWrapper);
        clientTicketAuthenticatorWrapper = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        clientSSSessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side

        Assert.assertArrayEquals(serverSSSessionKey, clientSSSessionKey);

        // init SS request on client side
        clientTicketAuthenticatorWrapper = clientHandler.initSSRequest(clientSSSessionKey, clientTicketAuthenticatorWrapper);

        // handle SS request on server side
        TicketAuthenticatorWrapper serverTicketAuthenticatorWrapper = serverHandler.handleSSRequest(serverSSSessionKey, serverSSPrivateKey, clientTicketAuthenticatorWrapper);

        // handle SS response on client side
        clientHandler.handleSSResponse(clientSSSessionKey, clientTicketAuthenticatorWrapper, serverTicketAuthenticatorWrapper);
    }

    // TODO: activate when working
    //@Test
    public void testAuthenticationWithNonExistentUser() throws Exception {
        System.out.println("testAuthenticationWithNonExistentUser");

        String nonExistentClientId = "NonExistentClientId";
        clientPasswordHash = EncryptionHelper.hash(clientPassword);
        AuthenticationServerHandlerImpl serverHandler = new AuthenticationServerHandlerImpl();

        try {
            serverHandler.handleKDCRequest(nonExistentClientId, clientPasswordHash, serverClientNetworkAddress, serverTGSSessionKey, serverTGSPrivateKey);
        } catch (NotAvailableException ex) {
            return;
        }
        fail("NotAvailableException has not been thrown even though there should be no user[" + nonExistentClientId + "] in the database!");
    }

    // TODO: activate when working
    //@Test
    public void testAuthenticationWithIncorrectPassword() throws Exception {
        System.out.println("testAuthenticationWithIncorrectPassword");

        System.out.println("testAuthenticationWithNonExistentUser");

        String clientId = "NonExistentClientId";
        clientPasswordHash = EncryptionHelper.hash("wrong passwd");

        AuthenticationServerHandlerImpl serverHandler = new AuthenticationServerHandlerImpl();
        AuthenticationClientHandlerImpl clientHandler = new AuthenticationClientHandlerImpl();

        TicketSessionKeyWrapper ticketSessionKeyWrapper = serverHandler.handleKDCRequest(clientId, clientPasswordHash, serverClientNetworkAddress, serverTGSSessionKey, serverTGSPrivateKey);
        // TODO: handle exception when thrown
//        try {
        List<Object> list = clientHandler.handleKDCResponse(clientId, clientPasswordHash, ticketSessionKeyWrapper);
//        } catch (NotAvailableException ex) {
//            return;
//        }
        fail("NotAvailableException has not been thrown even though there should be no user[" + clientId + "] in the database!");
    }
}
