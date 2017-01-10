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
import rst.domotic.state.HandleStateType.HandleState;
import rst.domotic.unit.dal.HandleDataType.HandleData;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class HandleController extends AbstractUnitController<HandleData, HandleData.Builder> implements Handle {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HandleData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HandleState.getDefaultInstance()));
    }

    public HandleController(final UnitHost unitHost, final HandleData.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(HandleController.class, unitHost, builder);
    }

    public void updateHandleStateProvider(final HandleState handleState) throws CouldNotPerformException {
        logger.debug("Apply handleState Update[" + handleState + "] for " + this + ".");

        try (ClosableDataBuilder<HandleData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setHandleState(handleState);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply handleState Update[" + handleState + "] for " + this + "!", ex);
        }
    }

    @Override
    public HandleState getHandleState() throws NotAvailableException {
        try {
            return getData().getHandleState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("handleState", ex);
        }
    }
}
