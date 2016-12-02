package org.openbase.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.provider.PresenceStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.PresenceStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.PresenceStateType.PresenceState;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface PresenceStateProviderServiceCollection extends PresenceStateProviderService {

    //TODO: is implemented in the service remotes but still used in the LocationController because else it would lead to too many unitRemots
    //remove when remote cashing is implemented
    /**
     * Returns presence if at least one motion provider returns movement else no movement.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public PresenceState getPresenceState() throws NotAvailableException {
        try {
            PresenceState.Builder builder = PresenceState.newBuilder().setValue(PresenceState.State.ABSENT);
            builder.getLastPresenceBuilder().setTime(System.currentTimeMillis());
            for (PresenceStateProviderService provider : getPresenceStateProviderServices()) {
                if (provider.getPresenceState().getValue() == PresenceState.State.PRESENT) {
                    builder.setValue(PresenceState.State.PRESENT).build();
                    builder.getLastPresenceBuilder().setTime(Math.max(builder.getLastPresence().getTime(), provider.getPresenceState().getLastPresence().getTime()));
                }
            }
            return builder.build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PresenceState", ex);
        }
    }

    public Collection<PresenceStateProviderService> getPresenceStateProviderServices() throws CouldNotPerformException;
}
