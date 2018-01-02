package org.openbase.bco.manager.location.binding.openhab.unitgroup;

/*-
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
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.unit.unitgroup.UnitGroupRemote;
import org.openbase.bco.manager.location.binding.openhab.execution.OpenHABCommandFactory;
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
import rst.domotic.unit.unitgroup.UnitGroupDataType.UnitGroupData;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupRemoteFactoryImpl implements Factory<UnitGroupRemote, UnitConfig> {
    
    private OpenHABRemote openHABRemote;

    public UnitGroupRemoteFactoryImpl() {
    }

    public void init(final OpenHABRemote openHABRemote) {
        this.openHABRemote = openHABRemote;
    }

    @Override
    public UnitGroupRemote newInstance(final UnitConfig unitGroupUnitConfig) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            UnitGroupRemote unitGroupRemote = Units.getUnit(unitGroupUnitConfig, false, Units.UNITGROUP);
            unitGroupRemote.addDataObserver((final Observable<UnitGroupData> source, UnitGroupData data) -> {
                // some locations do not have units for a given state so this state will not be set in LocationData and should not be updated in openhab
                if (data.hasColorState()) {
                    openHABRemote.postUpdate(OpenHABCommandFactory.newHSBCommand(data.getColorState().getColor().getHsbColor()).setItem(generateItemId(unitGroupUnitConfig, ServiceType.COLOR_STATE_SERVICE)).build());
                }

                if (data.hasPowerState()) {
                    openHABRemote.postUpdate(OpenHABCommandFactory.newOnOffCommand(data.getPowerState().getValue()).setItem(generateItemId(unitGroupUnitConfig, ServiceType.POWER_STATE_SERVICE)).build());
                }
            });
            return unitGroupRemote;
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(LocationRemote.class, ex);
        }
    }

    //TODO: method is implemented in the openhab config generator and should be used from there
    private String generateItemId(UnitConfig locationUnitConfig, ServiceType serviceType) throws CouldNotPerformException {
        return StringProcessor.transformToIdString("UnitGroup")
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformUpperCaseToCamelCase(serviceType.name())
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(locationUnitConfig.getScope(), ITEM_SUBSEGMENT_DELIMITER);
    }
}
