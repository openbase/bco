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
import org.openbase.bco.dal.lib.layer.service.provider.TamperStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.state.TamperStateType.TamperState;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface TamperStateProviderServiceCollection extends TamperStateProviderService {

    //TODO: is implemented in the service remotes but still used in the LocationController because else it would lead to too many unitRemots
    //remove when remote cashing is implemented
    /**
     * Returns tamper if at least one of the tamper providers returns tamper and
     * else no tamper.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public TamperState getTamperState() throws NotAvailableException {
        try {
            for (TamperStateProviderService provider : getTamperStateProviderServices()) {
                if (provider.getTamperState().getValue() == TamperState.State.TAMPER) {
                    return TamperState.newBuilder().setValue(TamperState.State.TAMPER).build();
                }
            }
            return TamperState.newBuilder().setValue(TamperState.State.NO_TAMPER).build();

        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("TamperState", ex);
        }
    }

    public Collection<TamperStateProviderService> getTamperStateProviderServices() throws CouldNotPerformException;
}
