package org.openbase.bco.dal.remote.layer.service;

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

import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.collection.TamperStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.TamperStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.TamperStateType.TamperState;
import org.openbase.type.domotic.state.TamperStateType.TamperState.State;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TamperStateServiceRemote extends AbstractServiceRemote<TamperStateProviderService, TamperState> implements TamperStateProviderServiceCollection {

    public TamperStateServiceRemote() {
        super(ServiceType.TAMPER_STATE_SERVICE, TamperState.class);
    }

    /**§
     * {@inheritDoc}
     * Computes the tamper state as tamper if at least one underlying service detects tamper and else no tamper.
     * Additionally the last detection timestamp as set as the latest of the underlying services.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected TamperState computeServiceState() throws CouldNotPerformException {
        return getTamperState(UnitType.UNKNOWN);
    }

    @Override
    public TamperState getTamperState(final UnitType unitType) throws NotAvailableException {
        try {
            return (TamperState) generateAggregatedState(unitType, State.NO_TAMPER, State.TAMPER).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException(Services.getServiceStateName(getServiceType()), ex);
        }
    }
}
