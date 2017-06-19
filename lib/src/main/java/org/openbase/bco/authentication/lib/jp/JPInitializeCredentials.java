package org.openbase.bco.authentication.lib.jp;

import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.preset.AbstractJPBoolean;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class JPInitializeCredentials extends AbstractJPBoolean {
    
    public final static String[] COMMAND_IDENTIFIERS = {"--init"};

    public JPInitializeCredentials() {
        super(COMMAND_IDENTIFIERS);
    }

    /**
     * returns true if JPS is in test mode or JPResetDB is enabled.
     * @return
     */
    @Override
    protected Boolean getPropertyDefaultValue() {
        try {
            return JPService.testMode() || JPService.getProperty(JPResetCredentials.class).getValue();
        } catch (JPNotAvailableException ex) {
            JPService.printError("Could not load default value!", ex);
            return false;
        }
    }

    @Override
    public String getDescription() {
        return "Initialize a new instance of the credential database.";
    }
}
