package org.openbase.bco.authentication.lib.jp;

import java.io.IOException;
import java.util.List;
import org.openbase.jps.core.AbstractJavaProperty;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPBadArgumentException;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.exception.JPValidationException;
import org.openbase.jps.preset.AbstractJPBoolean;
import org.openbase.jps.preset.JPTestMode;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class JPResetCredentials extends AbstractJPBoolean {
    
    public final static String[] COMMAND_IDENTIFIERS = {"--reset"};

    public JPResetCredentials() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    protected Boolean getPropertyDefaultValue() {
        return false;
    }

    @Override
    public void validate() throws JPValidationException {
        super.validate();
        if (getValueType().equals((AbstractJavaProperty.ValueType.CommandLine))) {
            logger.warn("WARNING: OVERWRITING CURRENT CREDENTIALS!!!");
            try {
                if (JPService.getProperty(JPTestMode.class).getValue()) {
                    return;
                }
            } catch (JPServiceException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access java property!", ex), logger);
            }

            logger.warn("=== Type y and press enter to contine ===");
            try {
                if (!(System.in.read() == 'y')) {
                    throw new JPValidationException("Execution aborted by user!");
                }
            } catch (IOException ex) {
                throw new JPValidationException("Validation failed because of invalid input state!", ex);
            }
        }
    }

    @Override
    protected Boolean parse(List<String> arguments) throws JPBadArgumentException {
        return super.parse(arguments);
    }

    @Override
    public String getDescription() {
        return "Reset the internal credentials.";
    }
}
