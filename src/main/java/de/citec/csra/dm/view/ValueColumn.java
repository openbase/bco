/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view;

import de.citec.csra.dm.remote.DeviceRegistryRemote;
import de.citec.csra.dm.view.cellfactory.ValueCell;
import de.citec.csra.dm.view.struct.leaf.Leaf;
import de.citec.csra.dm.view.struct.node.Node;
import javafx.event.EventHandler;
import javafx.scene.control.TreeTableColumn;

/**
 *
 * @author thuxohl
 */
public class ValueColumn extends Column {
    
    public ValueColumn(DeviceRegistryRemote remote) {
        super("Value", new ValueCell(remote));
        this.setEditable(true);
        this.setSortable(false);
        this.setOnEditCommit(new EventHandlerImpl());
        this.setPrefWidth(1024 - 400);
    }
    
    private class EventHandlerImpl implements EventHandler<TreeTableColumn.CellEditEvent<Node, Node>> {
        
        @Override
        public void handle(CellEditEvent<Node, Node> event) {
            if (event.getRowValue().getValue() instanceof Leaf) {
                ((Leaf) event.getRowValue().getValue()).setValue(((Leaf) event.getNewValue()).getValue());
            }
        }
    }
}
