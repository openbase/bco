/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.lom.lib.util;

import org.dc.bco.dal.remote.unit.AmbientLightRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.vision.HSVColorType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class ColorLoopControl {

    public final static Random random = new Random();

    private final LocationRegistryRemote locationRegistryRemote;
    private final ArrayList<HSVColor> colorList;
    private final List<AmbientLightRemote> ambientLightRemoteList;
    private final long delay;

    
    public ColorLoopControl(final String locationId, final Collection<HSVColorType.HSVColor> colors) throws InstantiationException, InterruptedException {
        this(locationId,colors, 500);
    }
    
    public ColorLoopControl(final String locationId, final Collection<HSVColorType.HSVColor> colors, final long delay) throws InstantiationException, InterruptedException {
        try {
            this.delay = delay;
            this.colorList = new ArrayList<>(colors);
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
                try {
                    while (!isInterrupted()) {
                        Collections.shuffle(ambientLightRemoteList);
                        for (AmbientLightRemote remote : ambientLightRemoteList) {
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

    public HSVColor getRandomColor() {
        return colorList.get(random.nextInt(colorList.size()));
    }

}
