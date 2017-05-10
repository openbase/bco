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
import rst.domotic.state.BrightnessStateType.BrightnessState;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * 
 * Custom unit simulator.
 */
public class BrightnessStateServiceSimulator extends AbstractScheduledServiceSimulator<BrightnessState> {

    public static final int MAX_BRIGHTNESS = 100;

    /**
     * Creates a new custom unit simulator.
     * @param unitController the unit to simulate.
     */
    public BrightnessStateServiceSimulator(UnitController unitController) {
        super(unitController, ServiceType.BRIGHTNESS_STATE_SERVICE);
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     * @throws NotAvailableException {@inheritDoc }
     */
    @Override
    protected BrightnessState getNextServiceState() throws NotAvailableException {
        return BrightnessState.newBuilder().setBrightnessDataUnit(BrightnessState.DataUnit.PERCENT).setBrightness(RANDOM.nextInt(MAX_BRIGHTNESS)).build();
    }
}
