/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import de.citec.csra.dm.view.struct.leave.PositionContainer;
import javafx.scene.control.TreeItem;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author thuxohl
 */
public class PlacementConfigContainer extends TreeItem<Node> implements Node {

    private PlacementConfig placement;

    public PlacementConfigContainer(PlacementConfig placement) {
        this.placement = placement;
        TreeItem<Node> position = new TreeItem<>(new PositionContainer(placement.getPosition()));
        TreeItem<Node> location = new LocationConfigContainer(placement.getLocation());
        this.getChildren().addAll(position, location);
    }

    @Override
    public String getDescriptor() {
        return "Placement";
    }
}
