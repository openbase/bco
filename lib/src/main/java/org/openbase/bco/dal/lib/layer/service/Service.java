package org.openbase.bco.dal.lib.layer.service;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.type.domotic.mode.OperationModeType;
import org.openbase.type.domotic.state.ContactStateType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @author <a href="mailto:agatting@techfak.uni-bielefeld.de">Andreas Gatting</a>
 */
public interface Service {

    Package SERVICE_STATE_PACKAGE = ContactStateType.class.getPackage();
    Package SERVICE_MODE_PACKAGE = OperationModeType.OperationMode.class.getPackage();
    String SERVICE_LABEL = Service.class.getSimpleName();

    String RESPONSIBLE_ACTION_FIELD_NAME = "responsible_action";

    /**
     * Method returns the unit or remote instance which offers this service.
     * @return
     */
    ServiceProvider getServiceProvider();
}
