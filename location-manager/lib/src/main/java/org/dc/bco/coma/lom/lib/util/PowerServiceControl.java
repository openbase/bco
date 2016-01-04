/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.lom.lib.util;

import de.citec.dal.remote.service.PowerServiceRemote;
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
