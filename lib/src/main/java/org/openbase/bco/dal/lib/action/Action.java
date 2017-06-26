package org.openbase.bco.dal.lib.action;

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

import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Executable;
import org.openbase.jul.iface.Initializable;
import rst.domotic.action.ActionDescriptionType.ActionDescription;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface Action extends Initializable<ActionDescription>, Executable {

    /**
     * Method returns the action description of this action.
     *
     * @return the action description of this action.
     * @throws org.openbase.jul.exception.NotAvailableException is thrown if the action description is not available yet which means the Action was never initialized.
     */
    ActionDescription getActionDescription() throws NotAvailableException;
}
