package org.openbase.bco.dal.remote.printer.jp;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.dal.remote.printer.jp.JPLogFormat.LogFormat;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.preset.AbstractJPEnum;

public class JPLogFormat extends AbstractJPEnum<LogFormat> {

    /**
     * The valid values.
     */
    public enum LogFormat {
        PROLOG,
        PROLOG_DISCRETE_VALUES_ONLY,
        HUMAN_READABLE
    }

    /**
     * Command line argument strings.
     */
    public static final String[] COMMAND_IDENTIFIERS = {"--log-format", "--print-format"};

    /**
     * Constructor for the JPPrintFormat class.
     */
    public JPLogFormat() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    protected LogFormat getPropertyDefaultValue() throws JPNotAvailableException {
        return LogFormat.HUMAN_READABLE;
    }

    @Override
    public String getDescription() {
        return "Defines the format used to print the logs";
    }
}
