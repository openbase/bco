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
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openbase.bco.dal.remote.unit.scene.SceneRemote;
import org.openbase.bco.dal.visual.util.SelectorPanel.LocationUnitConfigHolder;
import org.openbase.bco.dal.visual.util.StatusPanel;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.scene.remote.SceneRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.SceneRegistryDataType.SceneRegistryData;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.state.EnablingStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.scene.SceneConfigType.SceneConfig;

public class SceneCreationPanel extends javax.swing.JPanel {

    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(SceneCreationPanel.class);

    private final ObservableImpl<SceneConfig> observable;
    private SceneRegistryRemote sceneRegistryRemote;
    private UnitConfig lastSelected = null;
    private LocationUnitConfigHolder location = null;

    /**
     * Creates new form SceneCreationPanel
     *
     */
    public SceneCreationPanel() {
        initComponents();
        observable = new ObservableImpl<>();
    }

    public void init() throws CouldNotPerformException, InterruptedException {
        sceneRegistryRemote = Registries.getSceneRegistry();
        StatusPanel.getInstance().setStatus("Wait for scene registry data...", StatusPanel.StatusType.INFO, sceneRegistryRemote.getDataFuture());
        sceneRegistryRemote.waitForData();
        StatusPanel.getInstance().setStatus("Scene registry loaded.", StatusPanel.StatusType.INFO, 3);
        initDynamicComponents();
        updateDynamicComponents();
    }

    private void initDynamicComponents() throws CouldNotPerformException, InitializationException, InterruptedException {
        sceneRegistryRemote.addDataObserver((final Observable<SceneRegistryData> source, SceneRegistryData data) -> {
            updateDynamicComponents();
        });
        locationSelectorPanel.addObserver((final Observable<LocationUnitConfigHolder> source, LocationUnitConfigHolder data) -> {
            location = data;
            logger.info("location update:" + location);
        });
        // init locationSelectorPanel after registering the observer so that the selected location is recieved from the start
        locationSelectorPanel.init(false);
    }

    private void updateDynamicComponents() throws CouldNotPerformException {

        if (!sceneRegistryRemote.isDataAvailable()) {
            return;
        }

        ArrayList<SceneUnitConfigHolder> sceneConfigHolderList = new ArrayList<>();
        for (final UnitConfig sceneUnitConfig : sceneRegistryRemote.getSceneConfigs()) {
            sceneConfigHolderList.add(new SceneUnitConfigHolder(sceneUnitConfig));
        }

        if (sceneConfigHolderList.isEmpty()) {
            sceneSelectionComboBox.setEnabled(false);
            return;
        } else {
            sceneSelectionComboBox.setEnabled(true);
        }

        Collections.sort(sceneConfigHolderList);
        sceneSelectionComboBox.setModel(new DefaultComboBoxModel(sceneConfigHolderList.toArray()));
        if (lastSelected == null) {
            sceneSelectionComboBox.setSelectedIndex(0);
            lastSelected = ((SceneUnitConfigHolder) sceneSelectionComboBox.getSelectedItem()).getConfig();
            observable.notifyObservers(lastSelected.getSceneConfig());
        } else {
            sceneSelectionComboBox.setSelectedItem(new SceneUnitConfigHolder(lastSelected));
        }
    }

    public void updateSceneConfig(List<ServiceStateDescription> servicestateDescription) throws CouldNotPerformException {
        UnitConfig.Builder sceneUnitConfig = ((SceneUnitConfigHolder) sceneSelectionComboBox.getSelectedItem()).getConfig().toBuilder();
        sceneUnitConfig.getSceneConfigBuilder().clearRequiredServiceStateDescription();
        sceneUnitConfig.getSceneConfigBuilder().clearOptionalServiceStateDescription();
        sceneUnitConfig.getSceneConfigBuilder().addAllRequiredServiceStateDescription(servicestateDescription);
        sceneUnitConfig.getPlacementConfigBuilder().setLocationId(location.getConfig().getId());
        logger.info("save location:" + location.getConfig().getLabel());
        try {
            if (!sceneRegistryRemote.containsSceneConfig(sceneUnitConfig.build())) {
                logger.debug("Registering scene from updateSceneConfig");
                lastSelected = sceneRegistryRemote.registerSceneConfig(sceneUnitConfig.build()).get();
            } else {
                lastSelected = sceneRegistryRemote.updateSceneConfig(sceneUnitConfig.build()).get();
            }
        } catch (ExecutionException | InterruptedException ex) {
            throw new CouldNotPerformException("Could not register/update scene", ex);
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

        sceneSelectionComboBox = new javax.swing.JComboBox();
        newButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        locationSelectorPanel = new org.openbase.bco.manager.scene.visual.LocationSelectorPanel();
        applyUpdateButton = new javax.swing.JButton();

        sceneSelectionComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sceneSelectionComboBoxActionPerformed(evt);
            }
        });

        newButton.setText("New");
        newButton.setMaximumSize(new java.awt.Dimension(81, 25));
        newButton.setMinimumSize(new java.awt.Dimension(81, 25));
        newButton.setPreferredSize(new java.awt.Dimension(81, 25));
        newButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newButtonActionPerformed(evt);
            }
        });

        deleteButton.setText("Delete");
        deleteButton.setMaximumSize(new java.awt.Dimension(80, 25));
        deleteButton.setMinimumSize(new java.awt.Dimension(80, 25));
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });

        applyUpdateButton.setText("Apply Scene");
        applyUpdateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyUpdateButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(locationSelectorPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(sceneSelectionComboBox, 0, 394, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(newButton, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(applyUpdateButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sceneSelectionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(newButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deleteButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(locationSelectorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(applyUpdateButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void sceneSelectionComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sceneSelectionComboBoxActionPerformed
        //return if no new scene has been selected
        if (sceneSelectionComboBox.getSelectedIndex() == -1
                || (lastSelected != null && lastSelected.getId().equals(((SceneUnitConfigHolder) sceneSelectionComboBox.getSelectedItem()).getConfig().getId()))) {
            return;
        }

        lastSelected = ((SceneUnitConfigHolder) sceneSelectionComboBox.getSelectedItem()).getConfig();
        locationSelectorPanel.updateSelection(lastSelected.getPlacementConfig().getLocationId());
        try {
            observable.notifyObservers(lastSelected.getSceneConfig());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not notify observers!", ex), logger, LogLevel.WARN);
        }
    }//GEN-LAST:event_sceneSelectionComboBoxActionPerformed

    private void newButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newButtonActionPerformed
        if (location == null) {
            logger.error("You have to select a location first!");
            return;
        }
        String label = JOptionPane.showInputDialog(this, "Enter scene label");
        //is null if cancel has been pressed
        if (label == null) {
            return;
        }
        try {
            logger.info("Registering scene from new button");
            UnitConfig.Builder unitConfig = UnitConfig.newBuilder().setLabel(label);
            unitConfig.setType(UnitTemplateType.UnitTemplate.UnitType.SCENE);
            unitConfig.getPlacementConfigBuilder().setLocationId(location.getConfig().getId());
            unitConfig.setEnablingState(EnablingStateType.EnablingState.newBuilder().setValue(EnablingStateType.EnablingState.State.ENABLED));
            unitConfig.setSceneConfig(SceneConfig.getDefaultInstance());
            lastSelected = sceneRegistryRemote.registerSceneConfig(unitConfig.build()).get();
            updateDynamicComponents();
            observable.notifyObservers(lastSelected.getSceneConfig());
        } catch (CouldNotPerformException | InterruptedException | ExecutionException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
    }//GEN-LAST:event_newButtonActionPerformed

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
        UnitConfig sceneUnitConfig = ((SceneUnitConfigHolder) sceneSelectionComboBox.getSelectedItem()).getConfig();
        if (sceneUnitConfig.hasId() && !sceneUnitConfig.getId().isEmpty()) {
            try {
                lastSelected = null;
                sceneRegistryRemote.removeSceneConfig(sceneUnitConfig);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
            }
        }
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void applyUpdateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyUpdateButtonActionPerformed
        if (lastSelected == null) {
            return;
        }

        // TODO: add statusPanel update?
        logger.info("Apply update...");
        SceneRemote sceneRemote = new SceneRemote();
        try {
            sceneRemote.init(lastSelected);
            applyUpdateButton.setBackground(Color.GRAY);
            sceneRemote.activate();
            sceneRemote.waitForData();
            try {
                sceneRemote.setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build()).get();
            } catch (ExecutionException ex) {
                throw new CouldNotPerformException("Could not activate scene [" + sceneRemote.getLabel() + "]", ex);
            }
        } catch (InterruptedException | CouldNotPerformException ex) {
            logger.warn("Could not apply update. Initialization and activation of scene remote failed!");
        }
    }//GEN-LAST:event_applyUpdateButtonActionPerformed

    public void addObserver(Observer<SceneConfig> observer) {
        observable.addObserver(observer);
    }

    public void removeObserver(Observer<SceneConfig> observer) {
        observable.removeObserver(observer);
    }

    private static class SceneUnitConfigHolder implements Comparable<SceneUnitConfigHolder> {

        private final UnitConfig config;

        public SceneUnitConfigHolder(UnitConfig config) {
            this.config = config;
        }

        @Override
        public String toString() {
            if (isNotSpecified()) {
                return "New";
            }
            return config.getLabel();
        }

        public boolean isNotSpecified() {
            return config == null;
        }

        public UnitConfig getConfig() {
            return config;
        }

        public String getSceneId() throws CouldNotPerformException {
            try {
                if (config == null) {
                    throw new NotAvailableException("messageOrBuilder");
                }
                String id = config.getId();
                if (id.isEmpty()) {
                    throw new VerificationFailedException("Detected id is empty!");
                }
                return id;
            } catch (NotAvailableException | VerificationFailedException ex) {
                throw new CouldNotPerformException("Could not detect id.", ex);
            }
        }

        @Override
        public int compareTo(SceneUnitConfigHolder o) {
            return toString().compareTo(o.toString());
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof SceneUnitConfigHolder) {
                final SceneUnitConfigHolder other = (SceneUnitConfigHolder) obj;
                try {
                    return new EqualsBuilder()
                            .append(getSceneId(), other.getSceneId())
                            .isEquals();
                } catch (CouldNotPerformException ex) {
                    return new EqualsBuilder()
                            .append(getConfig(), other.getConfig())
                            .isEquals();
                }
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            try {
                return new HashCodeBuilder()
                        .append(getSceneId())
                        .toHashCode();
            } catch (CouldNotPerformException ex) {
                return new HashCodeBuilder()
                        .append(config)
                        .toHashCode();
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton applyUpdateButton;
    private javax.swing.JButton deleteButton;
    private org.openbase.bco.manager.scene.visual.LocationSelectorPanel locationSelectorPanel;
    private javax.swing.JButton newButton;
    private javax.swing.JComboBox sceneSelectionComboBox;
    // End of variables declaration//GEN-END:variables
}
