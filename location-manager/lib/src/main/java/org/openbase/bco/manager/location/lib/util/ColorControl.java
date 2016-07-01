package org.openbase.bco.manager.location.lib.util;

/*
 * #%L
 * COMA LocationManager Library
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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

import org.openbase.bco.dal.remote.unit.AmbientLightRemote;
import org.openbase.bco.dal.lib.transform.HSVColorToRGBColorTransformer;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openbase.jul.schedule.GlobalExecutionService;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class ColorControl {

    public final static Random random = new Random();

    private final LocationRegistryRemote locationRegistryRemote;
    private final List<AmbientLightRemote> ambientLightRemoteList;

    public ColorControl(final String locationId) throws InstantiationException, InterruptedException {
        try {
            this.locationRegistryRemote = new LocationRegistryRemote();
            this.locationRegistryRemote.init();
            this.locationRegistryRemote.activate();
            List<UnitConfig> unitConfigs = this.locationRegistryRemote.getUnitConfigsByLocation(UnitType.AMBIENT_LIGHT, locationId);
            this.ambientLightRemoteList = new ArrayList<>();
            AmbientLightRemote ambientLightRemote;
            for (UnitConfig unitConfig : unitConfigs) {
                ambientLightRemote = new AmbientLightRemote();
                ambientLightRemote.init(unitConfig);
                ambientLightRemoteList.add(ambientLightRemote);
                ambientLightRemote.activate();

            }
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public Future<HSVColor> execute(final Color color) throws InterruptedException, CouldNotPerformException {
        return execute(HSVColorToRGBColorTransformer.transform(color));
    }
    
    public Future<HSVColor> execute(final HSVColor color) throws InterruptedException, CouldNotPerformException {

        return GlobalExecutionService.submit(new Callable<HSVColor>() {

            @Override
            public HSVColor call() throws Exception {
                for (AmbientLightRemote remote : ambientLightRemoteList) {
                    try {
                        remote.setColor(color);
                    } catch (CouldNotPerformException ex) {
                        Logger.getLogger(ColorControl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                return color;
            }
        });

    }
}
