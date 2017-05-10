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
import com.google.protobuf.GeneratedMessage;
import java.util.ArrayList;
import java.util.List;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * @param <SERVICE_STATE> the type of the service states used for the simulation.
 */
public class AbstractRandomServiceSimulator<SERVICE_STATE extends GeneratedMessage> extends AbstractScheduledServiceSimulator<SERVICE_STATE> {

    private final List<SERVICE_STATE> stateList;

    public AbstractRandomServiceSimulator(final UnitController unitController, final ServiceType serviceType) {
        super(unitController, serviceType);
        this.stateList = new ArrayList<>();
    }

    public AbstractRandomServiceSimulator(final UnitController unitController, final ServiceType serviceType, long changeRate) {
        super(unitController, serviceType, changeRate);
        this.stateList = new ArrayList<>();
    }

    /**
     * Method registers a new service state which is used for the service simulation.
     *
     * @param state
     */
    public void registerServiceState(final SERVICE_STATE state) {
        stateList.add(state);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws NotAvailableException {@inheritDoc}
     */
    @Override
    protected SERVICE_STATE getNextServiceState() throws NotAvailableException {
        if (stateList.isEmpty()) {
            throw new NotAvailableException("NextServiceState");
        }
        return stateList.get(RANDOM.nextInt(stateList.size()));
    }
}
