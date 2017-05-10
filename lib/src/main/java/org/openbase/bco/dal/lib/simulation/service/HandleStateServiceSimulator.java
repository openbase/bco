package org.openbase.bco.dal.lib.simulation.service;

/*-
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

import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.HandleStateType.HandleState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class HandleStateServiceSimulator extends AbstractScheduledServiceSimulator<HandleState> {

    public static final int MIN_HANDLE_POSITION = 0;
    public static final int MAX_HANDLE_POSITION = 360;
    public static final int HANDLE_POSITION_STEP = 30;

    private final HandleState.Builder simulatedHandleState;

    /**
     * Creates a new custom unit simulator.
     *
     * @param unitController the unit to simulate.
     */
    public HandleStateServiceSimulator(UnitController unitController) {
        super(unitController, ServiceType.HANDLE_STATE_SERVICE);
        this.simulatedHandleState = HandleState.newBuilder();
        this.simulatedHandleState.setPosition(MIN_HANDLE_POSITION);
    }

    private HandleState getSimulatedHandleState() {
        simulatedHandleState.setPosition((simulatedHandleState.getPosition() + HANDLE_POSITION_STEP) % MAX_HANDLE_POSITION);
        return simulatedHandleState.build();
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     * @throws NotAvailableException {@inheritDoc }
     */
    @Override
    protected HandleState getNextServiceState() throws NotAvailableException {
        return getSimulatedHandleState();
    }
}
