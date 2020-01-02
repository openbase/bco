package org.openbase.bco.app.cloudconnector.mapping.service;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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

import org.openbase.bco.app.cloudconnector.mapping.lib.Named;
import org.openbase.bco.app.cloudconnector.mapping.lib.Toggle;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.StandbyStateType.StandbyState;
import org.openbase.type.domotic.state.StandbyStateType.StandbyState.State;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class StandbyStateTogglesMapper extends AbstractServiceStateTogglesMapper<StandbyState> {

    private final Toggle standbyToggle;
    private final Named running, standby;

    public StandbyStateTogglesMapper() {
        super(ServiceType.STANDBY_STATE_SERVICE);

        this.running = new Named("running", "an", "läuft");
        this.standby = new Named("standby", "standby", "aus");
        this.standbyToggle = new Toggle(standby, running);
    }

    @Override
    public Toggle getToggle() {
        return standbyToggle;
    }

    @Override
    public boolean isOn(StandbyState standbyState) {
        return standbyState.getValue() == State.STANDBY;
    }

    @Override
    public StandbyState getServiceState(boolean on) {
        if (on) {
            return StandbyState.newBuilder().setValue(State.STANDBY).build();
        } else {
            return StandbyState.newBuilder().setValue(State.RUNNING).build();
        }
    }
}
