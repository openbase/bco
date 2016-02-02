/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.control.action;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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

import org.dc.bco.dal.lib.layer.service.ServiceType;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.control.action.ActionConfigType;
import rst.homeautomation.service.ServiceConfigType;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class Action implements ActionService {

    public enum ActionState {

        INITIATED,
        SCHEDULED,
        INITIALIZING,
        EXECUTING,
        FINISHING,
        FINISHED,
        ABORTING,
        ABORTED,
        REJECTED,
        FAILED
    }

    public enum ActionPriority {

        USER_LOW,
        USER_NORMAL,
        USER_HIGHT,
        SYSTEM_LOW,
        SYSTEM_NORMAL,
        SYSTEM_HIGH
    };

    // config
    private ActionPriority priority;
    private String origin;
    private long executionDelay;
    private long period;
    private ActionState state;

    private ActionConfigType.ActionConfig config;

    public Action(final ActionConfigType.ActionConfig config) {
        this.config = config;
    }



    @Override
    public void execute() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ServiceType getServiceType() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ServiceConfigType.ServiceConfig getServiceConfig() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
