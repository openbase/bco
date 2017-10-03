/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.dal.lib.layer.unit;

/*-
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
import rst.domotic.state.IlluminanceStateType.IlluminanceState;
import rst.domotic.unit.dal.LightSensorDataType.LightSensorData;

/**
 *
 * @author pleminoq
 */
public class LightSensorController extends AbstractDALUnitController<LightSensorData, LightSensorData.Builder> implements LightSensor {
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LightSensorData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(IlluminanceState.getDefaultInstance()));
    }
    
    public LightSensorController(final UnitHost unitHost, LightSensorData.Builder builder) throws org.openbase.jul.exception.InstantiationException, CouldNotPerformException {
        super(LightSensorController.class, unitHost, builder);
    }
    
//    public void updateIlluminanceStateProvider(final IlluminanceState illuminanceState) throws CouldNotPerformException {
//        logger.debug("Apply illuminanceState Update[" + illuminanceState + "] for " + this + ".");
//        
//        try (ClosableDataBuilder<LightSensorData.Builder> dataBuilder = getDataBuilder(this)) {
//            long transactionId = dataBuilder.getInternalBuilder().getIlluminanceState().getTransactionId() + 1;
//            dataBuilder.getInternalBuilder().setIlluminanceState(illuminanceState.toBuilder().setTransactionId(transactionId));
//        } catch (Exception ex) {
//            throw new CouldNotPerformException("Could not apply illuminanceState Update[" + illuminanceState + "] for " + this + "!", ex);
//        }
//    }
    
    @Override
    public IlluminanceState getIlluminanceState() throws NotAvailableException {
        try {
            return getData().getIlluminanceState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("illuminanceState", ex);
        }
    }
    
}
