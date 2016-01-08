/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.location.lib.util;

import org.dc.bco.dal.remote.unit.AmbientLightRemote;
import org.dc.bco.dal.lib.transform.HSVColorToRGBColorTransformer;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
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

        return Executors.newSingleThreadExecutor().submit(new Callable<HSVColor>() {

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
