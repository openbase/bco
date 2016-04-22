/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.scene.binding.openhab;

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

import org.dc.bco.manager.scene.binding.openhab.execution.OpenHABCommandFactory;
import org.dc.bco.manager.scene.remote.SceneRemote;
import org.dc.jul.exception.CouldNotPerformException;
import static org.dc.jul.extension.openhab.binding.AbstractOpenHABRemote.ITEM_SEGMENT_DELIMITER;
import static org.dc.jul.extension.openhab.binding.AbstractOpenHABRemote.ITEM_SUBSEGMENT_DELIMITER;
import org.dc.jul.extension.openhab.binding.interfaces.OpenHABRemote;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.pattern.Factory;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.control.scene.SceneDataType.SceneData;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class SceneRemoteFactoryImpl implements Factory<SceneRemote, SceneConfig> {

    private OpenHABRemote openHABRemote;

    public SceneRemoteFactoryImpl() {
    }

    public void init(final OpenHABRemote openHABRemote) {
        this.openHABRemote = openHABRemote;
    }

    @Override
    public SceneRemote newInstance(SceneConfig config) throws org.dc.jul.exception.InstantiationException, InterruptedException {
        SceneRemote sceneRemote = new SceneRemote();
        try {
            sceneRemote.addObserver(new Observer<SceneData>() {

                @Override
                public void update(Observable<SceneData> source, SceneData data) throws Exception {
                    openHABRemote.postUpdate(OpenHABCommandFactory.newOnOffCommand(data.getActivationState()).setItem(generateItemId(config)).build());
                }
            });
            sceneRemote.init(config);
            sceneRemote.activate();

            return sceneRemote;
        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(sceneRemote, ex);
        }
    }

    //TODO: method is implemented in the openhab config generator and should be used from there
    private String generateItemId(SceneConfig sceneConfig) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("Scene")
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(sceneConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }
}
