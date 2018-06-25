package org.openbase.bco.app.cloud.connector.mapping.service;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 openbase.org
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

import org.openbase.bco.app.cloud.connector.mapping.lib.Named;
import org.openbase.bco.app.cloud.connector.mapping.lib.Toggle;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.MotionStateType.MotionState;
import rst.domotic.state.MotionStateType.MotionState.State;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class MotionServiceTogglesMapper extends AbstractServiceProviderTogglesTraitMapper<MotionState> {

    private final Toggle motionToggle;
    private final Named motion, noMotion;

    public MotionServiceTogglesMapper() {
        super(ServiceType.MOTION_STATE_SERVICE);

        motion = new Named("motion", "bewegung");
        noMotion = new Named("no motion", "keine bewegung");
        motionToggle = new Toggle(motion, noMotion);
    }

    @Override
    public Toggle getToggle() {
        return motionToggle;
    }

    @Override
    public boolean isOn(MotionState motionState) {
        return motionState.getValue() == State.MOTION;
    }
}
