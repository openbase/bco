/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.homeautomation.service.OpenhabServiceConfigType.OpenhabServiceConfig;

/**
 *
 * @author thuxohl
 */
public class OpenhabServiceConfigContainer extends NodeContainer<OpenhabServiceConfig.Builder> {

    public OpenhabServiceConfigContainer(OpenhabServiceConfig.Builder openhabServiceConfig) {
        super("Openhab Service Configuration", openhabServiceConfig);
        super.add(openhabServiceConfig.getHardwareConfig(), "hardware_configuration");
    }
}
