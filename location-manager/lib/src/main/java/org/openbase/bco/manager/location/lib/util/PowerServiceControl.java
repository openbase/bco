package org.openbase.bco.manager.location.lib.util;

/*
 * #%L
 * BCO Manager Location Library
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import org.openbase.bco.dal.remote.service.PowerStateServiceRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class PowerServiceControl {

    protected final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

    public final static Random random = new Random();

    private final PowerStateServiceRemote powerServiceRemote;
    private final LocationRegistryRemote locationRegistryRemote;
    private final PowerState powerState;

    public PowerServiceControl(final String locationId, final PowerState.State powerState) throws InstantiationException, InterruptedException {
        try {
            this.powerState = PowerState.newBuilder().setValue(powerState).build();
            this.locationRegistryRemote = new LocationRegistryRemote();
            this.locationRegistryRemote.init();
            this.locationRegistryRemote.activate();

            List<UnitConfig> unitConfigs = this.locationRegistryRemote.getUnitConfigsByLocation(ServiceType.POWER_STATE_SERVICE, locationId);
            this.powerServiceRemote = new PowerStateServiceRemote();
            try {
                this.powerServiceRemote.init(unitConfigs);
            } catch (Exception ex) {
                ExceptionPrinter.printHistory(ex, logger);
            }
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void activate() throws InterruptedException, CouldNotPerformException {
        new Thread() {

            @Override
            public void run() {
                try {
                    powerServiceRemote.setPowerState(powerState);
                } catch (CouldNotPerformException ex) {
                    Logger.getLogger(PowerServiceControl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }
}
