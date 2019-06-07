package org.openbase.bco.dal.lib.simulation.service;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
import static org.openbase.bco.dal.lib.simulation.service.AbstractScheduledServiceSimulator.RANDOM;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BatteryStateType.BatteryState;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * Custom unit simulator.
 */
public class BatteryStateServiceSimulator extends AbstractScheduledServiceSimulator<BatteryState> {

    public static final double MAX_BATTERY_LEVEL = 1d;
    public static final double MIN_BATTERY_LEVEL = 0;

    private final BatteryState.Builder simulatedBatteryState;

    /**
     * Creates a new custom unit simulator.
     *
     * @param unitController the unit to simulate.
     */
    public BatteryStateServiceSimulator(final UnitController unitController) {
        super(unitController, ServiceType.BATTERY_STATE_SERVICE);
        this.simulatedBatteryState = BatteryState.newBuilder();
        this.simulatedBatteryState.setLevel(RANDOM.nextDouble());
    }

    private BatteryState getSimulatedBatteryState() {
        if (simulatedBatteryState.getLevel() <= 0) {
            simulatedBatteryState.setLevel(MAX_BATTERY_LEVEL);
        }
        simulatedBatteryState.setLevel(Math.max(MIN_BATTERY_LEVEL, simulatedBatteryState.getLevel() - (RANDOM.nextDouble() * 0.02d)));
        return simulatedBatteryState.build();
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     * @throws NotAvailableException {@inheritDoc }
     */
    @Override
    protected BatteryState getNextServiceState() throws NotAvailableException {
        return getSimulatedBatteryState();
    }
}
