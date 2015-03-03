/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import de.citec.csra.dm.view.struct.leave.UnitTypeContainer;
import java.util.List;
import javafx.scene.control.TreeItem;
import rst.homeautomation.unit.UnitTypeHolderType.UnitTypeHolder;

/**
 *
 * @author thuxohl
 */
public class UnitTypeListContainer extends TreeItem<Node> implements Node {

    public UnitTypeListContainer(List<UnitTypeHolder> unitTypes) {
        for (UnitTypeHolder unitType : unitTypes) {
            this.getChildren().add(new TreeItem<Node>(new UnitTypeContainer(unitType)));
        }
    }

    @Override
    public String getDescriptor() {
        return "Unit Types";
    }
}
