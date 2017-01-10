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
import org.openbase.bco.dal.lib.layer.service.provider.MotionStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.state.MotionStateType.MotionState;
import rst.timing.TimestampType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface MotionStateProviderServiceCollection extends MotionStateProviderService {

    //TODO: is implemented in the service remotes but still used in the LocationController because else it would lead to too many unitRemots
    //remove when remote cashing is implemented
    /**
     * Returns movement if at least one motion provider returns movement else no
     * movement.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public MotionState getMotionState() throws NotAvailableException {
        try {
            MotionState.Builder builder = MotionState.newBuilder().setValue(MotionState.State.NO_MOTION);
            builder.getLastMotionBuilder().setTime(System.currentTimeMillis());
            for (MotionStateProviderService provider : getMotionStateProviderServices()) {
                if (provider.getMotionState().getValue() == MotionState.State.MOTION) {
                    builder.setValue(MotionState.State.MOTION).build();
                    builder.getLastMotionBuilder().setTime(Math.max(builder.getLastMotion().getTime(), provider.getMotionState().getLastMotion().getTime()));
                }
            }
            return builder.build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("MotionState", ex);
        }
    }

    public Collection<MotionStateProviderService> getMotionStateProviderServices() throws CouldNotPerformException;
}
