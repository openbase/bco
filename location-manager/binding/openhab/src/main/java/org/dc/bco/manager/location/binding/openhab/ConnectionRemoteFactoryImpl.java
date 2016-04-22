/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.location.binding.openhab;

import org.dc.bco.manager.location.remote.ConnectionRemote;
import org.dc.jul.exception.CouldNotPerformException;
import static org.dc.jul.extension.openhab.binding.AbstractOpenHABRemote.ITEM_SEGMENT_DELIMITER;
import static org.dc.jul.extension.openhab.binding.AbstractOpenHABRemote.ITEM_SUBSEGMENT_DELIMITER;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.pattern.Factory;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.service.ServiceTemplateType;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.ConnectionDataType.ConnectionData;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ConnectionRemoteFactoryImpl implements Factory<ConnectionRemote, ConnectionConfig> {

    @Override
    public ConnectionRemote newInstance(ConnectionConfig config) throws org.dc.jul.exception.InstantiationException, InterruptedException {
        ConnectionRemote connectionRemote = new ConnectionRemote();
        try {
            connectionRemote.addObserver(new Observer<ConnectionData>() {

                @Override
                public void update(Observable<ConnectionData> source, ConnectionData data) throws Exception {
//                    openHABRemote.postUpdate(OpenHABCommandFactory.newOnOffCommand(data.getActivationState()).setItem(generateItemId(config)).build());
                }
            });
            connectionRemote.init(config);
            connectionRemote.activate();

            return connectionRemote;
        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(connectionRemote, ex);
        }
    }
    
    private String generateItemId(ConnectionConfig connectionConfig, ServiceTemplateType.ServiceTemplate.ServiceType serviceType) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("Connection")
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformUpperCaseToCamelCase(serviceType.name())
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(connectionConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }
}
