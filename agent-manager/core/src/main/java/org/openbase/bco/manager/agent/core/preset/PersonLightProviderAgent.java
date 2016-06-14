package org.openbase.bco.manager.agent.core.preset;

/*
 * #%L
 * COMA AgentManager Core
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import org.openbase.bco.manager.agent.core.AbstractAgent;
import org.openbase.bco.manager.location.remote.LocationRemote;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.pattern.Observable;
import rst.homeautomation.state.MotionStateType.MotionState;
import rst.homeautomation.state.MotionStateType.MotionStateOrBuilder;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class PersonLightProviderAgent extends AbstractAgent {

    public static final double MINIMUM_LIGHT_THRESHOLD = 100;
    private LocationRemote locationRemote;
    private PresenseDetector presenseDetector;

    public PersonLightProviderAgent() throws InstantiationException, CouldNotPerformException, InterruptedException {
        super(false);
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getConfig().getLabel()+ "]");
        locationRemote = new LocationRemote();
        locationRemote.init(CachedLocationRegistryRemote.getRegistry().getLocationConfigById(getConfig().getLocationId()));
        locationRemote.activate();

        this.presenseDetector = new PresenseDetector();
        presenseDetector.init(locationRemote);

        this.presenseDetector.addObserver((Observable<MotionState> source, MotionState data) -> {
            notifyMotionStateChanged(data);
        });
        super.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getClass().getSimpleName() + "]");
        locationRemote.deactivate();
        presenseDetector.shutdown();
        super.deactivate();
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        presenseDetector.activate();
        locationRemote.activate();
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        presenseDetector.deactivate();
        locationRemote.deactivate();
    }

    private void notifyMotionStateChanged(final MotionStateOrBuilder motionState) throws CouldNotPerformException {
        if (motionState.getValue() == MotionState.State.MOVEMENT) {
            locationRemote.setPower(PowerState.State.ON);
        } else {
            locationRemote.setPower(PowerState.State.OFF);
        }
        logger.info("detect: " + motionState.getValue());
    }
}
