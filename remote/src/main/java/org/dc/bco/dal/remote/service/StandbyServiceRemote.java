/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import org.dc.bco.dal.lib.layer.service.StandbyService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType;
import rst.homeautomation.state.StandbyStateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class StandbyServiceRemote extends AbstractServiceRemote<StandbyService> implements StandbyService {

    public StandbyServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType serviceType) {
        super(serviceType);
    }

    @Override
    public void setStandby(StandbyStateType.StandbyState.State state) throws CouldNotPerformException {
        for(StandbyService service : getServices()) {
            service.setStandby(state);
        }
    }

    @Override
    public StandbyStateType.StandbyState getStandby() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
