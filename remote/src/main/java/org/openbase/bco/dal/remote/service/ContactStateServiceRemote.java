package org.openbase.bco.dal.remote.service;

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
import java.util.Collection;
import org.openbase.bco.dal.lib.layer.service.collection.ContactStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.ContactStateProviderService;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ContactStateType.ContactState;
import rst.timing.TimestampType.Timestamp;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ContactStateServiceRemote extends AbstractServiceRemote<ContactStateProviderService, ContactState> implements ContactStateProviderServiceCollection {

    public ContactStateServiceRemote() {
        super(ServiceType.CONTACT_STATE_SERVICE);
    }

    @Override
    public Collection<ContactStateProviderService> getContactStateProviderServices() {
        return getServices();
    }

    /**
     * {@inheritDoc}
     * Computes the contact state as open if at least one underlying service is open and else closed.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected ContactState computeServiceState() throws CouldNotPerformException {
        ContactState.State contactValue = ContactState.State.CLOSED;
        for (ContactStateProviderService provider : getContactStateProviderServices()) {
            if (!((UnitRemote) provider).isDataAvailable()) {
                continue;
            }

            if (provider.getContactState().getValue() == ContactState.State.OPEN) {
                contactValue = ContactState.State.OPEN;
            }
        }

        return ContactState.newBuilder().setValue(contactValue).setTimestamp(Timestamp.newBuilder().setTime(System.currentTimeMillis())).build();
    }

    @Override
    public ContactState getContactState() throws NotAvailableException {
        return getServiceState();
    }
}
