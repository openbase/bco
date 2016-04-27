/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.provider.SmokeAlarmStateProvider;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.state.AlarmStateType.AlarmState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface SmokeAlarmStateProviderServiceCollection extends SmokeAlarmStateProvider {

    /**
     * Returns alarm if at least one smoke alarm state provider returns alarm
     * else no alarm.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public AlarmState getSmokeAlarmState() throws CouldNotPerformException {
        for (SmokeAlarmStateProvider provider : getSmokeAlarmStateProviderServices()) {
            if (provider.getSmokeAlarmState().getValue() == AlarmState.State.ALARM) {
                return AlarmState.newBuilder().setValue(AlarmState.State.ALARM).build();
            }
        }
        return AlarmState.newBuilder().setValue(AlarmState.State.NO_ALARM).build();
    }

    public Collection<SmokeAlarmStateProvider> getSmokeAlarmStateProviderServices();
}
