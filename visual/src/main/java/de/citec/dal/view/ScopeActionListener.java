/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.view;

import de.citec.dal.Controller;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JTextField;
import rsb.Scope;

/**
 *
 * @author thuxohl
 */
public class ScopeActionListener implements ActionListener{

    private final Controller controller;
    private final JTextField scope1;
    private final JTextField scope2;
    
    public ScopeActionListener(Controller controller, JTextField scope1, JTextField scope2) {
        this.controller = controller;
        this.scope1 = scope1;
        this.scope2 = scope2;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Scope statusScope = controller.createStatusScope(e.getActionCommand());
        if(e.getSource().equals(scope1)) {
            controller.activateListener(statusScope, controller.getPowerPlugListener());
        } else if (e.getSource().equals(scope2)) {
            controller.activateListener(statusScope, controller.getButtonListener());
        }
    }
}
