package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.openbase.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BatteryStateType.BatteryState;
import rst.domotic.unit.dal.BatteryDataType.BatteryData;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BatteryController extends AbstractDALUnitController<BatteryData, BatteryData.Builder> implements Battery {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BatteryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BatteryState.getDefaultInstance()));
    }

    public BatteryController(final UnitHost unitHost, BatteryData.Builder builder) throws InstantiationException {
        super(BatteryController.class, unitHost, builder);
    }

    @Override
    protected void applyCustomDataUpdate(BatteryData.Builder internalBuilder, ServiceType serviceType) {
        switch (serviceType) {
            case BATTERY_STATE_SERVICE:
                if (!internalBuilder.getBatteryState().hasValue() || internalBuilder.getBatteryState().getValue() == BatteryState.State.UNKNOWN) {
                    if (internalBuilder.getBatteryState().getLevel() <= 5) {
                        internalBuilder.getBatteryStateBuilder().setValue(BatteryState.State.INSUFFICIENT);
                    } else if (internalBuilder.getBatteryState().getLevel() <= 15) {
                        internalBuilder.getBatteryStateBuilder().setValue(BatteryState.State.CRITICAL);
                    } else {
                        internalBuilder.getBatteryStateBuilder().setValue(BatteryState.State.OK);
                    }
                }
                break;
        }
    }
}
