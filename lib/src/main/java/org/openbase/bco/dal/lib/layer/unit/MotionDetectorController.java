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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.MotionStateType.MotionState;
import rst.domotic.unit.dal.MotionDetectorDataType.MotionDetectorData;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class MotionDetectorController extends AbstractDALUnitController<MotionDetectorData, MotionDetectorData.Builder> implements MotionDetector {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionDetectorData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionState.getDefaultInstance()));
    }

    public MotionDetectorController(final UnitHost unitHost, final MotionDetectorData.Builder builder) throws org.openbase.jul.exception.InstantiationException, CouldNotPerformException {
        super(MotionDetectorController.class, unitHost, builder);
    }

    @Override
    public MotionState getMotionState() throws NotAvailableException {
        try {
            return getData().getMotionState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("motionState", ex);
        }
    }

    @Override
    protected void applyDataUpdate(MotionDetectorData.Builder internalBuilder, ServiceType serviceType) {
        switch (serviceType) {
            case MOTION_STATE_SERVICE:
                MotionState.Builder motionState = internalBuilder.getMotionStateBuilder();

                // Update timestamp if necessary
                if (motionState.getValue() == MotionState.State.MOTION) {
                    if (!motionState.hasTimestamp()) {
                        logger.warn("State[" + motionState.getClass().getSimpleName() + "] of " + this + " does not contain any state related timestampe!");
                        motionState = TimestampProcessor.updateTimestampWithCurrentTime(motionState, logger);
                    }
                    motionState.setLastMotion(motionState.getTimestamp());
                } else if(motionState.getValue() == MotionState.State.NO_MOTION && internalBuilder.getMotionStateLast().hasLastMotion()) {
                    motionState.setLastMotion(internalBuilder.getMotionStateLast().getLastMotion());
                }
                break;
        }
    }
}
