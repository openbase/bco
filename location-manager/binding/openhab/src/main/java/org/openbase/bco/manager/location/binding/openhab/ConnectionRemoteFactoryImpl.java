package org.openbase.bco.manager.location.binding.openhab;

/*
 * #%L
 * BCO Manager Location Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import static org.openbase.jul.extension.openhab.binding.AbstractOpenHABRemote.ITEM_SEGMENT_DELIMITER;
import static org.openbase.jul.extension.openhab.binding.AbstractOpenHABRemote.ITEM_SUBSEGMENT_DELIMITER;
import org.openbase.jul.extension.openhab.binding.interfaces.OpenHABRemote;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.pattern.Factory;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.connection.ConnectionDataType.ConnectionData;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ConnectionRemoteFactoryImpl implements Factory<ConnectionRemote, UnitConfig> {

    private OpenHABRemote openHABRemote;

    public ConnectionRemoteFactoryImpl() {
    }

    public void init(final OpenHABRemote openHABRemote) {
        this.openHABRemote = openHABRemote;
    }

    @Override
    public ConnectionRemote newInstance(UnitConfig config) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            ConnectionRemote connectionRemote = Units.getUnit(config, false, Units.CONNECTION);
            connectionRemote.addDataObserver((final Observable<ConnectionData> source, ConnectionData data) -> {
                //todo: implement
//                 openHABRemote.postUpdate(OpenHABCommandFactory.newOnOffCommand(data.getPowerState().getValue()).setItem(generateItemId(locationUnitConfig, ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE)).build());
            });
            return connectionRemote;
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(ConnectionRemote.class, ex);
        }
    }

    private String generateItemId(final UnitConfig connectionUnitConfig, ServiceTemplateType.ServiceTemplate.ServiceType serviceType) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("Connection")
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformUpperCaseToCamelCase(serviceType.name())
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(connectionUnitConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }
}
