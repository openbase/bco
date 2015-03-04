/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import de.citec.csra.dm.view.struct.leave.UnitTypeContainer;
import java.util.Collection;
import javafx.scene.control.TreeItem;
import rst.homeautomation.unit.UnitTypeHolderType.UnitTypeHolder;

/**
 *
 * @author thuxohl
 */
public class UnitTypeListContainer extends TreeItem<Node> implements Node {

    public UnitTypeListContainer(final Collection<UnitTypeHolder> unitTypes) {
        unitTypes.stream().forEach((unitType) -> {
            this.getChildren().add(new TreeItem<>(new UnitTypeContainer(unitType)));
        });
    }

    @Override
    public String getDescriptor() {
        return "Unit Types";
    }
}
