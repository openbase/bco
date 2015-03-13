/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.service.SinactServiceConfigType.SinactServiceConfig;

/**
 *
 * @author thuxohl
 */
class SinactServiceConfigContainer extends NodeContainer<SinactServiceConfig> {
    
    public SinactServiceConfigContainer(SinactServiceConfig sinactServiceConfig) {
        super("Sinact Service Configuration", sinactServiceConfig);
        super.add(sinactServiceConfig.getHardwareConfig(), "Hardware Configuration");
    }
    
}
