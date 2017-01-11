package org.openbase.bco.manager.location.binding.openhab;

/*
 * #%L
 * BCO Manager Location Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import org.openbase.bco.manager.location.binding.openhab.execution.OpenHABCommandFactory;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import static org.openbase.jul.extension.openhab.binding.AbstractOpenHABRemote.ITEM_SEGMENT_DELIMITER;
import static org.openbase.jul.extension.openhab.binding.AbstractOpenHABRemote.ITEM_SUBSEGMENT_DELIMITER;
import org.openbase.jul.extension.openhab.binding.interfaces.OpenHABRemote;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.pattern.Factory;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.location.LocationDataType.LocationData;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class LocationRemoteFactoryImpl implements Factory<LocationRemote, UnitConfig> {

    private OpenHABRemote openHABRemote;

    public LocationRemoteFactoryImpl() {
    }

    public void init(final OpenHABRemote openHABRemote) {
        this.openHABRemote = openHABRemote;
    }

    @Override
    public LocationRemote newInstance(final UnitConfig locationUnitConfig) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        LocationRemote locationRemote = new LocationRemote();
        try {
            locationRemote.addDataObserver((final Observable<LocationData> source, LocationData data) -> {
                openHABRemote.postUpdate(OpenHABCommandFactory.newHSBCommand(data.getColorState().getColor().getHsbColor()).setItem(generateItemId(locationUnitConfig, ServiceType.COLOR_STATE_SERVICE)).build());
                openHABRemote.postUpdate(OpenHABCommandFactory.newOnOffCommand(data.getPowerState().getValue()).setItem(generateItemId(locationUnitConfig, ServiceType.POWER_STATE_SERVICE)).build());
                openHABRemote.postUpdate(OpenHABCommandFactory.newDecimalCommand(data.getPowerConsumptionState().getConsumption()).setItem(generateItemId(locationUnitConfig, ServiceType.POWER_CONSUMPTION_STATE_SERVICE)).build());
            });
            locationRemote.init(locationUnitConfig);
            locationRemote.activate();

            return locationRemote;
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(locationRemote, ex);
        }
    }

    //TODO: method is implemented in the openhab config generator and should be used from there
    private String generateItemId(UnitConfig locationUnitConfig, ServiceType serviceType) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("Location")
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformUpperCaseToCamelCase(serviceType.name())
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(locationUnitConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }
}
