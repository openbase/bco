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
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingUtilities;
import static org.openbase.bco.dal.visual.util.SelectorPanel.ALL_LOCATION;
import static org.openbase.bco.dal.visual.util.SelectorPanel.ALL_Service;
import static org.openbase.bco.dal.visual.util.SelectorPanel.ALL_UNIT;
import org.openbase.bco.dal.visual.util.SelectorPanel.LocationUnitConfigHolder;
import org.openbase.bco.dal.visual.util.SelectorPanel.ServiceTypeHolder;
import org.openbase.bco.dal.visual.util.SelectorPanel.UnitConfigHolder;
import org.openbase.bco.dal.visual.util.SelectorPanel.UnitTypeHolder;
import org.openbase.bco.dal.visual.util.StatusPanel;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.LocationRegistryDataType.LocationRegistryData;
import rst.domotic.registry.UnitRegistryDataType;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class SceneSelectorPanel extends javax.swing.JPanel {

    protected static final Logger logger = LoggerFactory.getLogger(SceneSelectorPanel.class);

    private StatusPanel statusPanel;

    private LocationUnitConfigHolder selectedLocationConfigHolder;
    private UnitConfigHolder selectedUnitConfigHolder;
    private ServiceTypeHolder selectedServiceTypeHolder;

    private final ObservableImpl<ServiceTypeHolder> unitConfigServiceTypeObservable;

    private boolean init = false;

    private final ReentrantReadWriteLock updateComponentLock;

    /**
     * Creates new form SceneSelectorPanel
     *
     */
    public SceneSelectorPanel() {
        this.unitConfigServiceTypeObservable = new ObservableImpl<>();
        this.updateComponentLock = new ReentrantReadWriteLock();
        this.initComponents();
        this.setEnable(false);
        this.initDynamicComponents();
    }

    private void setEnable(final boolean value) {
        locationPanel.setEnabled(value);
        unitPanel.setEnabled(value);
        servicePanel.setEnabled(value);
        instancePanel.setEnabled(value);
        locationComboBox.setEnabled(value);
        unitTypeComboBox.setEnabled(value);
        unitConfigComboBox.setEnabled(value);
        serviceTypeComboBox.setEnabled(false);
        selectedServiceTypeComboBox.setEnabled(value);
    }

    public void init() throws InitializationException, InterruptedException, CouldNotPerformException {
        statusPanel = StatusPanel.getInstance();

        statusPanel.setStatus("Wait for unit registry...", StatusPanel.StatusType.INFO, true);
        Registries.getUnitRegistry().waitForData();
        statusPanel.setStatus("Wait for location registry...", StatusPanel.StatusType.INFO, true);
        Registries.getLocationRegistry().waitForData();
        statusPanel.setStatus("Connection established.", StatusPanel.StatusType.INFO, 3);

        // register change observer
        Registries.getUnitRegistry().addDataObserver((Observable<UnitRegistryDataType.UnitRegistryData> source, UnitRegistryDataType.UnitRegistryData data) -> {
            SwingUtilities.invokeLater(() -> {
                updateDynamicComponents();
            });
        });

        Registries.getLocationRegistry().addDataObserver((Observable<LocationRegistryData> source, LocationRegistryData data) -> {
            SwingUtilities.invokeLater(() -> {
                updateDynamicComponents();
            });
        });

        init = true;

        setEnable(true);
        updateDynamicComponents();
    }

    private void initDynamicComponents() {
        // init unit types
        ArrayList<UnitTypeHolder> unitTypeHolderList = new ArrayList<>();
        for (UnitType type : UnitTemplateType.UnitTemplate.UnitType.values()) {
            unitTypeHolderList.add(new UnitTypeHolder(type));
        }
        Collections.sort(unitTypeHolderList);
        SwingUtilities.invokeLater(() -> {
            unitTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(unitTypeHolderList.toArray()));
        });

        // init service types
        ArrayList<ServiceTypeHolder> serviceTypeHolderList = new ArrayList<>();
        for (ServiceType type : ServiceType.values()) {
            serviceTypeHolderList.add(new ServiceTypeHolder(type));
        }
        Collections.sort(serviceTypeHolderList);
        SwingUtilities.invokeLater(() -> {
            serviceTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(serviceTypeHolderList.toArray()));
            selectedServiceTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(serviceTypeHolderList.toArray()));
        });
    }

    private void updateDynamicComponents() {
        MultiException.ExceptionStack exceptionStack = null;

        updateComponentLock.writeLock().lock();
        try {
            if (!init) {
                return;
            }
            logger.debug("Update selectorPanel!");
            try {
                if (locationComboBox.getSelectedIndex() != -1) {
                    selectedLocationConfigHolder = (LocationUnitConfigHolder) locationComboBox.getSelectedItem();
                } else {
                    selectedLocationConfigHolder = ALL_LOCATION;
                }
            } catch (Exception ex) {
                selectedLocationConfigHolder = ALL_LOCATION;
                ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
            }

            try {
                if (unitConfigComboBox.getSelectedIndex() != -1) {
                    selectedUnitConfigHolder = (UnitConfigHolder) unitConfigComboBox.getSelectedItem();
                } else {
                    selectedUnitConfigHolder = ALL_UNIT;
                }
            } catch (Exception ex) {
                selectedUnitConfigHolder = ALL_UNIT;
                ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
            }

            try {
                if (serviceTypeComboBox.getSelectedIndex() != -1) {
                    selectedServiceTypeHolder = (ServiceTypeHolder) selectedServiceTypeComboBox.getSelectedItem();
                } else {
                    selectedServiceTypeHolder = ALL_Service;
                }
            } catch (Exception ex) {
                selectedServiceTypeHolder = ALL_Service;
                ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
            }

            try {
                ArrayList<LocationUnitConfigHolder> locationConfigHolderList = new ArrayList<>();
                locationConfigHolderList.add(ALL_LOCATION);
                for (UnitConfig locationUnitConfig : Registries.getLocationRegistry().getLocationConfigs()) {
                    locationConfigHolderList.add(new LocationUnitConfigHolder(locationUnitConfig));
                }
                Collections.sort(locationConfigHolderList);
                locationComboBox.setModel(new DefaultComboBoxModel(locationConfigHolderList.toArray()));

                int selectedLocationIndex = Collections.binarySearch(locationConfigHolderList, selectedLocationConfigHolder);
                if (selectedLocationIndex >= 0) {
                    locationComboBox.setSelectedItem(locationConfigHolderList.get(selectedLocationIndex));
                }

                locationComboBox.setEnabled(locationConfigHolderList.size() > 0);
            } catch (CouldNotPerformException ex) {
                locationComboBox.setEnabled(false);
                ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
            }

            try {
                ArrayList<UnitConfigHolder> unitConfigHolderList = new ArrayList<>();
                UnitType selectedUnitType = ((UnitTypeHolder) unitTypeComboBox.getSelectedItem()).getType();
                if (selectedUnitType == UnitType.UNKNOWN) {
                    if (unitConfigComboBox.isEnabled() && selectedLocationConfigHolder != null && !selectedLocationConfigHolder.isNotSpecified()) {
                        for (UnitConfig config : Registries.getLocationRegistry().getUnitConfigsByLocation(selectedLocationConfigHolder.getConfig().getId())) {
                            unitConfigHolderList.add(new UnitConfigHolder(config, Registries.getLocationRegistry().getLocationConfigById(config.getPlacementConfig().getLocationId())));
                        }
                    } else {
                        for (UnitConfig config : Registries.getUnitRegistry().getUnitConfigs()) {
                            try {
                                // ignore non installed units
                                if (!config.getEnablingState().getValue().equals(EnablingState.State.ENABLED)) {
                                    continue;
                                }
                                if (config.getPlacementConfig().getLocationId().isEmpty()) {
                                    throw new InvalidStateException("Could not load location unit of " + config.getLabel() + " because its not configured!");
                                }
                                unitConfigHolderList.add(new UnitConfigHolder(config, Registries.getLocationRegistry().getLocationConfigById(config.getPlacementConfig().getLocationId())));
                            } catch (CouldNotPerformException ex) {
                                exceptionStack = MultiException.push(this, ex, exceptionStack);
                            }
                        }
                    }
                } else {
                    if (unitConfigComboBox.isEnabled() && selectedLocationConfigHolder != null && !selectedLocationConfigHolder.isNotSpecified()) {
                        for (UnitConfig config : Registries.getLocationRegistry().getUnitConfigsByLocation(selectedUnitType, selectedLocationConfigHolder.getConfig().getId())) {
                            try {
                                unitConfigHolderList.add(new UnitConfigHolder(config, Registries.getLocationRegistry().getLocationConfigById(config.getPlacementConfig().getLocationId())));
                            } catch (CouldNotPerformException ex) {
                                exceptionStack = MultiException.push(this, ex, exceptionStack);
                            }
                        }
                    } else {
                        for (UnitConfig config : Registries.getUnitRegistry().getUnitConfigs(selectedUnitType)) {
                            try {
                                // ignore non installed units
                                if (!config.getEnablingState().getValue().equals(EnablingState.State.ENABLED)) {
                                    continue;
                                }
                                unitConfigHolderList.add(new UnitConfigHolder(config, Registries.getLocationRegistry().getLocationConfigById(config.getPlacementConfig().getLocationId())));
                            } catch (CouldNotPerformException ex) {
                                exceptionStack = MultiException.push(this, ex, exceptionStack);
                            }
                        }
                    }
                }
                Collections.sort(unitConfigHolderList);
                unitConfigComboBox.setModel(new DefaultComboBoxModel(unitConfigHolderList.toArray()));
                if (selectedUnitConfigHolder != null) {
                    int selectedUnitIndex = Collections.binarySearch(unitConfigHolderList, selectedUnitConfigHolder);
                    if (selectedUnitIndex >= 0) {
                        unitConfigComboBox.setSelectedItem(unitConfigHolderList.get(selectedUnitIndex));
                    }
                }
                unitConfigComboBox.setEnabled(unitConfigHolderList.size() > 0);
            } catch (CouldNotPerformException ex) {
                unitConfigComboBox.setEnabled(false);
                throw ex;
            }

            try {
                ArrayList<ServiceTypeHolder> serviceTypeHolderList = new ArrayList<>();
                UnitType selectedUnitType = ((UnitTypeHolder) unitTypeComboBox.getSelectedItem()).getType();
                serviceTypeHolderList.add(ALL_Service);
                for (ServiceDescription serviceDescription : Registries.getUnitRegistry().getUnitTemplateByType(selectedUnitType).getServiceDescriptionList()) {
                    if (serviceDescription.getPattern() != ServiceTemplate.ServicePattern.OPERATION) {
                        continue;
                    }

                    serviceTypeHolderList.add(new ServiceTypeHolder(serviceDescription.getType()));
                }
                Collections.sort(serviceTypeHolderList);
                selectedServiceTypeComboBox.setModel(new DefaultComboBoxModel(serviceTypeHolderList.toArray()));

                int selectedLocationIndex = Collections.binarySearch(serviceTypeHolderList, selectedServiceTypeHolder);
                if (selectedLocationIndex >= 0) {
                    selectedServiceTypeComboBox.setSelectedItem(serviceTypeHolderList.get(selectedLocationIndex));
                }

                selectedServiceTypeComboBox.setEnabled(serviceTypeHolderList.size() > 0);
            } catch (CouldNotPerformException ex) {
                selectedServiceTypeComboBox.setEnabled(false);
                ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
            }

            MultiException.checkAndThrow("Could not acquire all informations!", exceptionStack);
        } catch (CouldNotPerformException | NullPointerException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update all dynamic components!", ex), logger);
        } catch (InterruptedException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Component update interrupted.", ex), logger, LogLevel.WARN);
            Thread.currentThread().interrupt();
        } finally {
            updateComponentLock.writeLock().unlock();
        }
    }

    public void addObserver(Observer<ServiceTypeHolder> observer) {
        unitConfigServiceTypeObservable.addObserver(observer);
    }

    public void removeObserver(Observer<ServiceTypeHolder> observer) {
        unitConfigServiceTypeObservable.removeObserver(observer);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel4 = new javax.swing.JPanel();
        unitPanel = new javax.swing.JPanel();
        unitTypeComboBox = new javax.swing.JComboBox();
        locationPanel = new javax.swing.JPanel();
        locationComboBox = new javax.swing.JComboBox();
        servicePanel = new javax.swing.JPanel();
        serviceTypeComboBox = new javax.swing.JComboBox();
        instancePanel = new javax.swing.JPanel();
        unitConfigComboBox = new javax.swing.JComboBox();
        selectedServiceTypeComboBox = new javax.swing.JComboBox();
        addButton = new javax.swing.JButton();

        unitPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Unit"));

        unitTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "AmbientLight" }));
        unitTypeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unitTypeComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout unitPanelLayout = new javax.swing.GroupLayout(unitPanel);
        unitPanel.setLayout(unitPanelLayout);
        unitPanelLayout.setHorizontalGroup(
            unitPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, unitPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(unitTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        unitPanelLayout.setVerticalGroup(
            unitPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(unitPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(unitTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        locationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Location"));

        locationComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                locationComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout locationPanelLayout = new javax.swing.GroupLayout(locationPanel);
        locationPanel.setLayout(locationPanelLayout);
        locationPanelLayout.setHorizontalGroup(
            locationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(locationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(locationComboBox, 0, 244, Short.MAX_VALUE)
                .addContainerGap())
        );
        locationPanelLayout.setVerticalGroup(
            locationPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, locationPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(locationComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        servicePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Service"));

        serviceTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout servicePanelLayout = new javax.swing.GroupLayout(servicePanel);
        servicePanel.setLayout(servicePanelLayout);
        servicePanelLayout.setHorizontalGroup(
            servicePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(servicePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(serviceTypeComboBox, 0, 256, Short.MAX_VALUE)
                .addContainerGap())
        );
        servicePanelLayout.setVerticalGroup(
            servicePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, servicePanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(serviceTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        instancePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected Instance"));

        unitConfigComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unitConfigComboBoxActionPerformed(evt);
            }
        });

        selectedServiceTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        addButton.setText("Add");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout instancePanelLayout = new javax.swing.GroupLayout(instancePanel);
        instancePanel.setLayout(instancePanelLayout);
        instancePanelLayout.setHorizontalGroup(
            instancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(instancePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(unitConfigComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 492, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectedServiceTypeComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(addButton)
                .addContainerGap())
        );
        instancePanelLayout.setVerticalGroup(
            instancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, instancePanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(instancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(unitConfigComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(selectedServiceTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(instancePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(locationPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(unitPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(servicePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(unitPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(locationPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(servicePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(instancePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Registry", jPanel4);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void unitTypeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unitTypeComboBoxActionPerformed
        updateDynamicComponents();
    }//GEN-LAST:event_unitTypeComboBoxActionPerformed

    private void locationComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_locationComboBoxActionPerformed
        updateDynamicComponents();
    }//GEN-LAST:event_locationComboBoxActionPerformed

    private void unitConfigComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unitConfigComboBoxActionPerformed
    }//GEN-LAST:event_unitConfigComboBoxActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        final UnitConfigHolder unitConfigHolder = (UnitConfigHolder) unitConfigComboBox.getSelectedItem();
        final UnitConfig selectedUnitConfig = unitConfigHolder.getConfig();
        final ServiceTypeHolder serviceTypeHolder = (ServiceTypeHolder) selectedServiceTypeComboBox.getSelectedItem();
        final ServiceType serviceType = serviceTypeHolder.getServiceType();
        try {
            unitConfigServiceTypeObservable.notifyObservers(new ServiceTypeHolder(serviceType, selectedUnitConfig));
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
    }//GEN-LAST:event_addButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JPanel instancePanel;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JComboBox locationComboBox;
    private javax.swing.JPanel locationPanel;
    private javax.swing.JComboBox selectedServiceTypeComboBox;
    private javax.swing.JPanel servicePanel;
    private javax.swing.JComboBox serviceTypeComboBox;
    private javax.swing.JComboBox unitConfigComboBox;
    private javax.swing.JPanel unitPanel;
    private javax.swing.JComboBox unitTypeComboBox;
    // End of variables declaration//GEN-END:variables
}
