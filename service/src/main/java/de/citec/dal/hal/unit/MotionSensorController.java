/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;


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
