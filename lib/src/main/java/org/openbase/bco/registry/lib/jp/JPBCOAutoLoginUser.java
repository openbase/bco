package org.openbase.bco.registry.lib.jp;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.exception.JPValidationException;
import org.openbase.jps.preset.AbstractJPString;
import org.openbase.jps.tools.FileHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class JPBCOAutoLoginUser extends AbstractJPString {

    private static Logger LOGGER = LoggerFactory.getLogger(JPBCOAutoLoginUser.class);

    public static final String DEFAULT_DB_PATH = "registry/db";
    public static final String[] COMMAND_IDENTIFIERS = {"--user"};

    public static final FileHandler.ExistenceHandling EXISTENCE_HANDLING = FileHandler.ExistenceHandling.Must;
    public static final FileHandler.AutoMode AUTO_MODE = FileHandler.AutoMode.On;

    public static final String OTHER = "Other";

    public JPBCOAutoLoginUser() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    public void validate() throws JPValidationException {
        super.validate();

        if(getValue() == OTHER) {
            // other rights are applied by default, so no action necessary.
            return;
        }
    }

    @Override
    protected String getPropertyDefaultValue() throws JPNotAvailableException {
        return OTHER;
    }

    @Override
    public String getDescription() {
        return "Specifies the bco user which is used for the auto login routine. The user can be identified via its id or username. As requirement the password of the user must be provided by the local credential store.";
    }
}
