package org.openbase.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.provider.PresenceStateProviderService;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface PresenceStateProviderServiceCollection extends PresenceStateProviderService {

    /**
     * Returns presence if at least one presenceDetector returns presence else no no presence.
     * The most recent lastPresenceTime is also set as the lastPresenceTime.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default PresenceState getPresenceState() throws NotAvailableException {
        return PresenceStateProviderService.super.getPresenceState();
    }

    /**
     * Returns presence if at least one presenceDetector with given unitType returns presence else no no presence.
     * The most recent lastPresenceTime is also set as the lastPresenceTime.
     *
     * @return
     * @throws NotAvailableException
     */
    PresenceState getPresenceState(final UnitType unitType) throws NotAvailableException;
}
