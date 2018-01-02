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
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.TamperStateType.TamperState;
import rst.domotic.unit.dal.TamperDetectorDataType.TamperDetectorData;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TamperDetectorController extends AbstractDALUnitController<TamperDetectorData, TamperDetectorData.Builder> implements TamperDetector {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperDetectorData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperState.getDefaultInstance()));
    }

    public TamperDetectorController(final UnitHost unitHost, final TamperDetectorData.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(TamperDetectorController.class, unitHost, builder);
    }

    @Override
    public TamperState getTamperState() throws NotAvailableException {
        try {
            return getData().getTamperState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("tamperState", ex);
        }
    }

    @Override
    protected void applyDataUpdate(TamperDetectorData.Builder internalBuilder, ServiceType serviceType) {
        switch (serviceType) {
            case TAMPER_STATE_SERVICE:
                TamperState.Builder tamperState = internalBuilder.getTamperStateBuilder();

                // Update timestemp if necessary
                if (tamperState.getValue() == TamperState.State.TAMPER) {
                    if (!tamperState.hasTimestamp()) {
                        logger.warn("State[" + tamperState.getClass().getSimpleName() + "] of " + this + " does not contain any state related timestampe!");
                        tamperState = TimestampProcessor.updateTimestampWithCurrentTime(tamperState, logger);
                    }
                    tamperState.setLastDetection(tamperState.getTimestamp());
                }
                break;
        }
    }
}
