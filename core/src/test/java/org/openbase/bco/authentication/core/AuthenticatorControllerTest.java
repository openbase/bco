package org.openbase.bco.authentication.core;

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

import java.util.List;
import java.util.concurrent.Callable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authentication.lib.AuthenticationClientHandler;
import org.openbase.bco.authentication.lib.ClientRemote;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.jp.JPAuthentificationScope;
import org.openbase.jps.core.JPService;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBListener;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rsb.Event;
import rsb.Handler;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;

/**
 *
 * @author Tamino Huxohl <thuxohl@techfak.uni-bielefel.de>
 */
public class AuthenticatorControllerTest {

    private static AuthenticatorLauncher authenticatorLauncher;

    public AuthenticatorControllerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();

        GlobalCachedExecutorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                authenticatorLauncher = new AuthenticatorLauncher();
                authenticatorLauncher.launch();
                return null;
            }
        });
    }

    @AfterClass
    public static void tearDownClass() {
        if (authenticatorLauncher != null) {
            authenticatorLauncher.shutdown();
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of init method, of class AuthenticatorController.
     */
    @Test(timeout = 5000)
    public void testCommunication() throws Exception {
        String client_password = "password";
        String client_id = "maxmustermann";
        
        RSBListener listener = RSBFactoryImpl.getInstance().createSynchronizedListener(JPService.getProperty(JPAuthentificationScope.class).getValue(), RSBSharedConnectionConfig.getParticipantConfig());
        listener.addHandler(new Handler() {
            @Override
            public void internalNotify(Event event) {
                System.out.println(event.getData());
            }
        }, true);
        listener.activate();
        
        ClientRemote clientRemote = new ClientRemote();
        clientRemote.init();
        clientRemote.activate();

        Thread.sleep(500);

        byte[] client_passwordHash = EncryptionHelper.hash(client_password);

        // handle KDC request on server side
        TicketSessionKeyWrapper slr = clientRemote.requestTGT(client_id).get();

        // handle KDC response on client side
        List<Object> list = AuthenticationClientHandler.handleKDCResponse(client_id, client_passwordHash, slr);
        TicketAuthenticatorWrapper client_at = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        byte[] client_TGSSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side

        // handle TGS request on server side
        slr = clientRemote.requestCST(client_at).get();

        // handle TGS response on client side
        list = AuthenticationClientHandler.handleTGSResponse(client_id, client_TGSSessionKey, slr);
        client_at = (TicketAuthenticatorWrapper) list.get(0); // save at somewhere temporarily
        byte[] client_SSSessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side

        // init SS request on client side
        client_at = AuthenticationClientHandler.initSSRequest(client_SSSessionKey, client_at);

        // handle SS request on server side
        TicketAuthenticatorWrapper server_at = clientRemote.validateCST(client_at).get();

        // handle SS response on client side
        client_at = AuthenticationClientHandler.handleSSResponse(client_SSSessionKey, client_at, server_at);

        clientRemote.shutdown();
    }

}
