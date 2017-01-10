package org.openbase.bco.dal.remote.unit;

/*
 * #%L
 * BCO DAL Remote
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
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.AlarmStateType.AlarmState;
import rst.domotic.state.SmokeStateType.SmokeState;
import rst.domotic.unit.dal.SmokeDetectorDataType.SmokeDetectorData;
import org.openbase.bco.dal.lib.layer.unit.SmokeDetector;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SmokeDetectorRemote extends AbstractUnitRemote<SmokeDetectorData> implements SmokeDetector {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeDetectorData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeState.getDefaultInstance()));
    }

    public SmokeDetectorRemote() {
        super(SmokeDetectorData.class);
    }

    @Override
    public void notifyDataUpdate(SmokeDetectorData data) throws CouldNotPerformException {
    }

    @Override
    public AlarmState getSmokeAlarmState() throws NotAvailableException {
        try {
            return getData().getSmokeAlarmState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("SmokeAlarmState", ex);
        }
    }

    @Override
    public SmokeState getSmokeState() throws NotAvailableException {
        try {
            return getData().getSmokeState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("SmokeState", ex);
        }
    }
}
