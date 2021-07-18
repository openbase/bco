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
import org.openbase.jul.exception.EnumNotSupportedException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.action.ActionEmphasisType.ActionEmphasis;
import org.openbase.type.domotic.action.ActionEmphasisType.ActionEmphasis.Category;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState;
import org.openbase.type.domotic.state.PowerConsumptionStateType.PowerConsumptionState;

import java.util.Random;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * <p>
 * Custom unit simulator.
 */
public class EmphasisStateServiceSimulator extends AbstractScheduledServiceSimulator<EmphasisState> {

    /**
     * Creates a new emphasis state simulator.
     *
     * @param unitController the unit to simulate.
     */
    public EmphasisStateServiceSimulator(UnitController unitController) {
        super(unitController, ServiceType.EMPHASIS_STATE_SERVICE);
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     *
     * @throws NotAvailableException {@inheritDoc }
     */
    @Override
    protected EmphasisState getNextServiceState() throws NotAvailableException {

        // randomly select dominant emphasis category
        final Category emphasisCategory = Category.forNumber(RANDOM.nextInt(3) + 2);

        double security = 0, economy= 0, comfort= 0;

        // compute category weights
        switch (emphasisCategory) {
            case SECURITY:
                security = 0.5d + RANDOM.nextDouble() * 0.49d;
                economy = (1d - security) * RANDOM.nextDouble();
                comfort = 1d - (security + economy);
                break;
            case ECONOMY:
                economy = 0.5d + RANDOM.nextDouble() * 0.49d;
                security = (1d - economy) * RANDOM.nextDouble();
                comfort = 1d - (security + economy);
                break;
            case COMFORT:
                comfort = 0.5d + RANDOM.nextDouble() * 0.49d;
                economy = (1d - comfort) * RANDOM.nextDouble();
                security = 1d - (comfort + economy);
                break;
            default:
                LOGGER.error("Unexpected emphasis category simulated: "+ emphasisCategory.name());
                break;
        }

        return EmphasisState.newBuilder().setComfort(comfort).setSecurity(security).setEconomy(economy).build();
    }
}
