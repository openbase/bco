package org.openbase.bco.app.cloudconnector.mapping.unit;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import com.google.gson.JsonObject;
import com.google.protobuf.Message;
import org.openbase.bco.app.cloudconnector.FulfillmentHandler;
import org.openbase.bco.app.cloudconnector.mapping.lib.ErrorCode;
import org.openbase.bco.app.cloudconnector.mapping.lib.Trait;
import org.openbase.bco.app.cloudconnector.mapping.service.ServiceStateTraitMapper;
import org.openbase.bco.app.cloudconnector.mapping.service.ServiceStateTraitMapperFactory;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DefaultUnitDataMapper<UR extends UnitRemote<?>> implements UnitDataMapper<UR> {

    @SuppressWarnings("unchecked")
    @Override
    public void map(UR unitRemote, JsonObject deviceState) {
        try {
            final UnitType unitType;
            if (unitRemote.getUnitType() == UnitType.UNIT_GROUP) {
                unitType = unitRemote.getConfig().getUnitGroupConfig().getUnitType();
            } else {
                unitType = unitRemote.getUnitType();
            }
            final UnitTypeMapping unitTypeMapping = UnitTypeMapping.getByUnitType(unitType);

            for (final Trait trait : unitTypeMapping.getTraitSet()) {
                final ServiceType serviceType = unitTypeMapping.getServiceType(trait);

                try {
                    final Message serviceState = Services.invokeProviderServiceMethod(serviceType, unitRemote);
                    final ServiceStateTraitMapper serviceStateTraitMapper = ServiceStateTraitMapperFactory.getInstance().getServiceStateMapper(serviceType, trait);
                    serviceStateTraitMapper.map(serviceState, deviceState);
                } catch (CouldNotPerformException ex) {
                    // getting service value or resolving mapper failed
                    FulfillmentHandler.setError(deviceState, ex, ErrorCode.UNKNOWN_ERROR);
                }
            }
        } catch (NotAvailableException ex) {
            // getUnitTemplate or get ServiceTypeTraitMapper failed
            FulfillmentHandler.setError(deviceState, ex, ErrorCode.UNKNOWN_ERROR);
        }
    }
}
