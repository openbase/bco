/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.location.lib.util;

import org.dc.bco.dal.remote.unit.AmbientLightRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class PowerControl {

    public final static Random random = new Random();

    private final LocationRegistryRemote locationRegistryRemote;
    private final List<AmbientLightRemote> ambientLightRemoteList;
    private final PowerState.State powerState;

    public PowerControl(final String locationId, final PowerState.State powerState) throws InstantiationException, InterruptedException {
        try {
            this.powerState = powerState;
            this.locationRegistryRemote = new LocationRegistryRemote();
            this.locationRegistryRemote.init();
            this.locationRegistryRemote.activate();
            List<UnitConfig> unitConfigs = this.locationRegistryRemote.getUnitConfigsByLocation(UnitType.AMBIENT_LIGHT, locationId);
            this.ambientLightRemoteList = new ArrayList<>();
            AmbientLightRemote ambientLightRemote;
            for (UnitConfig unitConfig : unitConfigs) {
//                if(!unitConfig.getTemplate().getServiceTypeList().contains(ServiceTypeHolderType.ServiceTypeHolder.ServiceType.POWER_SERVICE)) {
//                    continue;
//                }
                
                ambientLightRemote = new AmbientLightRemote();
                ambientLightRemote.init(unitConfig);
                ambientLightRemoteList.add(ambientLightRemote);
            }
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void activate() throws InterruptedException, CouldNotPerformException {
        for (AmbientLightRemote remote : ambientLightRemoteList) {
            remote.activate();
        }
        new Thread() {

            @Override
            public void run() {
                    for (AmbientLightRemote remote : ambientLightRemoteList) {
                        try {
                            remote.setColor(Color.BLACK);
                            remote.callMethodAsync("setPower", PowerState.newBuilder().setValue(powerState).build());
//                            remote.setPower(powerState);
                        } catch (CouldNotPerformException ex) {
                            Logger.getLogger(PowerControl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
            }
        }.start();
    }
}
