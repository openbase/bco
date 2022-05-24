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

import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.exception.JPValidationException;
import org.openbase.jps.preset.AbstractJPTime;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class JPSessionTimeout extends AbstractJPTime {

    public final static String[] COMMAND_IDENTIFIERS = {"--session-timeout"};

    public static final long DEFAULT_TIMEOUT = TimeUnit.DAYS.toMillis(7);
    public static final long DEFAULT_TEST_TIMEOUT = TimeUnit.MINUTES.toMillis(15);

    public JPSessionTimeout() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    protected Long getPropertyDefaultValue() throws JPNotAvailableException {
        if (JPService.testMode()) {
            return DEFAULT_TEST_TIMEOUT;
        }
        return DEFAULT_TIMEOUT;
    }

    @Override
    protected void validate() throws JPValidationException {
        super.validate();

        final long sessionTimeout = getValue();
        if (sessionTimeout <= 0) {
            throw new JPValidationException("SessionTimeout is negative or null[" + sessionTimeout + "]");
        }

        //TODO: should this timeout have a minimum value when not in test mode?
    }

    @Override
    public String getTimeDescription() {
        return "Set the session timeout.";
    }
}
