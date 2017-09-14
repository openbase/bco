package org.openbase.bco.dal.lib.jp;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.openbase.jps.preset.AbstractJPBoolean;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class JPResourceAllocation extends AbstractJPBoolean {

    public final static String[] COMMAND_IDENTIFIERS = {"--resource-allocation"};

    public JPResourceAllocation() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    protected Boolean getPropertyDefaultValue() throws JPNotAvailableException {
        return false;
    }
    
    @Override
    public String getDescription() {
        return "Enable the usage of the resource allocation, by this a allocation server is needed for proper bco operation.";
    }
}
