package org.openbase.bco.manager.scene.visual;

/*
 * #%L
 * BCO Manager Scene Visualisation
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import javax.swing.JComponent;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceJSonProcessor;
import org.openbase.bco.dal.visual.service.AbstractServicePanel;
import org.openbase.bco.dal.visual.unit.GenericUnitPanel;
import org.openbase.bco.dal.visual.unit.RemovableGenericUnitPanel;
import org.openbase.bco.dal.visual.util.SelectorPanel;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.scene.SceneConfigType.SceneConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneEditor extends javax.swing.JFrame {
    
    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(SceneEditor.class);
    private final ServiceJSonProcessor serviceJSonProcessor;

    /**
     * Creates new form DalSceneEditor
     *
     */
    public SceneEditor() {
        this.serviceJSonProcessor = new ServiceJSonProcessor();
        initComponents();
    }
    
    public SceneEditor init() throws InitializationException, InterruptedException {
        try {
            sceneSelectorPanel.addObserver((final Observable<SelectorPanel.ServiceTypeHolder> source, SelectorPanel.ServiceTypeHolder data) -> {
                genericUnitCollectionPanel.add(data.getUnitConfig(), data.getServiceType(), true);
            });
            sceneCreationPanel.addObserver((final Observable<SceneConfig> source, SceneConfig data) -> {
                genericUnitCollectionPanel.clearUnitPanel();
                for (ServiceStateDescription serviceStateDescription : data.getRequiredServiceStateDescriptionList()) {
                    logger.info("Adding new unit panel for action [" + serviceStateDescription.getServiceAttributeType() + "][" + serviceStateDescription.getServiceAttribute() + "]");
                    Object value = serviceJSonProcessor.deserialize(serviceStateDescription.getServiceAttribute(), serviceStateDescription.getServiceAttributeType());
                    genericUnitCollectionPanel.add(serviceStateDescription.getUnitId(), serviceStateDescription.getServiceType(), value, true);
                    RemovableGenericUnitPanel removableUnitPanel = (RemovableGenericUnitPanel) genericUnitCollectionPanel.getUnitPanelMap().get(serviceStateDescription.getUnitId() + serviceStateDescription.getServiceType().toString());
                    removableUnitPanel.selectType(serviceStateDescription.getUnitType());
                }
            });
            
            CachedLocationRegistryRemote.waitForData();
            CachedUnitRegistryRemote.waitForData();
            
            GlobalCachedExecutorService.submit(() -> {
                try {
                    genericUnitCollectionPanel.init();
                    sceneSelectorPanel.init();
                    sceneCreationPanel.init();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
            return this;
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
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
        sceneCreationPanel = new org.openbase.bco.manager.scene.visual.SceneCreationPanel();
        sceneSelectorPanel = new org.openbase.bco.manager.scene.visual.SceneSelectorPanel();
        genericUnitCollectionPanel = new org.openbase.bco.dal.visual.unit.GenericUnitCollectionPanel();
        saveButton = new javax.swing.JButton();
        statusPanel1 = new org.openbase.bco.dal.visual.util.StatusPanel();

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
        List<ServiceStateDescription> actionConfigs = new ArrayList<>();
        // Generic unit panel means removable generic unit panel in this case
        for (GenericUnitPanel genericUnitPanel : unitPanelList) {
            RemovableGenericUnitPanel removableUnitPanel = (RemovableGenericUnitPanel) genericUnitPanel;
            GenericUnitPanel unitPanel = removableUnitPanel.getGenericUnitPanel();
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
                    ServiceStateDescription.Builder actionConfig = ServiceStateDescription.newBuilder().setServiceType(panel.getServiceType()).setUnitId(panel.getUnitId());
                    Object value = getServiceValue(panel.getOperationService(), panel.getServiceType());
                    actionConfig.setServiceAttribute(serviceJSonProcessor.serialize(value));
                    actionConfig.setServiceAttributeType(serviceJSonProcessor.getServiceAttributeType(value));
                    actionConfig.setUnitType(removableUnitPanel.getSelectedUnitType());
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
            java.util.logging.Logger.getLogger(SceneEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SceneEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SceneEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SceneEditor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        JPService.setApplicationName("scene-remote");
        JPService.parseAndExitOnError(args);

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            try {
                new SceneEditor().init().setVisible(true);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
                System.exit(1);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.openbase.bco.dal.visual.unit.GenericUnitCollectionPanel genericUnitCollectionPanel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton saveButton;
    private org.openbase.bco.manager.scene.visual.SceneCreationPanel sceneCreationPanel;
    private org.openbase.bco.manager.scene.visual.SceneSelectorPanel sceneSelectorPanel;
    private org.openbase.bco.dal.visual.util.StatusPanel statusPanel1;
    // End of variables declaration//GEN-END:variables
}
