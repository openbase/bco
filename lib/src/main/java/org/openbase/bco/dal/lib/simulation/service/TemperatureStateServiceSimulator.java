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
import rst.domotic.state.TemperatureStateType.TemperatureState;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * Custom unit simulator.
 */
public class TemperatureStateServiceSimulator extends AbstractScheduledServiceSimulator<TemperatureState> {

    public static final double MAX_TEMPERATURE = 35;
    public static final double MIN_TEMPERATURE = -10;

    private double simulatedTemperature;

    /**
     * Creates a new custom unit simulator.
     *
     * @param unitController the unit to simulate.
     * @param serviceType the serviceType of the simulator, either TEMPERATURE_STATE_SERVICE or TARGET_TEMPERATURE_STATE_SERVICE
     */
    public TemperatureStateServiceSimulator(final UnitController unitController, final ServiceType serviceType) {
        super(unitController, serviceType);
        this.simulatedTemperature = RANDOM.nextInt((int) MAX_TEMPERATURE);
    }

    /**
     * Creates a new custom unit simulator.
     *
     * @param unitController the unit to simulate.
     */
    public TemperatureStateServiceSimulator(final UnitController unitController) {
        this(unitController, ServiceType.TEMPERATURE_STATE_SERVICE);
    }

    private double getSimulatedTemperature() {
        simulatedTemperature += (RANDOM.nextGaussian() - 0.5);
        simulatedTemperature = Math.max(MIN_TEMPERATURE, Math.min(MAX_TEMPERATURE, simulatedTemperature));
        return simulatedTemperature;
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     * @throws NotAvailableException {@inheritDoc }
     */
    @Override
    protected TemperatureState getNextServiceState() throws NotAvailableException {
        return TemperatureState.newBuilder().setTemperatureDataUnit(TemperatureState.DataUnit.CELSIUS).setTemperature(getSimulatedTemperature()).build();
    }
}
