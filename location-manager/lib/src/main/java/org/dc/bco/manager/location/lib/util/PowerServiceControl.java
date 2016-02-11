/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.location.lib.util;

/*
 * #%L
 * COMA LocationManager Library
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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

import org.dc.bco.dal.remote.service.PowerServiceRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.InstantiationException;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class PowerServiceControl {

    protected final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

    public final static Random random = new Random();

    private final PowerServiceRemote powerServiceRemote;
    private final LocationRegistryRemote locationRegistryRemote;
    private final PowerState.State powerState;

    public PowerServiceControl(final String locationId, final PowerState.State powerState) throws InstantiationException, InterruptedException {
        try {
            this.powerState = powerState;
            this.locationRegistryRemote = new LocationRegistryRemote();
            this.locationRegistryRemote.init();
            this.locationRegistryRemote.activate();

            List<UnitConfig> unitConfigs = this.locationRegistryRemote.getUnitConfigsByLocation(ServiceType.POWER_SERVICE, locationId);
            this.powerServiceRemote = new PowerServiceRemote();
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
                    powerServiceRemote.setPower(powerState);
                } catch (CouldNotPerformException ex) {
                    Logger.getLogger(PowerServiceControl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }
}
