/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.scene.visual;

/*
 * #%L
 * COMA SceneManager Visualisation
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.JComponent;
import org.dc.bco.dal.lib.layer.service.Service;
import org.dc.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.dc.bco.dal.visual.service.AbstractServicePanel;
import org.dc.bco.dal.visual.unit.GenericUnitPanel;
import org.dc.bco.dal.visual.unit.RemovableGenericUnitPanel;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.dc.jul.processing.StringProcessor;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.action.ActionAuthorityType.ActionAuthority;
import rst.homeautomation.control.action.ActionConfigType.ActionConfig;
import rst.homeautomation.control.action.ActionPriorityType.ActionPriority;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class DalSceneEditor extends javax.swing.JFrame {

    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(DalSceneEditor.class);
    private final ServiceJSonProcessor serviceProcessor;

    /**
     * Creates new form DalSceneEditor
     *
     */
    public DalSceneEditor() {
        serviceProcessor = new ServiceJSonProcessor();
        initComponents();
    }

    public DalSceneEditor init() throws InitializationException, InterruptedException {
        try {
            sceneSelectorPanel.addObserver(new Observer<SceneSelectorPanel.UnitConfigServiceTypeHolder>() {

                @Override
                public void update(Observable<SceneSelectorPanel.UnitConfigServiceTypeHolder> source, SceneSelectorPanel.UnitConfigServiceTypeHolder data) throws Exception {
                    genericUnitCollectionPanel.add(data.getConfig(), data.getServiceType(), true);
                }
            });
            sceneCreationPanel.addObserver(new Observer<List<ActionConfig>>() {

                @Override
                public void update(Observable<List<ActionConfig>> source, List<ActionConfig> data) throws Exception {
//                    logger.info("Update through new scene selected!");
                    genericUnitCollectionPanel.clearUnitPanel();
//                    logger.info("Cleared unit collection panel!");
                    for (ActionConfig action : data) {
//                        logger.info("Adding new unit panel for action [" + action.getServiceAttributeType() + "][" + action.getServiceAttribute() + "]");
                        Object value = serviceProcessor.deserialize(action.getServiceAttribute(), action.getServiceAttributeType());
                        genericUnitCollectionPanel.add(action.getServiceHolder(), action.getServiceType(), value, true);
                    }
                }
            });
            genericUnitCollectionPanel.init();
            sceneCreationPanel.init();

            CompletableFuture.runAsync(() -> {
                try {
                    sceneSelectorPanel.init();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
                }
            });
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        return this;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        sceneCreationPanel = new org.dc.bco.manager.scene.visual.SceneCreationPanel();
        sceneSelectorPanel = new org.dc.bco.manager.scene.visual.SceneSelectorPanel();
        genericUnitCollectionPanel = new org.dc.bco.dal.visual.unit.GenericUnitCollectionPanel();
        saveButton = new javax.swing.JButton();
        statusPanel1 = new org.dc.bco.dal.visual.util.StatusPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Scene"));

        saveButton.setText("Save");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sceneCreationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(sceneSelectorPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(genericUnitCollectionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 103, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(statusPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(saveButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sceneCreationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(sceneSelectorPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(genericUnitCollectionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(saveButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        Collection<GenericUnitPanel> unitPanelList = genericUnitCollectionPanel.getUnitPanelList();
        List<ActionConfig> actionConfigs = new ArrayList<>();
        // Generic unit panel means removable generic unit panel in this case
        for (GenericUnitPanel genericUnitPanel : unitPanelList) {
            GenericUnitPanel unitPanel = ((RemovableGenericUnitPanel) genericUnitPanel).getGenericUnitPanel();
            List<JComponent> componentList = unitPanel.getComponentList();
            for (JComponent component : componentList) {
                AbstractServicePanel panel = null;
                for (Component com : component.getComponents()) {
                    if (com instanceof AbstractServicePanel) {
                        panel = (AbstractServicePanel) com;
                    } else {
                    }
                }
                if (panel == null) {
                    continue;
                }
                try {
                    ActionConfig.Builder actionConfig = ActionConfig.newBuilder().setServiceType(panel.getServiceType()).setServiceHolder(panel.getUnitId());
                    Object value = getServiceValue(panel.getService(), panel.getServiceType());
                    actionConfig.setServiceAttribute(serviceProcessor.serialize(value));
                    actionConfig.setServiceAttributeType(serviceProcessor.getServiceAttributeType(value));
                    actionConfig.setActionAuthority(ActionAuthority.newBuilder().setAuthority(ActionAuthority.Authority.USER)).setActionPriority(ActionPriority.newBuilder().setPriority(ActionPriority.Priority.NORMAL));
                    actionConfigs.add(actionConfig.build());
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
                }
            }
        }
        try {
            sceneCreationPanel.updateSceneConfig(actionConfigs);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
        }
    }//GEN-LAST:event_saveButtonActionPerformed

    public Object getServiceValue(Service service, ServiceType serviceType) throws CouldNotPerformException {
        Object value = null;
        try {
            String methodName = ("get" + StringProcessor.transformUpperCaseToCamelCase(serviceType.name())).replaceAll("Service", "");
            Method method = service.getClass().getMethod(methodName);
            value = method.invoke(service);
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new CouldNotPerformException("Could not invoke getter for type [" + serviceType + "] on service [" + service + "]", ex);
        }
        return value;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DalSceneEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DalSceneEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DalSceneEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DalSceneEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            try {
                new DalSceneEditor().init().setVisible(true);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
                System.exit(1);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.dc.bco.dal.visual.unit.GenericUnitCollectionPanel genericUnitCollectionPanel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton saveButton;
    private org.dc.bco.manager.scene.visual.SceneCreationPanel sceneCreationPanel;
    private org.dc.bco.manager.scene.visual.SceneSelectorPanel sceneSelectorPanel;
    private org.dc.bco.dal.visual.util.StatusPanel statusPanel1;
    // End of variables declaration//GEN-END:variables
}
