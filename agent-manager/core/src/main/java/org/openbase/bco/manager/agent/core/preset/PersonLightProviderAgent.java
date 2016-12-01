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
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observable;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.PresenceStateType.PresenceStateOrBuilder;
import rst.domotic.unit.location.LocationDataType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PersonLightProviderAgent extends AbstractAgent {

    public static final double MINIMUM_LIGHT_THRESHOLD = 100;
    private LocationRemote locationRemote;

    public PersonLightProviderAgent() throws InstantiationException, CouldNotPerformException, InterruptedException {
        super();
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getConfig().getLabel() + "]");
        locationRemote = new LocationRemote();
        Registries.getLocationRegistry().waitForData();
        locationRemote.init(Registries.getLocationRegistry().getLocationConfigById(getConfig().getId()));
        locationRemote.activate();
        locationRemote.addDataObserver((Observable<LocationDataType.LocationData> source, LocationDataType.LocationData data) -> {
            try {
                notifyPresenceStateChanged(data.getPresenceState());
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not notify presence state change!", ex), logger);
            }
        });
        super.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getClass().getSimpleName() + "]");
        locationRemote.deactivate();
        super.deactivate();
    }

    @Override
    protected void execute() throws CouldNotPerformException, InterruptedException {
        locationRemote.activate();
    }

    @Override
    protected void stop() throws CouldNotPerformException, InterruptedException {
        locationRemote.deactivate();
    }

    private void notifyPresenceStateChanged(final PresenceStateOrBuilder presenceState) throws CouldNotPerformException {
        if (presenceState.getValue() == PresenceState.State.PRESENT) {
            locationRemote.setPowerState(PowerState.State.ON);
        } else {
            locationRemote.setPowerState(PowerState.State.OFF);
        }
        logger.info("detect: " + presenceState.getValue());
    }
}
