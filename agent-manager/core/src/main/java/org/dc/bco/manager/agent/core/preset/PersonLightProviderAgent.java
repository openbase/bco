/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.core.preset;

/*
 * #%L
 * COMA AgentManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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

import org.dc.bco.dal.remote.service.PowerServiceRemote;
import org.dc.bco.manager.agent.core.AbstractAgent;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.pattern.Observable;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.state.MotionStateType.MotionState;
import rst.homeautomation.state.MotionStateType.MotionStateOrBuilder;
import rst.homeautomation.state.PowerStateType;
import rst.homeautomation.unit.UnitTemplateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class PersonLightProviderAgent extends AbstractAgent {

    public static final double MINIMUM_LIGHT_THRESHOLD = 100;
    private MotionStateFutionProvider motionStateProvider;
    private PowerServiceRemote powerServiceRemote;

    public PersonLightProviderAgent() throws InstantiationException, CouldNotPerformException, InterruptedException {
        super(false);
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getClass().getSimpleName() + "]");
        super.activate();

        LocationRegistryRemote locationRegistryRemote = new LocationRegistryRemote();
        locationRegistryRemote.init();
        locationRegistryRemote.activate();

        this.motionStateProvider = new MotionStateFutionProvider(locationRegistryRemote.getUnitConfigsByLocation(UnitTemplateType.UnitTemplate.UnitType.MOTION_SENSOR, getConfig().getLocationId()));

        this.motionStateProvider.addObserver((Observable<MotionState> source, MotionState data) -> {
            notifyMotionStateChanged(data);
        });

        String homeId = locationRegistryRemote.getLocationConfigsByLabel("Home").stream().findFirst().get().getId();
        powerServiceRemote = new PowerServiceRemote();
        powerServiceRemote.init(locationRegistryRemote.getUnitConfigsByLocation(ServiceTemplate.ServiceType.POWER_SERVICE, homeId));
        locationRegistryRemote.deactivate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getClass().getSimpleName() + "]");
        powerServiceRemote.deactivate();
        motionStateProvider.shutdown();

        super.deactivate();

    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        motionStateProvider.activate();
        powerServiceRemote.activate();
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        motionStateProvider.deactivate();
        powerServiceRemote.deactivate();
    }



    private void notifyMotionStateChanged(final MotionStateOrBuilder motionState) throws CouldNotPerformException {
        if (motionState.getValue() == MotionState.State.MOVEMENT) {
            powerServiceRemote.setPower(PowerStateType.PowerState.State.ON);
        } else {
            powerServiceRemote.setPower(PowerStateType.PowerState.State.OFF);
        }

        logger.info("detect: " + motionState.getValue());

    }
}
