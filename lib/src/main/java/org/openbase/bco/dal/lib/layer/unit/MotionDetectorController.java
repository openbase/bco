package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import rst.homeautomation.state.MotionStateType.MotionState;
import rst.homeautomation.unit.MotionDetectorDataType.MotionDetectorData;
import rst.timing.TimestampType;

/**
 *
 * @author mpohling
 */
public class MotionDetectorController extends AbstractUnitController<MotionDetectorData, MotionDetectorData.Builder> implements MotionDetectorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionDetectorData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionState.getDefaultInstance()));
    }

    public MotionDetectorController(final UnitHost unitHost, final MotionDetectorData.Builder builder) throws org.openbase.jul.exception.InstantiationException, CouldNotPerformException {
        super(MotionDetectorController.class, unitHost, builder);
    }

    public void updateMotionStateProvider(MotionState state) throws CouldNotPerformException {
        
        logger.debug("Apply motion Update[" + state + "] for " + this + ".");
        
        try (ClosableDataBuilder<MotionDetectorData.Builder> dataBuilder = getDataBuilder(this)) {

            MotionState.Builder motionStateBuilder = dataBuilder.getInternalBuilder().getMotionStateBuilder();
            
            // Update value
            motionStateBuilder.setValue(state.getValue());
            
            // Update timestemp if necessary
            if (state.getValue()== MotionState.State.MOVEMENT) {
            //TODO tamino: need to be tested! Please write an unit test.
                motionStateBuilder.setLastMovement(TimestampType.Timestamp.newBuilder().setTime(System.currentTimeMillis()));
            }

            dataBuilder.getInternalBuilder().setMotionState(motionStateBuilder);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply motion Update[" + state + "] for " + this + "!", ex);
        }
    }

    @Override
    public MotionState getMotionState() throws NotAvailableException {
        try {
            return getData().getMotionState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("motion", ex);
        }
    }
}
