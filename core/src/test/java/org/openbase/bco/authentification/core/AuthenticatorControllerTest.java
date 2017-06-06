package org.openbase.bco.authentification.core;

import java.util.List;
import java.util.concurrent.Callable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authenticator.lib.ClientRemote;
import org.openbase.bco.authenticator.lib.classes.AuthenticationHandler;
import org.openbase.bco.authenticator.lib.jp.JPAuthentificationScope;
import org.openbase.jps.core.JPService;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBListener;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rsb.Event;
import rsb.Handler;
import rst.domotic.authentification.AuthenticatorTicketType.AuthenticatorTicket;
import rst.domotic.authentification.LoginResponseType.LoginResponse;

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
        
        AuthenticationHandler instance = new AuthenticationHandler();

        ClientRemote clientRemote = new ClientRemote();
        clientRemote.init();
        clientRemote.activate();

        Thread.sleep(500);

        byte[] client_passwordHash = instance.initKDCRequest(client_password);

        // handle KDC request on server side
        LoginResponse slr = clientRemote.requestTGT(client_id).get();

        // handle KDC response on client side
        List<Object> list = instance.handleKDCResponse(client_id, client_passwordHash, slr);
        AuthenticatorTicket client_at = (AuthenticatorTicket) list.get(0); // save at somewhere temporarily
        byte[] client_TGSSessionKey = (byte[]) list.get(1); // save TGS session key somewhere on client side


        // handle TGS request on server side
        slr = clientRemote.requestCST(client_at).get();

        // handle TGS response on client side
        list = instance.handleTGSResponse(client_id, client_TGSSessionKey, slr);
        client_at = (AuthenticatorTicket) list.get(0); // save at somewhere temporarily
        byte[] client_SSSessionKey = (byte[]) list.get(1); // save SS session key somewhere on client side

        // init SS request on client side
        client_at = instance.initSSRequest(client_SSSessionKey, client_at);

        // handle SS request on server side
        AuthenticatorTicket server_at = clientRemote.validateCST(client_at).get();

        // handle SS response on client side
        client_at = instance.handleSSResponse(client_SSSessionKey, client_at, server_at);

        clientRemote.shutdown();
    }

}
