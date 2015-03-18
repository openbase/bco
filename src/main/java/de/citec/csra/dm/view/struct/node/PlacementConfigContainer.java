/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author thuxohl
 */
public class PlacementConfigContainer extends NodeContainer<PlacementConfig.Builder> {

    public PlacementConfigContainer(PlacementConfig.Builder placement) {
        super("Placement", placement);
        super.add(new PositionContainer(placement.getPositionBuilder()));
        super.add(new LocationConfigContainer(placement.getLocationBuilder()));
    }
}
