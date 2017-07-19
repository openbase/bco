package org.openbase.bco.authentication.test;

import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.authentication.core.AuthenticatorController;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jps.core.JPService;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class InitialRegistrationTest {
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticatorControllerTest.class);

    public InitialRegistrationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    
        
    /**
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void initialRegistrationTest() throws Exception {
        LOGGER.info("initialRegistrationTest");
        
        // start an authenticator with an empty registry
        AuthenticatorController authenticator = new AuthenticatorController();
        authenticator.init();
        authenticator.activate();
        authenticator.waitForActivation();
        
        assertTrue("Initial password has not been generated despite an empty registry", authenticator.getInitialPassword() != null);
        
        // register the initial user via session manager
        String userId = "First";
        String password = "random";
        SessionManager.getInstance().initialRegistration(authenticator.getInitialPassword(), userId, password);
        
        // test if login works afterwards
        SessionManager.getInstance().login(userId, password);
        assertTrue("User is not logged in", SessionManager.getInstance().isLoggedIn());
        assertTrue("User cannot be authenticated", SessionManager.getInstance().isAuthenticated());
        
        // logout and shutdown authenticator to not interfere with other tests
        SessionManager.getInstance().logout();
        authenticator.shutdown();
    }
}
