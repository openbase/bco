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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.AlarmStateType.AlarmState;
import rst.domotic.state.SmokeStateType.SmokeState;
import rst.domotic.unit.dal.SmokeDetectorDataType.SmokeDetectorData;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SmokeDetectorController extends AbstractUnitController<SmokeDetectorData, SmokeDetectorData.Builder> implements SmokeDetector {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeDetectorData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeState.getDefaultInstance()));
    }

    public SmokeDetectorController(final UnitHost unitHost, SmokeDetectorData.Builder builder) throws org.openbase.jul.exception.InstantiationException, CouldNotPerformException {
        super(SmokeDetectorController.class, unitHost, builder);
    }

    public void updateSmokeAlarmStateProvider(final AlarmState value) throws CouldNotPerformException {
        logger.debug("Apply smokeAlarmState Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<SmokeDetectorData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setSmokeAlarmState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply smokeAlarmState Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public AlarmState getSmokeAlarmState() throws NotAvailableException {
        try {
            return getData().getSmokeAlarmState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("smokeState", ex);
        }
    }

    public void updateSmokeStateProvider(final SmokeState value) throws CouldNotPerformException {
        logger.debug("Apply smokeState Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<SmokeDetectorData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setSmokeState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply smokeState Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public SmokeState getSmokeState() throws NotAvailableException {
        try {
            return getData().getSmokeState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("smokestate", ex);
        }
    }
}
