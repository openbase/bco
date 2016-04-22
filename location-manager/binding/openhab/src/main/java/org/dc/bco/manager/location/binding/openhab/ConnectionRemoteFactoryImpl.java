/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.location.binding.openhab;

import org.dc.bco.manager.location.remote.ConnectionRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.pattern.Factory;
import rst.spatial.ConnectionConfigType.ConnectionConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ConnectionRemoteFactoryImpl implements Factory<ConnectionRemote, ConnectionConfig> {

    @Override
    public ConnectionRemote newInstance(ConnectionConfig config) throws org.dc.jul.exception.InstantiationException, InterruptedException {
        ConnectionRemote connectionRemote = new ConnectionRemote();
        try {
//            locationRemote.addObserver(new Observer<SceneData>() {
//
//                @Override
//                public void update(Observable<SceneData> source, SceneData data) throws Exception {
//                    openHABRemote.postUpdate(OpenHABCommandFactory.newOnOffCommand(data.getActivationState()).setItem(generateItemId(config)).build());
//                }
//            });
            connectionRemote.init(config);
            connectionRemote.activate();

            return connectionRemote;
        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(connectionRemote, ex);
        }
    }
}
