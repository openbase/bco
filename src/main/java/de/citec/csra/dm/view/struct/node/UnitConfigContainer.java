/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import de.citec.csra.dm.view.struct.leave.DescriptionContainer;
import de.citec.csra.dm.view.struct.leave.LabelContainer;
import de.citec.csra.dm.view.struct.leave.NameContainer;
import de.citec.csra.dm.view.struct.leave.ScopeContainer;
import javafx.scene.control.TreeItem;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class UnitConfigContainer extends TreeItem<Node> implements Node {
    
    private UnitConfigType.UnitConfig unitConfig;

    public UnitConfigContainer(UnitConfigType.UnitConfig unitConfig) {
        this.unitConfig = unitConfig;
        TreeItem<Node> label = new TreeItem(new LabelContainer(unitConfig.getLabel()));
        TreeItem<Node> name = new TreeItem(new NameContainer(unitConfig.getName()));
        TreeItem<Node> placement = new PlacementConfigContainer(unitConfig.getPlacement());
        TreeItem<Node> services = new ServiceConfigListContainer(unitConfig.getServiceConfigsList());
        TreeItem<Node> scope = new TreeItem(new ScopeContainer(unitConfig.getScope()));
        TreeItem<Node> description = new TreeItem(new DescriptionContainer(unitConfig.getDescription()));
        this.getChildren().addAll(label,name,placement,services,scope,description);
    }

    @Override
    public String getDescriptor() {
        return "Unit Config";
    }
}
