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
import rst.domotic.state.ContactStateType.ContactState;
import rst.domotic.unit.dal.ReedContactDataType.ReedContactData;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ReedContactController extends AbstractUnitController<ReedContactData, ReedContactData.Builder> implements ReedContact {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ReedContactData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ContactState.getDefaultInstance()));
    }

    public ReedContactController(final UnitHost unitHost, ReedContactData.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(ReedContactController.class, unitHost, builder);
    }

    public void updateContactStateProvider(final ContactState contactState) throws CouldNotPerformException {
        logger.debug("Apply contactState Update[" + contactState + "] for " + this + ".");

        try (ClosableDataBuilder<ReedContactData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setContactState(contactState);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply contactState Update[" + contactState + "] for " + this + "!", ex);
        }
    }

    @Override
    public ContactState getContactState() throws NotAvailableException {
        try {
            return getData().getContactState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("contactState", ex);
        }
    }
}
