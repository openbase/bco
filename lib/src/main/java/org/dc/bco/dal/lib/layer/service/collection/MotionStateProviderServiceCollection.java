/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.provider.MotionProvider;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.state.MotionStateType.MotionState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface MotionStateProviderServiceCollection extends MotionProvider {

    /**
     * Returns movement if at least one motion provider returns movement else no
     * movement.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public MotionState getMotion() throws CouldNotPerformException {
        for (MotionProvider provider : getMotionStateProviderServices()) {
            if (provider.getMotion().getValue() == MotionState.State.MOVEMENT) {
                return MotionState.newBuilder().setValue(MotionState.State.MOVEMENT).build();
            }
        }
        return MotionState.newBuilder().setValue(MotionState.State.NO_MOVEMENT).build();
    }

    public Collection<MotionProvider> getMotionStateProviderServices();
}
