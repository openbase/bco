/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.ShutterService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.state.ShutterStateType.ShutterState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface ShutterStateOperationServiceCollection extends ShutterService {

    @Override
    default public void setShutter(ShutterState state) throws CouldNotPerformException {
        for (ShutterService service : getShutterStateOperationServices()) {
            service.setShutter(state);
        }
    }

    /**
     * Returns up if all shutter services are up and else the from up differing
     * state of the first shutter.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public ShutterState getShutter() throws CouldNotPerformException {
        for (ShutterService service : getShutterStateOperationServices()) {
            switch (service.getShutter().getValue()) {
                case DOWN:
                    return ShutterState.newBuilder().setValue(ShutterState.State.DOWN).build();
                case STOP:
                    return ShutterState.newBuilder().setValue(ShutterState.State.STOP).build();
                case UP:
                default:
            }
        }
        return ShutterState.newBuilder().setValue(ShutterState.State.UP).build();
    }

    public Collection<ShutterService> getShutterStateOperationServices();
}
