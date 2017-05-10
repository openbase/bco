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
import rst.domotic.state.PowerConsumptionStateType.PowerConsumptionState;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * Custom unit simulator.
 */
public class PowerConsumptionStateServiceSimulator extends AbstractScheduledServiceSimulator<PowerConsumptionState> {

    public static final long CHANGE_RATE = 5000;
    public static final double FUSE = 16;
    public static final double POWER_VOLTAGE = 230;
    public static final double MAX_POWER_CONSUMPTION = POWER_VOLTAGE * FUSE;
    public static final double MIN_POWER_CONSUMPTION = 0;

    private double simulatedPowerConsumption;

    /**
     * Creates a new custom unit simulator.
     * @param unitController the unit to simulate.
     */
    public PowerConsumptionStateServiceSimulator(UnitController unitController) {
        super(unitController, ServiceType.POWER_CONSUMPTION_STATE_SERVICE, CHANGE_RATE);
        this.simulatedPowerConsumption = RANDOM.nextInt((int) MAX_POWER_CONSUMPTION);
    }

    private double getSimulatedPowerConsumption() {
        simulatedPowerConsumption += (RANDOM.nextGaussian() * 1000d - 500d);
        simulatedPowerConsumption = Math.max(MIN_POWER_CONSUMPTION, Math.min(MAX_POWER_CONSUMPTION, simulatedPowerConsumption));
        return simulatedPowerConsumption;
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     * @throws NotAvailableException {@inheritDoc }
     */
    @Override
    protected PowerConsumptionState getNextServiceState() throws NotAvailableException {
        return PowerConsumptionState.newBuilder().setVoltage(POWER_VOLTAGE).setConsumption(getSimulatedPowerConsumption()).build();
    }
}
