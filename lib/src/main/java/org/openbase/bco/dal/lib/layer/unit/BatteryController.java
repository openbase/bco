package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.BatteryStateType.BatteryState;
import rst.domotic.unit.dal.BatteryDataType.BatteryData;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BatteryController extends AbstractDALUnitController<BatteryData, BatteryData.Builder> implements Battery {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BatteryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BatteryState.getDefaultInstance()));
    }

    public BatteryController(final UnitHost unitHost, BatteryData.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(BatteryController.class, unitHost, builder);
    }

    public void updateBatteryStateProvider(final BatteryState batteryState) throws CouldNotPerformException {
        logger.debug("Apply batteryState Update[" + batteryState + "] for " + this + ".");

        try (ClosableDataBuilder<BatteryData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setBatteryState(batteryState);
            if (!batteryState.hasValue() || batteryState.getValue() == BatteryState.State.UNKNOWN) {
                if (batteryState.getLevel() <= 5) {
                    dataBuilder.getInternalBuilder().getBatteryStateBuilder().setValue(BatteryState.State.INSUFFICIENT);
                } else if (batteryState.getLevel() <= 15) {
                    dataBuilder.getInternalBuilder().getBatteryStateBuilder().setValue(BatteryState.State.CRITICAL);
                } else {
                    dataBuilder.getInternalBuilder().getBatteryStateBuilder().setValue(BatteryState.State.OK);
                }
            }
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply batteryState Update[" + batteryState + "] for " + this + "!", ex);
        }
    }

    @Override
    public BatteryState getBatteryState() throws NotAvailableException {
        try {
            return getData().getBatteryState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("batteryState", ex);
        }
    }
}
