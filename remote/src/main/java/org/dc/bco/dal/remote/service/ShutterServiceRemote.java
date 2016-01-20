/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import org.dc.bco.dal.lib.layer.service.ShutterService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType;
import rst.homeautomation.state.ShutterStateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ShutterServiceRemote extends AbstractServiceRemote<ShutterService> implements ShutterService {
    
    public ShutterServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType serviceType) {
        super(serviceType);
    }
    
    @Override
    public void setShutter(ShutterStateType.ShutterState.State state) throws CouldNotPerformException {
        for (ShutterService service : getServices()) {
            service.setShutter(state);
        }
    }
    
    @Override
    public ShutterStateType.ShutterState getShutter() throws CouldNotPerformException {
        throw new CouldNotPerformException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
