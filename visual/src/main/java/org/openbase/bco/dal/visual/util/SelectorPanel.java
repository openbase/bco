package org.openbase.bco.dal.visual.util;

/*
 * #%L
 * BCO DAL Visualisation
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingUtilities;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.LocationRegistryDataType.LocationRegistryData;
import rst.domotic.registry.UnitRegistryDataType;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType.Scope;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class SelectorPanel extends javax.swing.JPanel {

    protected static final Logger logger = LoggerFactory.getLogger(SelectorPanel.class);

    private StatusPanel statusPanel;

    private LocationUnitConfigHolder selectedLocationConfigHolder;
    private UnitConfigHolder selectedUnitConfigHolder;

    private ObservableImpl<UnitConfig> unitConfigObservable;

    private boolean init = false;

    private Future currentTask;

    private final ReentrantReadWriteLock updateComponentLock;

    /**
     * Creates new form SelectorPanel
     *
     * @throws org.openbase.jul.exception.InstantiationException
     */
    public SelectorPanel() throws InstantiationException {
        this.unitConfigObservable = new ObservableImpl<>();
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
        scopeTextField.setEnabled(value);
        scopeCancelButton.setEnabled(value);
        scopeApplyButton.setEnabled(value);
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
        Collections.sort(unitTypeHolderList);
        SwingUtilities.invokeLater(() -> {
            serviceTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(serviceTypeHolderList.toArray()));
        });
    }

    private synchronized void updateDynamicComponents() {
        MultiException.ExceptionStack exceptionStack = null;

        updateComponentLock.writeLock().lock();
        try {
            if (!init) {
                return;
            }
            logger.debug("Update selectorPanel!");
            try {
                selectedLocationConfigHolder = (LocationUnitConfigHolder) locationComboBox.getSelectedItem();
                if (selectedLocationConfigHolder == null) {
                    selectedLocationConfigHolder = ALL_LOCATION;
                }
            } catch (Exception ex) {
                selectedLocationConfigHolder = ALL_LOCATION;
                ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
            }

            try {
                selectedUnitConfigHolder = (UnitConfigHolder) unitConfigComboBox.getSelectedItem();
            } catch (Exception ex) {
                selectedUnitConfigHolder = null;
                ExceptionPrinter.printHistory(ex, logger);
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
                ExceptionPrinter.printHistory(ex, logger);
            }

            try {
                ArrayList<UnitConfigHolder> unitConfigHolderList = new ArrayList<>();
                UnitType selectedUnitType = ((UnitTypeHolder) unitTypeComboBox.getSelectedItem()).getType();
                if (selectedUnitType == UnitType.UNKNOWN) {
                    if (selectedLocationConfigHolder != null && !selectedLocationConfigHolder.isNotSpecified()) {
                        for (final UnitConfig config : Registries.getLocationRegistry().getUnitConfigsByLocation(selectedLocationConfigHolder.getConfig().getId())) {
                            try {
                                unitConfigHolderList.add(new UnitConfigHolder(config, Registries.getLocationRegistry().getLocationConfigById(config.getPlacementConfig().getLocationId())));
                            } catch (CouldNotPerformException ex) {
                                exceptionStack = MultiException.push(this, ex, exceptionStack);
                            }
                        }
                    } else {
                        for (UnitConfig config : Registries.getUnitRegistry().getUnitConfigs()) {
                            try {
                                // ignore disabled units
                                if (config.getEnablingState().getValue() != EnablingState.State.ENABLED) {
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
                } else if (selectedLocationConfigHolder != null && !selectedLocationConfigHolder.isNotSpecified()) {
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
                            // ignore disabled units
                            if (config.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                                continue;
                            }
                            unitConfigHolderList.add(new UnitConfigHolder(config, Registries.getLocationRegistry().getLocationConfigById(config.getPlacementConfig().getLocationId())));
                        } catch (CouldNotPerformException ex) {
                            exceptionStack = MultiException.push(this, ex, exceptionStack);
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
            updateRemotePanel();
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

    public void addObserver(Observer<UnitConfig> observer) {
        unitConfigObservable.addObserver(observer);
    }

    public void removeObserver(Observer<UnitConfig> observer) {
        unitConfigObservable.removeObserver(observer);
    }

    private UnitConfig loadedUnitConfig;

    private synchronized void updateRemotePanel() {
        if (updateComponentLock.writeLock().tryLock()) {
            try {
                final UnitConfigHolder unitConfigHolder = (UnitConfigHolder) unitConfigComboBox.getSelectedItem();

                if (unitConfigHolder == null) {
                    // no config available.
                    return;
                }

                final UnitConfig selectedUnitConfig = unitConfigHolder.getConfig();

                if (loadedUnitConfig == selectedUnitConfig) {
                    // selection has not been changed.
                    return;
                }
                statusPanel.setStatus("Load new remote control " + unitConfigHolder + "...", StatusPanel.StatusType.INFO, executeSingleTask((Callable<Void>) () -> {
                    try {
                        unitConfigComboBox.setForeground(Color.BLACK);
                        unitConfigObservable.notifyObservers(selectedUnitConfig);
                        scopeTextField.setText(ScopeGenerator.generateStringRep(selectedUnitConfig.getScope()));
                    } catch (MultiException ex) {
                        unitConfigComboBox.setForeground(Color.RED);
                        statusPanel.setError(ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger));
                    }
                    updateButtonStates();
                    return null;
                }));

                loadedUnitConfig = selectedUnitConfig;
            } catch (Exception ex) {
                statusPanel.setError(ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not update remote panel!", ex), logger));
            } finally {
                updateComponentLock.writeLock().unlock();
            }
        }
    }

    private synchronized Future executeSingleTask(final Callable task) throws CouldNotPerformException {
        if (currentTask != null) {
            currentTask.cancel(true);
        }

        currentTask = GlobalCachedExecutorService.submit(task);
        return currentTask;
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
        jPanel5 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        scopeTextField = new javax.swing.JTextField();
        scopeApplyButton = new javax.swing.JButton();
        scopeCancelButton = new javax.swing.JButton();

        unitPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Unit"));

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

        unitConfigComboBox.setMaximumRowCount(40);
        unitConfigComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                unitConfigComboBoxItemStateChanged(evt);
            }
        });
        unitConfigComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unitConfigComboBoxActionPerformed(evt);
            }
        });
        unitConfigComboBox.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                unitConfigComboBoxPropertyChange(evt);
            }
        });

        javax.swing.GroupLayout instancePanelLayout = new javax.swing.GroupLayout(instancePanel);
        instancePanel.setLayout(instancePanelLayout);
        instancePanelLayout.setHorizontalGroup(
            instancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(instancePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(unitConfigComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        instancePanelLayout.setVerticalGroup(
            instancePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, instancePanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(unitConfigComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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

        java.awt.FlowLayout flowLayout1 = new java.awt.FlowLayout();
        flowLayout1.setAlignOnBaseline(true);
        jPanel5.setLayout(flowLayout1);

        jLabel1.setText("Scope");

        scopeTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                scopeTextFieldFocusLost(evt);
            }
        });
        scopeTextField.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
            }
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
                scopeTextFieldCaretPositionChanged(evt);
            }
        });
        scopeTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scopeTextFieldActionPerformed(evt);
            }
        });
        scopeTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                scopeTextFieldKeyTyped(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                scopeTextFieldKeyReleased(evt);
            }
        });

        scopeApplyButton.setText("Apply");
        scopeApplyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scopeApplyButtonActionPerformed(evt);
            }
        });

        scopeCancelButton.setText("Cancel");
        scopeCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scopeCancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(scopeTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 819, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(scopeCancelButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(scopeApplyButton)))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(scopeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(scopeApplyButton)
                    .addComponent(scopeCancelButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.add(jPanel6);

        jTabbedPane1.addTab("Scope", jPanel5);

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

    private void scopeCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scopeCancelButtonActionPerformed
        try {
            scopeTextField.setText(ScopeGenerator.generateStringRep(unitConfigObservable.getValue().getScope()));
        } catch (CouldNotPerformException | NullPointerException ex) {
            scopeTextField.setText("");
        }
        updateButtonStates();
    }//GEN-LAST:event_scopeCancelButtonActionPerformed

    private void scopeTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scopeTextFieldActionPerformed
        if (scopeApplyButton.isEnabled()) {
            scopeApplyButtonActionPerformed(evt);
        }
    }//GEN-LAST:event_scopeTextFieldActionPerformed

    private void locationComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_locationComboBoxActionPerformed
        updateDynamicComponents();
    }//GEN-LAST:event_locationComboBoxActionPerformed

    private void unitConfigComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unitConfigComboBoxActionPerformed
        updateRemotePanel();
    }//GEN-LAST:event_unitConfigComboBoxActionPerformed

    private void scopeApplyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scopeApplyButtonActionPerformed
        try {
            statusPanel.setStatus("Load new remote control " + scopeTextField.getText().toLowerCase() + "...", StatusPanel.StatusType.INFO, executeSingleTask(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        scopeTextField.setForeground(Color.BLACK);
                        Scope scope = ScopeTransformer.transform(new rsb.Scope(scopeTextField.getText().toLowerCase()));
                        unitConfigObservable.notifyObservers(Registries.getUnitRegistry().getUnitConfigByScope(scope));
                        scopeTextField.setText(ScopeGenerator.generateStringRep(unitConfigObservable.getValue().getScope()));
                    } catch (CouldNotPerformException ex) {
                        scopeTextField.setForeground(Color.RED);
                        statusPanel.setError(ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger));
                    }
                    return null;
                }
            }));
            updateButtonStates();
        } catch (CouldNotPerformException | NullPointerException ex) {
            statusPanel.setError(ExceptionPrinter.printHistoryAndReturnThrowable(new CouldNotPerformException("Could not load new remote!", ex), logger));
        }
    }//GEN-LAST:event_scopeApplyButtonActionPerformed

    private void scopeTextFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_scopeTextFieldKeyTyped

    }//GEN-LAST:event_scopeTextFieldKeyTyped

    private void scopeTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_scopeTextFieldKeyReleased
        updateButtonStates();
    }//GEN-LAST:event_scopeTextFieldKeyReleased

    private void scopeTextFieldCaretPositionChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_scopeTextFieldCaretPositionChanged
    }//GEN-LAST:event_scopeTextFieldCaretPositionChanged

    private void scopeTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_scopeTextFieldFocusLost
        scopeTextField.setText(scopeTextField.getText().toLowerCase());
        updateButtonStates();
    }//GEN-LAST:event_scopeTextFieldFocusLost

    private void unitConfigComboBoxPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_unitConfigComboBoxPropertyChange
        if (evt.getPropertyName().equals("model")) {
            updateRemotePanel();
        }
    }//GEN-LAST:event_unitConfigComboBoxPropertyChange

    private void unitConfigComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_unitConfigComboBoxItemStateChanged
        updateRemotePanel();
    }//GEN-LAST:event_unitConfigComboBoxItemStateChanged

    public void updateButtonStates() {

        boolean changes;
        boolean validScope;

        String text = scopeTextField.getText().toLowerCase();

        try {
            Scope scope = ScopeTransformer.transform(new rsb.Scope(text));
            Registries.getUnitRegistry().getUnitConfigByScope(scope);
            validScope = true;
        } catch (CouldNotPerformException | IllegalArgumentException | NullPointerException ex) {
            validScope = false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }

        try {
            changes = !ScopeGenerator.generateStringRep(unitConfigObservable.getValue().getScope()).equals(text);
        } catch (CouldNotPerformException ex) {
            changes = !scopeTextField.getText().isEmpty();
        }

        scopeApplyButton.setEnabled(validScope && changes);
        scopeCancelButton.setEnabled(changes);

        if (validScope) {
            if (changes) {
                scopeTextField.setForeground(Color.BLACK);
            } else {
                scopeTextField.setForeground(Color.BLUE.darker());
            }
        } else {
            scopeTextField.setForeground(Color.RED.darker());
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel instancePanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JComboBox locationComboBox;
    private javax.swing.JPanel locationPanel;
    private javax.swing.JButton scopeApplyButton;
    private javax.swing.JButton scopeCancelButton;
    private javax.swing.JTextField scopeTextField;
    private javax.swing.JPanel servicePanel;
    private javax.swing.JComboBox serviceTypeComboBox;
    private javax.swing.JComboBox unitConfigComboBox;
    private javax.swing.JPanel unitPanel;
    private javax.swing.JComboBox unitTypeComboBox;
    // End of variables declaration//GEN-END:variables

    public final static LocationUnitConfigHolder ALL_LOCATION = new LocationUnitConfigHolder(null);
    public final static UnitConfigHolder ALL_UNIT = new UnitConfigHolder(null, null);
    public final static ServiceTypeHolder ALL_Service = new ServiceTypeHolder(ServiceType.UNKNOWN);

    public static class LocationUnitConfigHolder implements Comparable<LocationUnitConfigHolder> {

        private final UnitConfig locationUnitConfig;

        public LocationUnitConfigHolder(UnitConfig locationUnitConfig) {
            this.locationUnitConfig = locationUnitConfig;
        }

        @Override
        public String toString() {
            if (isNotSpecified()) {
                return "All";
            }
            return locationUnitConfig.getLabel();
        }

        public boolean isNotSpecified() {
            return locationUnitConfig == null;
        }

        public UnitConfig getConfig() {
            return locationUnitConfig;
        }

        @Override
        public int compareTo(LocationUnitConfigHolder o) {
            if (o == null) {
                return -1;
            }

            // make sure "all" is on top.
            if (isNotSpecified()) {
                return -1;
            } else if (o.isNotSpecified()) {
                return +1;
            }

            return toString().compareTo(o.toString());
        }
    }

    public static class UnitTypeHolder implements Comparable<UnitTypeHolder> {

        private final UnitType type;

        public UnitTypeHolder(final UnitType type) {
            this.type = type;
        }

        @Override
        public String toString() {
            if (isNotSpecified()) {
                return "All";
            }
            return StringProcessor.transformUpperCaseToCamelCase(type.name());
        }

        public boolean isNotSpecified() {
            return type.equals(UnitType.UNKNOWN);
        }

        public UnitType getType() {
            return type;
        }

        @Override
        public int compareTo(final UnitTypeHolder o) {
            if (o == null) {
                return -1;
            }

            // make sure "all" is on top.
            if (isNotSpecified()) {
                return -1;
            } else if (o.isNotSpecified()) {
                return +1;
            }

            return toString().compareTo(o.toString());
        }
    }

    public static class ServiceTypeHolder implements Comparable<ServiceTypeHolder> {

        private final ServiceType type;
        private final UnitConfig unitConfig;

        public ServiceTypeHolder(final ServiceType type, final UnitConfig unitConfig) {
            this.type = type;
            this.unitConfig = unitConfig;
        }

        public ServiceTypeHolder(final ServiceType type) {
            this.type = type;
            this.unitConfig = null;
        }

        @Override
        public String toString() {
            if (type.equals(ServiceType.UNKNOWN)) {
                return "All";
            }
            return StringProcessor.transformUpperCaseToCamelCase(type.name());
        }

        public boolean isNotSpecified() {
            return type.equals(ServiceType.UNKNOWN);
        }

        public ServiceType getServiceType() {
            return type;
        }

        public UnitConfig getUnitConfig() throws NotAvailableException {
            if (unitConfig == null) {
                throw new NotAvailableException("UnitConfig");
            }
            return unitConfig;
        }

        @Override
        public int compareTo(final ServiceTypeHolder o) {
            if (o == null) {
                return -1;
            }

            // make sure "all" is on top.
            if (type.equals(ServiceType.UNKNOWN)) {
                return -1;
            } else if (o.getServiceType().equals(ServiceType.UNKNOWN)) {
                return +1;
            }

            return toString().compareTo(o.toString());
        }
    }

    public static class UnitConfigHolder implements Comparable<UnitConfigHolder> {

        private final UnitConfig config;
        private final UnitConfig locationUnitConfig;

        public UnitConfigHolder(final UnitConfig unitConfig, final UnitConfig locationUnitConfig) {
            this.config = unitConfig;
            this.locationUnitConfig = locationUnitConfig;
        }

        @Override
        public String toString() {
            if (isNotSpecified()) {
                return "Not Available";
            }
            return StringProcessor.transformUpperCaseToCamelCase(config.getType().name())
                    + " = " + config.getLabel() + ""
                    + " @ " + locationUnitConfig.getLabel()
                    + (config.getDescription().isEmpty() ? "" : " (" + config.getDescription() + ")");
        }

        public boolean isNotSpecified() {
            return config == null;
        }

        public UnitConfig getConfig() {
            return config;
        }

        @Override
        public int compareTo(final UnitConfigHolder o) {
            if (o == null) {
                return -1;
            }
            return toString().compareTo(o.toString());
        }

    }
}
