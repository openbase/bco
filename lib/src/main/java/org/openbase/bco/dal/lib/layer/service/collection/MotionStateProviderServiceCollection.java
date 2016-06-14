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
import org.openbase.bco.dal.lib.layer.service.provider.MotionProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.homeautomation.state.MotionStateType.MotionState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface MotionStateProviderServiceCollection extends MotionProviderService {

    /**
     * Returns movement if at least one motion provider returns movement else no
     * movement.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public MotionState getMotion() throws NotAvailableException {
        try {
            for (MotionProviderService provider : getMotionStateProviderServices()) {
                if (provider.getMotion().getValue() == MotionState.State.MOVEMENT) {
                    return MotionState.newBuilder().setValue(MotionState.State.MOVEMENT).build();
                }
            }
            return MotionState.newBuilder().setValue(MotionState.State.NO_MOVEMENT).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("MotionState", ex);
        }
    }

    public Collection<MotionProviderService> getMotionStateProviderServices() throws CouldNotPerformException;
}
