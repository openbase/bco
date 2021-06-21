package org.openbase.bco.dal.lib.simulation.service;

/*-
 * #%L
 * BCO DAL Library
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

import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BlindStateType.BlindState;

/**
 * Custom service simulator.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BlindStateServiceSimulator extends AbstractScheduledServiceSimulator<BlindState> {

    public static final double MIN_OPENING_RATIO = 0d;
    public static final double MAX_OPENING_RATIO = 1d;
    public static final double OPENING_RATIO_STEP = 0.1d;

    private final BlindState.Builder simulatedBlindState;

    /**
     * Creates a new custom service simulator.
     *
     * @param unitController the unit to simulate.
     */
    public BlindStateServiceSimulator(UnitController unitController) {
        super(unitController, ServiceType.BLIND_STATE_SERVICE);
        this.simulatedBlindState = BlindState.newBuilder();
        this.simulatedBlindState.setValue(BlindState.State.STOP);
        this.simulatedBlindState.setOpeningRatio(1d);
    }

    private BlindState getSimulatedBlindState() {
        switch (simulatedBlindState.getValue()) {
            case STOP:
                if (simulatedBlindState.getOpeningRatio() == MAX_OPENING_RATIO) {
                    simulatedBlindState.setValue(BlindState.State.DOWN);
                    simulatedBlindState.setOpeningRatio(MAX_OPENING_RATIO - OPENING_RATIO_STEP);
                } else {
                    simulatedBlindState.setValue(BlindState.State.UP);
                    simulatedBlindState.setOpeningRatio(MIN_OPENING_RATIO + OPENING_RATIO_STEP);
                }
                break;
            case DOWN:
                if (simulatedBlindState.getOpeningRatio() > MIN_OPENING_RATIO) {
                    simulatedBlindState.setOpeningRatio(simulatedBlindState.getOpeningRatio() - OPENING_RATIO_STEP);
                } else {
                    simulatedBlindState.setValue(BlindState.State.STOP);
                    simulatedBlindState.setOpeningRatio(MIN_OPENING_RATIO);
                }
                break;
            case UP:
                if (simulatedBlindState.getOpeningRatio() < MAX_OPENING_RATIO) {
                    simulatedBlindState.setOpeningRatio(simulatedBlindState.getOpeningRatio() + OPENING_RATIO_STEP);
                } else {
                    simulatedBlindState.setValue(BlindState.State.STOP);
                    simulatedBlindState.setOpeningRatio(MAX_OPENING_RATIO);
                }
                break;
            default:
                simulatedBlindState.setValue(BlindState.State.STOP);
                simulatedBlindState.setOpeningRatio(MAX_OPENING_RATIO);
                break;
        }
        return simulatedBlindState.build();
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     * @throws NotAvailableException {@inheritDoc }
     */
    @Override
    protected BlindState getNextServiceState() throws NotAvailableException {
        return getSimulatedBlindState();
    }
}
