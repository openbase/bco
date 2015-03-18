/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view;

import de.citec.csra.dm.view.struct.node.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.util.Callback;

/**
 *
 * @author thuxohl
 */
public class Column extends TreeTableColumn<Node, Node> {
    
    public Column(String text, TreeTableCell<Node, Node> treeTableCell) {
        super(text);
        this.setCellValueFactory(new TreeItemPropertyValueFactory<>("this"));
        this.setCellFactory(new Callback<TreeTableColumn<Node, Node>, TreeTableCell<Node, Node>>() {
            
            @Override
            public TreeTableCell<Node, Node> call(TreeTableColumn<Node, Node> param) {
                return treeTableCell;
            }
        });
    }    
}
