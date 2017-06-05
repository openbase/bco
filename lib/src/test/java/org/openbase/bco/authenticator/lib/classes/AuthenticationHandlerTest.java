/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.authenticator.lib.classes;

/*-
 * #%L
 * BCO Authentification Library
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.RejectedException;
import rst.domotic.authentification.AuthenticatorTicketType.AuthenticatorTicket;
import rst.domotic.authentification.AuthenticatorType.Authenticator;
import rst.domotic.authentification.LoginResponseType.LoginResponse;

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
        server_TGSSessionKey = SessionKey.generateKey();
        server_TGSPrivateKey = SessionKey.generateKey();
        server_SSSessionKey = SessionKey.generateKey();
        server_SSPrivateKey = SessionKey.generateKey();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of encryptObject and decryptObject method, of class
     * AuthenticationHandler.
     */
    @Test
    public void testEncryptDecryptObject() {
        try {
            System.out.println("encrypt and decrypt object");

            Authenticator.Builder ab = Authenticator.newBuilder();
            ab.setClientId(client_id);
            Authenticator authenticator = ab.build();

            AuthenticationHandler instance = new AuthenticationHandler();

            byte[] key = SessionKey.generateKey();

            ByteString encrypted = instance.encryptObject(authenticator, key);
            Authenticator decrypted = (Authenticator) instance.decryptObject(encrypted, key);

            assertEquals(authenticator.getClientId(), decrypted.getClientId());
        } catch (Exception ex) {
            Logger.getLogger(AuthenticationHandlerTest.class.getName()).log(Level.SEVERE, null, ex);
            fail("Exception thrown");
        }
    }

    /**
     * Test a full authentification cycle
     *
     * @throws Exception
     */
    @Test
    public void testAuthentification() throws Exception {
        System.out.println("TestAuthentification");
        AuthenticationHandler instance = new AuthenticationHandler();

        // init KDC request on client side
        client_passwordHash = instance.initKDCRequest(client_password);

        // handle KDC request on server side
        server_clientId = client_id;
        LoginResponse slr = instance.handleKDCRequest(server_clientId, server_clientNetworkAddress, server_TGSSessionKey, server_TGSPrivateKey);

        // handle KDC response on client side
        List<Object> list = instance.handleKDCResponse(client_id, client_passwordHash, slr);
        AuthenticatorTicket client_at = (AuthenticatorTicket) list.get(0); // save at somewhere temporarily
        client_TGSSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

        Assert.assertArrayEquals(server_TGSSessionKey, client_TGSSessionKey);

        // handle TGS request on server side
        slr = instance.handleTGSRequest(server_TGSSessionKey, server_TGSPrivateKey, server_SSSessionKey, server_SSPrivateKey, client_at);

        // handle TGS response on client side
        list = instance.handleTGSResponse(client_id, client_TGSSessionKey, slr);
        client_at = (AuthenticatorTicket) list.get(0); // save at somewhere temporarily
        client_SSSessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side

        Assert.assertArrayEquals(server_SSSessionKey, client_SSSessionKey);

        // init SS request on client side
        client_at = instance.initSSRequest(client_SSSessionKey, client_at);

        // handle SS request on server side
        AuthenticatorTicket server_at = instance.handleSSRequest(server_SSSessionKey, server_SSPrivateKey, client_at);

        // handle SS response on client side
        client_at = instance.handleSSResponse(client_SSSessionKey, client_at, server_at);
    }
}
