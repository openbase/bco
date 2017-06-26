package org.openbase.bco.authentication.lib.jp;

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
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.exception.JPValidationException;
import org.openbase.jps.preset.AbstractJPInteger;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class JPRegistrationMode extends AbstractJPInteger {

    public final static String[] COMMAND_IDENTIFIERS = {"--registration-mode"};

    /**
     * This integer represents if the registration mode is off.
     */
    public static final int DEFAULT_REGISTRATION_MODE = 0;

    public JPRegistrationMode() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    protected Integer getPropertyDefaultValue() throws JPNotAvailableException {
        return 0;
    }

    @Override
    protected void validate() throws JPValidationException {    
        if (getValue() < 0 || getValue() > 30) {
            throw new JPValidationException("Timeout for registration mode greater than 30 or lower than 0 minutes");
        }

        super.validate();
    }

    @Override
    public String getDescription() {
        return "Start the authenticator in registration mode. Only in this mode new users may be registered."
                + " With this property a timeout in minutes can be specified which dicatates how long the authenticator stays in this mode. "
                + " It can range from 1 to 30 minutes.";
    }

}
