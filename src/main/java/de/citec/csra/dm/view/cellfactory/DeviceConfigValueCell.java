/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.cellfactory;

import de.citec.csra.dm.remote.DeviceRegistryRemote;
import de.citec.csra.dm.view.struct.node.DeviceConfigContainer;
import de.citec.jul.exception.NotAvailableException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import rst.homeautomation.device.DeviceClassType.DeviceClass;

/**
 *
 * @author thuxohl
 */
public class DeviceConfigValueCell extends ValueCell {

    private final ComboBox<DeviceClass> comboBox;
    
    public DeviceConfigValueCell(DeviceRegistryRemote remote) {
        super(remote);
        
        comboBox = new ComboBox();
        comboBox.setCellFactory(new Callback<ListView<DeviceClass>, ListCell<DeviceClass>>() {

            @Override
            public ListCell<DeviceClass> call(ListView<DeviceClass> param) {
                return new ComboBoxCell();
            }
        });
    }
    
    @Override
    public void startEdit() {
        super.startEdit();
        
        if( getItem() instanceof DeviceConfigContainer ) {
            try {
                comboBox.setItems(FXCollections.observableArrayList(remote.getData().getDeviceClassesList()));
            } catch (NotAvailableException ex) {
                Logger.getLogger(DeviceConfigValueCell.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private class ComboBoxCell extends ListCell<DeviceClass> {
        
        @Override
        public void updateItem(DeviceClass item, boolean empty) {
            super.updateItem(item, empty);
            
            if(!empty) {
                setText(item.getId());
            }
        }
    }
}
