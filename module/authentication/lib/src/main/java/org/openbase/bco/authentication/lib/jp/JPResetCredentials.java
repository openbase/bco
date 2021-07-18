package org.openbase.bco.authentication.lib.jp;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2021 openbase.org
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
import java.io.IOException;
import org.openbase.jps.core.AbstractJavaProperty;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.exception.JPValidationException;
import org.openbase.jps.preset.AbstractJPBoolean;
import org.openbase.jps.preset.JPReset;
import org.openbase.jps.preset.JPTestMode;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class JPResetCredentials extends AbstractJPBoolean {

    public final static String[] COMMAND_IDENTIFIERS = {"--reset-credentials"};

    public JPResetCredentials() {
        super(COMMAND_IDENTIFIERS);
        registerDependingProperty(JPTestMode.class);
    }

    @Override
    protected Boolean getPropertyDefaultValue() {
        try {
            return JPService.getProperty(JPReset.class).getValue();
        } catch (JPNotAvailableException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not access JPReset property", ex), logger);
            return false;
        }
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

            logger.warn("=== Type y and press enter to continue ===");
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
    public String getDescription() {
        return "Reset the internal credentials.";
    }
}
