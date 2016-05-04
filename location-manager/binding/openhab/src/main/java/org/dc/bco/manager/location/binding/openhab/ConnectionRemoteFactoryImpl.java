package org.dc.bco.manager.location.binding.openhab;

/*
 * #%L
 * COMA LocationManager Binding OpenHAB
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
                public void update(final Observable<ConnectionData> source, ConnectionData data) throws Exception {
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
