package org.openbase.bco.authentication.lib;

import java.util.concurrent.Callable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openbase.bco.authentication.core.AuthenticatorLauncher;
import org.openbase.bco.authentication.lib.jp.JPAuthenticationScope;
import org.openbase.jps.core.JPService;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBListener;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rsb.Event;
import rsb.Handler;

/**
 *
 * @author Sebastian Fast <sfast@techfak.uni-bielefeld.de>
 */
public class SessionManagerTest {

    private static AuthenticatorLauncher authenticatorLauncher;
    private RSBListener listener;
    
    public SessionManagerTest() {
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
    public void setUp() throws Exception {
        
        listener = RSBFactoryImpl.getInstance().createSynchronizedListener(JPService.getProperty(JPAuthenticationScope.class).getValue(), RSBSharedConnectionConfig.getParticipantConfig());
        listener.addHandler(new Handler() {
            @Override
            public void internalNotify(Event event) {
                System.out.println(event.getData());
            }
        }, true);
        listener.activate();
    }
    
    @After
    public void tearDown() throws Exception {
        listener.deactivate();
    }

    /**
     * Test of login method, of class SessionManager.
     */
    @Test(timeout = 5000)
    public void testLogin() throws Exception {
                
        SessionManager manager = new SessionManager();
        boolean result = manager.login("maxmustermann", "password");
        
        assertEquals(true, result);
    }

    /**
     * Test of login method, of class SessionManager.
     */
    @Test(timeout = 5000)
    public void testLogout() throws Exception {
                
        SessionManager manager = new SessionManager();
        boolean result = manager.login("maxmustermann", "password");
        
        assertEquals(true, result);
        
        manager.logout();
        assertEquals(null, manager.getClientServerTicket());
        assertEquals(null, manager.getSessionKey());
      
    }
    
}
