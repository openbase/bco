/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.location.binding.openhab;

/*
 * #%L
 * COMA SceneManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.bco.manager.location.remote.LocationRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.openhab.binding.interfaces.OpenHABRemote;
import org.dc.jul.pattern.Factory;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class LocationRemoteFactoryImpl implements Factory<LocationRemote, LocationConfig> {

//    private OpenHABRemote openHABRemote;

    public LocationRemoteFactoryImpl() {
    }

//    public void init(final OpenHABRemote openHABRemote) {
//        this.openHABRemote = openHABRemote;
//    }

    @Override
    public LocationRemote newInstance(LocationConfig config) throws org.dc.jul.exception.InstantiationException, InterruptedException {
        LocationRemote locationRemote = new LocationRemote();
        try {
//            locationRemote.addObserver(new Observer<SceneData>() {
//
//                @Override
//                public void update(Observable<SceneData> source, SceneData data) throws Exception {
//                    openHABRemote.postUpdate(OpenHABCommandFactory.newOnOffCommand(data.getActivationState()).setItem(generateItemId(config)).build());
//                }
//            });
            locationRemote.init(config);
            locationRemote.activate();

            return locationRemote;
        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(locationRemote, ex);
        }
    }

    //TODO: method is implemented in the openhab config generator and should be used from there
//    private String generateItemId(SceneConfig sceneConfig) throws CouldNotPerformException {
//        return StringProcessor.transformToIdString("Scene")
//                + ITEM_SEGMENT_DELIMITER
//                + ScopeGenerator.generateStringRepWithDelimiter(sceneConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
//    }
}
