package org.openbase.bco.dal.remote.service;

/*
 * #%L
 * BCO DAL Remote
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
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.collection.SmokeAlarmStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.SmokeAlarmStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.AlarmStateType.AlarmState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SmokeAlarmStateServiceRemote extends AbstractServiceRemote<SmokeAlarmStateProviderService, AlarmState> implements SmokeAlarmStateProviderServiceCollection {

    public SmokeAlarmStateServiceRemote() {
        super(ServiceType.SMOKE_ALARM_STATE_SERVICE, AlarmState.class);
    }

    public Collection<SmokeAlarmStateProviderService> getSmokeAlarmStateProviderServices() {
        return getServices();
    }

    /**
     * {@inheritDoc}
     * Computes the alarm state as alarm if at least one underlying service is on alarm and else no alarm.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected AlarmState computeServiceState() throws CouldNotPerformException {
        return getSmokeAlarmState(UnitType.UNKNOWN);
    }

    @Override
    public AlarmState getSmokeAlarmState() throws NotAvailableException {
        return getServiceState();
    }

    @Override
    public AlarmState getSmokeAlarmState(final UnitType unitType) throws NotAvailableException {
        AlarmState.State alarmValue = AlarmState.State.NO_ALARM;
        long timestamp = 0;
        for (SmokeAlarmStateProviderService service : getServices(unitType)) {
            if (!((UnitRemote) service).isDataAvailable()) {
                continue;
            }

            if (service.getSmokeAlarmState().getValue() == AlarmState.State.ALARM) {
                alarmValue = AlarmState.State.ALARM;
            }

            timestamp = Math.max(timestamp, service.getSmokeAlarmState().getTimestamp().getTime());
        }
        return TimestampProcessor.updateTimestamp(timestamp, AlarmState.newBuilder().setValue(alarmValue), TimeUnit.MICROSECONDS, logger).build();
    }
}
