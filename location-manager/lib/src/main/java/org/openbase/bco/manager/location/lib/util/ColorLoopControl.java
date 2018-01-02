package org.openbase.bco.manager.location.lib.util;

/*
 * #%L
 * BCO Manager Location Library
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.vision.HSBColorType.HSBColor;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
@Deprecated
public class ColorLoopControl {

    public final static Random random = new Random();

    private final LocationRegistryRemote locationRegistryRemote;
    private final ArrayList<HSBColor> colorList;
    private final List<ColorableLightRemote> ambientLightRemoteList;
    private final long delay;

    
    public ColorLoopControl(final String locationId, final Collection<HSBColor> colors) throws InstantiationException, InterruptedException {
        this(locationId,colors, 500);
    }
    
    public ColorLoopControl(final String locationId, final Collection<HSBColor> colors, final long delay) throws InstantiationException, InterruptedException {
        try {
            this.delay = delay;
            this.colorList = new ArrayList<>(colors);
            this.locationRegistryRemote = new LocationRegistryRemote();
            this.locationRegistryRemote.init();
            this.locationRegistryRemote.activate();
            List<UnitConfig> unitConfigs = this.locationRegistryRemote.getUnitConfigsByLocation(UnitType.COLORABLE_LIGHT, locationId);
            this.ambientLightRemoteList = new ArrayList<>();
            ColorableLightRemote ambientLightRemote;
            for (UnitConfig unitConfig : unitConfigs) {
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
                try {
                    while (!isInterrupted()) {
                        Collections.shuffle(ambientLightRemoteList);
                        for (ColorableLightRemote remote : ambientLightRemoteList) {
                            try {
                                remote.setColor(getRandomColor());
                                if(delay > 0) {
                                    Thread.sleep(delay);
                                } else {
                                    Thread.yield();
                                }
                            } catch (CouldNotPerformException ex) {
                                Logger.getLogger(ColorLoopControl.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(ColorLoopControl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }

    public HSBColor getRandomColor() {
        return colorList.get(random.nextInt(colorList.size()));
    }

}
