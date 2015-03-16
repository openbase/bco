/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view;

import de.citec.csra.dm.view.struct.leaf.Leaf;
import de.citec.csra.dm.view.struct.node.Node;
import javafx.scene.control.TreeTableCell;

/**
 *
 * @author thuxohl
 */
public class NodeCell extends TreeTableCell<Node, Node> {
    
    protected Leaf leaf;

    public NodeCell() {
    }
    
    @Override
    public void startEdit() {
        super.startEdit();

        if (getItem() instanceof Leaf) {
            leaf = ((Leaf) getItem());
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setGraphic(null);
    }

    @Override
    public void commitEdit(Node newValue) {
        super.commitEdit(newValue);
        setGraphic(null);
        setText(((Leaf) newValue).getValue().toString());
    }

    @Override
    public void updateItem(Node item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setGraphic(null);
            setText("");
        } else if (item instanceof Leaf) {
            setGraphic(null);
            setText(((Leaf) item).getValue().toString());
        } else {
            setGraphic(null);
            setText("");
        }
    }
}
