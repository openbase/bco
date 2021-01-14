package org.openbase.bco.dal.lib.layer.service.collection;

/*
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

import org.openbase.bco.dal.lib.layer.service.provider.SmokeAlarmStateProviderService;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.state.AlarmStateType.AlarmState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface SmokeAlarmStateProviderServiceCollection extends SmokeAlarmStateProviderService {

    /**
     * Returns alarm if at least one smokeAlarmStateProvider returns alarm
     * else no alarm.
     *
     * @return
     *
     * @throws NotAvailableException
     */
    @Override
    default AlarmState getSmokeAlarmState() throws NotAvailableException {
        return SmokeAlarmStateProviderService.super.getSmokeAlarmState();
    }

    /**
     * Returns alarm if at least one smokeAlarmStateProvider with given unitType returns alarm
     * else no alarm.
     *
     * @param unitType
     *
     * @return
     *
     * @throws NotAvailableException
     */
    AlarmState getSmokeAlarmState(final UnitType unitType) throws NotAvailableException;
}
