package org.openbase.bco.manager.user.test.remote.user;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.openbase.bco.dal.test.AbstractBCOTest;
import org.openbase.bco.manager.user.core.UserManagerLauncher;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class AbstractBCOUserManagerTest extends AbstractBCOTest {
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(UserRemoteTest.class);
    
    protected static UserManagerLauncher userManagerLauncher;
    
    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            AbstractBCOTest.setUpClass();

            userManagerLauncher = new UserManagerLauncher();
            userManagerLauncher.launch();

            Registries.waitForData();
        } catch (CouldNotPerformException | InterruptedException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        try {
            if (userManagerLauncher != null) {
                userManagerLauncher.shutdown();
            }
            AbstractBCOTest.tearDownClass();
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }
}
