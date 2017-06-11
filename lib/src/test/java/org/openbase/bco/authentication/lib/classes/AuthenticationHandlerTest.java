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
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.AuthenticatorType.Authenticator;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;

/**
 *
 * @author sebastian
 */
public class AuthenticationHandlerTest {

    private String client_id;
    private String client_password;
    private byte[] client_passwordHash;
    private byte[] client_TGSSessionKey;
    private byte[] client_SSSessionKey;

    private String server_clientId;
    private String server_clientPassword;
    private byte[] server_clientPasswordHash;
    private String server_clientNetworkAddress;
    private byte[] server_TGSSessionKey;
    private byte[] server_TGSPrivateKey;
    private byte[] server_SSSessionKey;
    private byte[] server_SSPrivateKey;

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
        client_id = "maxmustermann";
        client_password = "password";
        server_clientNetworkAddress = "123.123.123.123";
        server_TGSSessionKey = EncryptionHelper.generateKey();
        server_TGSPrivateKey = EncryptionHelper.generateKey();
        server_SSSessionKey = EncryptionHelper.generateKey();
        server_SSPrivateKey = EncryptionHelper.generateKey();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of encryptObject and decryptObject method, of class
     * AuthenticationHandler.
     */
    @Test
    public void testEncryptDecryptObject() throws Exception {
        System.out.println("encrypt and decrypt object");

        Authenticator.Builder ab = Authenticator.newBuilder();
        ab.setClientId(client_id);
        Authenticator authenticator = ab.build();

        byte[] key = EncryptionHelper.generateKey();

        ByteString encrypted = EncryptionHelper.encrypt(authenticator, key);
        Authenticator decrypted = (Authenticator) EncryptionHelper.decrypt(encrypted, key);

        assertEquals(authenticator.getClientId(), decrypted.getClientId());
    }

    /**
     * Test a full authentification cycle
     *
     * @throws Exception
     */
    @Test
    public void testAuthentification() throws Exception {
        System.out.println("TestAuthentification");
        AuthenticationServerHandlerImpl serverHandler = new AuthenticationServerHandlerImpl();
        AuthenticationClientHandlerImpl clientHandler = new AuthenticationClientHandlerImpl();

        // init KDC request on client side
        client_passwordHash = EncryptionHelper.hash(client_password);

        // handle KDC request on server side
        server_clientId = client_id;
        TicketSessionKeyWrapper slr = serverHandler.handleKDCRequest(server_clientId, server_clientNetworkAddress, server_TGSSessionKey, server_TGSPrivateKey);

        // handle KDC response on client side
        List<Object> list = clientHandler.handleKDCResponse(client_id, client_passwordHash, slr);
        TicketAuthenticatorWrapper client_at = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        client_TGSSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

        Assert.assertArrayEquals(server_TGSSessionKey, client_TGSSessionKey);

        // handle TGS request on server side
        slr = serverHandler.handleTGSRequest(server_TGSSessionKey, server_TGSPrivateKey, server_SSSessionKey, server_SSPrivateKey, client_at);

        // handle TGS response on client side
        list = clientHandler.handleTGSResponse(client_id, client_TGSSessionKey, slr);
        client_at = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        client_SSSessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side

        Assert.assertArrayEquals(server_SSSessionKey, client_SSSessionKey);

        // init SS request on client side
        client_at = clientHandler.initSSRequest(client_SSSessionKey, client_at);

        // handle SS request on server side
        TicketAuthenticatorWrapper server_at = serverHandler.handleSSRequest(server_SSSessionKey, server_SSPrivateKey, client_at);

        // handle SS response on client side
        client_at = clientHandler.handleSSResponse(client_SSSessionKey, client_at, server_at);
    }
}
