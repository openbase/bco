package org.openbase.bco.dal.lib.layer.service.collection;

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
import java.util.Collection;
import org.openbase.bco.dal.lib.layer.service.provider.SmokeAlarmStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.state.AlarmStateType.AlarmState;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface SmokeAlarmStateProviderServiceCollection extends SmokeAlarmStateProviderService {

    //TODO: is implemented in the service remotes but still used in the LocationController because else it would lead to too many unitRemots
    //remove when remote cashing is implemented
    /**
     * Returns alarm if at least one smoke alarm state provider returns alarm
     * else no alarm.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public AlarmState getSmokeAlarmState() throws NotAvailableException {
        try {
            for (SmokeAlarmStateProviderService provider : getSmokeAlarmStateProviderServices()) {
                if (provider.getSmokeAlarmState().getValue() == AlarmState.State.ALARM) {
                    return AlarmState.newBuilder().setValue(AlarmState.State.ALARM).build();
                }
            }
            return AlarmState.newBuilder().setValue(AlarmState.State.NO_ALARM).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("AlarmState", ex);
        }
    }

    public Collection<SmokeAlarmStateProviderService> getSmokeAlarmStateProviderServices() throws CouldNotPerformException;
}
