/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.service.MieleAtHomeServiceConfigType.MieleAtHomeServiceConfig;

/**
 *
 * @author thuxohl
 */
class MieleAtHomeServiceConfigContainer extends NodeContainer<MieleAtHomeServiceConfig> {

    public MieleAtHomeServiceConfigContainer(MieleAtHomeServiceConfig mieleAtHomeServiceConfig) {
        super("Miele@Home Service Configuration", mieleAtHomeServiceConfig);
        super.add(mieleAtHomeServiceConfig.getHardwareConfig(), "Hardware Configuration");
    }

}
