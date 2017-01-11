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

import org.openbase.bco.dal.remote.unit.ColorableLightRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class PowerControl {

    public final static Random random = new Random();

    private final LocationRegistryRemote locationRegistryRemote;
    private final List<ColorableLightRemote> ambientLightRemoteList;
    private final PowerState.State powerState;

    public PowerControl(final String locationId, final PowerState.State powerState) throws InstantiationException, InterruptedException {
        try {
            this.powerState = powerState;
            this.locationRegistryRemote = new LocationRegistryRemote();
            this.locationRegistryRemote.init();
            this.locationRegistryRemote.activate();
            List<UnitConfig> unitConfigs = this.locationRegistryRemote.getUnitConfigsByLocation(UnitType.COLORABLE_LIGHT, locationId);
            this.ambientLightRemoteList = new ArrayList<>();
            ColorableLightRemote ambientLightRemote;
            for (UnitConfig unitConfig : unitConfigs) {
//                if(!unitConfig.getTemplate().getServiceTypeList().contains(ServiceTypeHolderType.ServiceTypeHolder.ServiceType.POWER_SERVICE)) {
//                    continue;
//                }
                
                ambientLightRemote = new ColorableLightRemote();
                ambientLightRemote.init(unitConfig);
                ambientLightRemoteList.add(ambientLightRemote);
            }
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void activate() throws InterruptedException, CouldNotPerformException {
        for (ColorableLightRemote remote : ambientLightRemoteList) {
            remote.activate();
        }
        new Thread() {

            @Override
            public void run() {
                    for (ColorableLightRemote remote : ambientLightRemoteList) {
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
