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
import static org.openbase.bco.dal.lib.simulation.service.AbstractScheduledServiceSimulator.RANDOM;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BatteryStateType.BatteryState;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * Custom unit simulator.
 */
public class BatteryStateServiceSimulator extends AbstractScheduledServiceSimulator<BatteryState> {

    public static final int MAX_BATTERY_LEVEL = 100;
    public static final int MIN_BATTERY_LEVEL = 0;

    private final BatteryState.Builder simulatedBatteryState;

    /**
     * Creates a new custom unit simulator.
     *
     * @param unitController the unit to simulate.
     */
    public BatteryStateServiceSimulator(UnitController unitController) {
        super(unitController, ServiceType.BATTERY_STATE_SERVICE);
        this.simulatedBatteryState = BatteryState.newBuilder();
        this.simulatedBatteryState.setLevel(RANDOM.nextInt(MAX_BATTERY_LEVEL));
    }

    private BatteryState getSimulatedBatteryState() {
        if (simulatedBatteryState.getLevel() <= 0) {
            simulatedBatteryState.setLevel(MAX_BATTERY_LEVEL);
        }
        simulatedBatteryState.setLevel(Math.max(MIN_BATTERY_LEVEL, simulatedBatteryState.getLevel() - (RANDOM.nextInt(2))));
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
