/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.unit;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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


import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.MotionStateType.MotionState;
import rst.homeautomation.unit.MotionSensorType.MotionSensor;
import rst.homeautomation.unit.UnitConfigType;
import rst.timing.TimestampType;

/**
 *
 * @author mpohling
 */
public class MotionSensorController extends AbstractUnitController<MotionSensor, MotionSensor.Builder> implements MotionSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionSensor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionState.getDefaultInstance()));
    }

    public MotionSensorController(final UnitConfigType.UnitConfig config, final UnitHost unitHost, final MotionSensor.Builder builder) throws org.dc.jul.exception.InstantiationException, CouldNotPerformException {
        super(config, MotionSensorController.class, unitHost, builder);
    }

    public void updateMotion(MotionState state) throws CouldNotPerformException {
        
        logger.debug("Apply motion Update[" + state + "] for " + this + ".");
        
        try (ClosableDataBuilder<MotionSensor.Builder> dataBuilder = getDataBuilder(this)) {

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
    public MotionState getMotion() throws NotAvailableException {
        try {
            return getData().getMotionState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("motion", ex);
        }
    }
}
