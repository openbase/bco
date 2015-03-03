/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import de.citec.csra.dm.view.struct.leave.BindingConfigContainer;
import de.citec.csra.dm.view.struct.leave.DescriptionContainer;
import de.citec.csra.dm.view.struct.leave.LabelContainer;
import de.citec.csra.dm.view.struct.leave.ProductNumberContainer;
import javafx.scene.control.TreeItem;
import rst.homeautomation.device.DeviceClassType.DeviceClass;

/**
 *
 * @author thuxohl
 */
public class DeviceClassContainer extends TreeItem<Node> implements Node {

    private final DeviceClass deviceClass;

    public DeviceClassContainer(DeviceClass deviceClass) {
        this.deviceClass = deviceClass;
        TreeItem<Node> label = new TreeItem(new LabelContainer(deviceClass.getLabel()));
        TreeItem<Node> description = new TreeItem(new DescriptionContainer(deviceClass.getDescription()));
        TreeItem<Node> bindingConfig = new TreeItem(new BindingConfigContainer(deviceClass.getBindingConfig()));
        TreeItem<Node> productNumber = new TreeItem(new ProductNumberContainer(deviceClass.getProductNumber()));
        UnitTypeListContainer unitTypeList = new UnitTypeListContainer(deviceClass.getUnitsList());
        this.getChildren().addAll(label, description, bindingConfig, productNumber, unitTypeList);
    }

    @Override
    public String getDescriptor() {
        return deviceClass.getId();
    }
}
