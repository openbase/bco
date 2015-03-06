/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import de.citec.csra.dm.view.struct.leave.LabelContainer;
import javafx.scene.control.TreeItem;
import rst.spatial.LocationConfigType;

/**
 *
 * @author thuxohl
 */
public class LocationConfigContainer extends TreeItem<Node> implements Node {
    
    private LocationConfigType.LocationConfig location;

    public LocationConfigContainer(LocationConfigType.LocationConfig location) {
        this.location = location;
        TreeItem<Node> label = new TreeItem<>(new LabelContainer(location.getLabel()));
        //TODO
    }

    @Override
    public String getDescriptor() {
        return "Location";
    } 
}
