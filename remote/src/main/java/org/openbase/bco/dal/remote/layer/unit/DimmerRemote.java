package org.openbase.bco.dal.remote.layer.unit;

import org.openbase.bco.dal.lib.layer.unit.Dimmer;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.dal.DimmerDataType.DimmerData;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DimmerRemote extends AbstractUnitRemote<DimmerData> implements Dimmer {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DimmerData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessState.getDefaultInstance()));
    }

    public DimmerRemote() {
        super(DimmerData.class);
    }
}
