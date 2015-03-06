/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import java.util.Collection;
import javafx.scene.control.TreeItem;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class UnitConfigListContainer extends TreeItem<Node> implements Node {
    
    public UnitConfigListContainer(final Collection<UnitConfigType.UnitConfig> unitConfigs) {
        unitConfigs.stream().forEach((unitConfig) -> {
            this.getChildren().add(new UnitConfigContainer(unitConfig));
        });
    }

    @Override
    public String getDescriptor() {
        return "Unit Configs";
    }
}
