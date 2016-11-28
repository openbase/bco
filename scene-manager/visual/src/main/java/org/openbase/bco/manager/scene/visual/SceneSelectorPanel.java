package org.openbase.bco.manager.scene.visual;

/*
 * #%L
 * COMA SceneManager Visualisation
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingUtilities;
import org.openbase.bco.dal.visual.util.StatusPanel;
import org.openbase.bco.dal.visual.util.UnitRemoteView;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.DeviceRegistryDataType.DeviceRegistryData;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.InventoryStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType.Scope;
import rst.domotic.registry.LocationRegistryDataType.LocationRegistryData;
import rst.domotic.state.EnablingStateType.EnablingState;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class SceneSelectorPanel extends javax.swing.JPanel {

    private final Object REMOTE_VIEW_LOCK = new Object();

    protected static final Logger logger = LoggerFactory.getLogger(SceneSelectorPanel.class);

    private DeviceRegistryRemote deviceRegistryRemote;
    private LocationRegistryRemote locationRegistryRemote;

    private UnitRemoteView remoteView;

    private StatusPanel statusPanel;

    private LocationUnitConfigHolder selectedLocationConfigHolder;
    private UnitConfigHolder selectedUnitConfigHolder;
    private ServiceTypeHolder selectedServiceTypeHolder;

    private final ObservableImpl<UnitConfigServiceTypeHolder> unitConfigServiceTypeObservable;

    private boolean init = false;

    /**
     * Creates new form SceneSelectorPanel
     *
     */
    public SceneSelectorPanel() {
        this.unitConfigServiceTypeObservable = new ObservableImpl<>();
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
        scopeTextField.setEnabled(value);
        scopeCancelButton.setEnabled(value);
        scopeApplyButton.setEnabled(value);
    }

    public void init() throws InitializationException, InterruptedException, CouldNotPerformException {
        this.deviceRegistryRemote = new DeviceRegistryRemote();
        this.locationRegistryRemote = new LocationRegistryRemote();

        statusPanel = StatusPanel.getInstance();
        statusPanel.setStatus("Init device manager connection...", StatusPanel.StatusType.INFO, true);
        deviceRegistryRemote.init();
        statusPanel.setStatus("Init location manager connection...", StatusPanel.StatusType.INFO, true);
        locationRegistryRemote.init();

        statusPanel.setStatus("Connecting to device manager...", StatusPanel.StatusType.INFO, true);
        deviceRegistryRemote.activate();
        statusPanel.setStatus("Connecting to location manager...", StatusPanel.StatusType.INFO, true);
        locationRegistryRemote.activate();
        statusPanel.setStatus("Connection established.", StatusPanel.StatusType.INFO, 3);

        locationRegistryRemote.addDataObserver((final Observable<LocationRegistryData> source, LocationRegistryData data) -> {
            SwingUtilities.invokeLater(() -> {
                updateDynamicComponents();
            });
        });

        deviceRegistryRemote.addDataObserver((final Observable<DeviceRegistryData> source, DeviceRegistryData data) -> {
            SwingUtilities.invokeLater(() -> {
                updateDynamicComponents();
            });
        });

        init = true;

        updateDynamicComponents();
        setEnable(true);
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
        if (!init) {
            return;
        }
        logger.info("Update selectorPanel!");
        try {
            if (locationComboBox.getSelectedIndex() != -1) {
                selectedLocationConfigHolder = (LocationUnitConfigHolder) locationComboBox.getSelectedItem();
            } else {
                selectedLocationConfigHolder = ALL_LOCATION;
            }
        } catch (Exception ex) {
            selectedLocationConfigHolder = ALL_LOCATION;
            logger.info("Test");
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
            logger.info("Test2");
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
            logger.info("Test3");
            ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
        }

        try {
            ArrayList<LocationUnitConfigHolder> locationConfigHolderList = new ArrayList<>();
            locationConfigHolderList.add(ALL_LOCATION);
            for (UnitConfig locationUnitConfig : locationRegistryRemote.getLocationConfigs()) {
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
                    for (UnitConfig config : locationRegistryRemote.getUnitConfigsByLocation(selectedLocationConfigHolder.getConfig().getId())) {
                        unitConfigHolderList.add(new UnitConfigHolder(config));
                    }
                } else {
                    for (UnitConfig config : deviceRegistryRemote.getUnitConfigs()) {

                        // ignore non installed units
                        if (!config.getEnablingState().getValue().equals(EnablingState.State.ENABLED)){
                            continue;
                        }
                        unitConfigHolderList.add(new UnitConfigHolder(config));
                    }
                }
            } else {
                if (unitConfigComboBox.isEnabled() && selectedLocationConfigHolder != null && !selectedLocationConfigHolder.isNotSpecified()) {
                    for (UnitConfig config : locationRegistryRemote.getUnitConfigsByLocation(selectedUnitType, selectedLocationConfigHolder.getConfig().getId())) {
                        unitConfigHolderList.add(new UnitConfigHolder(config));
                    }
                } else {
                    for (UnitConfig config : deviceRegistryRemote.getUnitConfigs(selectedUnitType)) {
                        // ignore non installed units
                        if (!config.getEnablingState().getValue().equals(EnablingState.State.ENABLED)){
                            continue;
                        }
                        unitConfigHolderList.add(new UnitConfigHolder(config));
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
            ExceptionPrinter.printHistory(ex, logger);
        }

        try {
            ArrayList<ServiceTypeHolder> serviceTypeHolderList = new ArrayList<>();
            UnitType selectedUnitType = ((UnitTypeHolder) unitTypeComboBox.getSelectedItem()).getType();
            serviceTypeHolderList.add(ALL_Service);
            for (ServiceTemplate serviceTemplate : deviceRegistryRemote.getUnitTemplateByType(selectedUnitType).getServiceTemplateList()) {
                serviceTypeHolderList.add(new ServiceTypeHolder(serviceTemplate.getType()));
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
    }

    private UnitType detectUnitTypeOutOfScope(Scope scope) throws NotAvailableException {
        for (String element : scope.getComponentList()) {
            for (UnitType type : UnitType.values()) {
                if (element.equalsIgnoreCase(StringProcessor.transformUpperCaseToCamelCase(type.name()))) {
                    return type;
                }
            }
        }
        throw new NotAvailableException("Could not detect unit type for Scope[" + ScopeGenerator.generateStringRep(scope) + "]!");
    }

    public void addObserver(Observer<UnitConfigServiceTypeHolder> observer) {
        unitConfigServiceTypeObservable.addObserver(observer);
    }

    public void removeObserver(Observer<UnitConfigServiceTypeHolder> observer) {
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
        jPanel5 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        scopeTextField = new javax.swing.JTextField();
        scopeApplyButton = new javax.swing.JButton();
        scopeCancelButton = new javax.swing.JButton();

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
//        try {
//            scopeTextField.setText(ScopeGenerator.generateStringRep(unitConfigServiceTypeObservable.getValue().getScope()));
//        } catch (CouldNotPerformException | NullPointerException ex) {
//            scopeTextField.setText("");
//        }
//        updatenButtonStates();
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

        final UnitConfigHolder unitConfigHolder = (UnitConfigHolder) unitConfigComboBox.getSelectedItem();
        final UnitConfig selectedUnitConfig = unitConfigHolder.getConfig();

//        statusPanel.setStatus("Load new remote control " + unitConfigHolder + "...", StatusPanel.StatusType.INFO, serviceExecuterService.submit(new Callable<Void>() {
//            
//            @Override
//            public Void call() throws Exception {
//                try {
//                    unitConfigComboBox.setForeground(Color.BLACK);
//                    
//                    unitConfigObservable.notifyObservers(selectedUnitConfig);
//                    scopeTextField.setText(ScopeGenerator.generateStringRep(selectedUnitConfig.getScope()));
//                } catch (MultiException ex) {
//                    unitConfigComboBox.setForeground(Color.RED);
//                    statusPanel.setError(ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger));
//                }
//                updatenButtonStates();
//                return null;
//            }
//        }));
    }//GEN-LAST:event_unitConfigComboBoxActionPerformed

    private void scopeApplyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scopeApplyButtonActionPerformed
//        statusPanel.setStatus("Load new remote control " + scopeTextField.getText().toLowerCase() + "...", StatusPanel.StatusType.INFO, serviceExecuterService.submit(new Callable<Void>() {
//            
//            @Override
//            public Void call() throws Exception {
//                try {
//                    scopeTextField.setForeground(Color.BLACK);
//                    Scope scope = ScopeTransformer.transform(new rsb.Scope(scopeTextField.getText().toLowerCase()));
//                    UnitConfig.Builder unitConfig = UnitConfig.newBuilder().setScope(scope).setType(detectUnitTypeOutOfScope(scope));
//                    unitConfigObservable.notifyObservers(unitConfig.build());
//                    scopeTextField.setText(ScopeGenerator.generateStringRep(unitConfigObservable.getValue().getScope()));
//                } catch (CouldNotPerformException ex) {
//                    scopeTextField.setForeground(Color.RED);
//                    statusPanel.setError(ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger));
//                }
//                return null;
//                
//            }
//        }));
//        updatenButtonStates();
    }//GEN-LAST:event_scopeApplyButtonActionPerformed

    private void scopeTextFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_scopeTextFieldKeyTyped

    }//GEN-LAST:event_scopeTextFieldKeyTyped

    private void scopeTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_scopeTextFieldKeyReleased
        updatenButtonStates();
    }//GEN-LAST:event_scopeTextFieldKeyReleased

    private void scopeTextFieldCaretPositionChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_scopeTextFieldCaretPositionChanged
    }//GEN-LAST:event_scopeTextFieldCaretPositionChanged

    private void scopeTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_scopeTextFieldFocusLost
        scopeTextField.setText(scopeTextField.getText().toLowerCase());
        updatenButtonStates();
    }//GEN-LAST:event_scopeTextFieldFocusLost

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        final UnitConfigHolder unitConfigHolder = (UnitConfigHolder) unitConfigComboBox.getSelectedItem();
        final UnitConfig selectedUnitConfig = unitConfigHolder.getConfig();
        final ServiceTypeHolder serviceTypeHolder = (ServiceTypeHolder) selectedServiceTypeComboBox.getSelectedItem();
        final ServiceType serviceType = serviceTypeHolder.getType();
        try {
            unitConfigServiceTypeObservable.notifyObservers(new UnitConfigServiceTypeHolder(selectedUnitConfig, serviceType));
        } catch (MultiException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
    }//GEN-LAST:event_addButtonActionPerformed

    public void updatenButtonStates() {

//        boolean changes;
//        boolean validScope;
//
//        String text = scopeTextField.getText().toLowerCase();
//
//        try {
//            detectUnitTypeOutOfScope(ScopeTransformer.transform(new rsb.Scope(text)));
//            validScope = true;
//        } catch (Exception ex) {
//            validScope = false;
//        }
//        try {
////            changes = !ScopeGenerator.generateStringRep(unitConfigServiceTypeObservable.getValue().getScope()).equals(text);
//        } catch (NotAvailableException ex) {
//            changes = !scopeTextField.getText().isEmpty();
//        }
//        scopeApplyButton.setEnabled(validScope && changes);
//        scopeCancelButton.setEnabled(changes);
//        if (validScope) {
//            if (changes) {
//                scopeTextField.setForeground(Color.BLACK);
//            } else {
//                scopeTextField.setForeground(Color.BLUE.darker());
//            }
//        } else {
//            scopeTextField.setForeground(Color.RED.darker());
//        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
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
    private javax.swing.JComboBox selectedServiceTypeComboBox;
    private javax.swing.JPanel servicePanel;
    private javax.swing.JComboBox serviceTypeComboBox;
    private javax.swing.JComboBox unitConfigComboBox;
    private javax.swing.JPanel unitPanel;
    private javax.swing.JComboBox unitTypeComboBox;
    // End of variables declaration//GEN-END:variables

    private final static LocationUnitConfigHolder ALL_LOCATION = new LocationUnitConfigHolder(null);
    private final UnitConfigHolder ALL_UNIT = new UnitConfigHolder(null);
    private final static ServiceTypeHolder ALL_Service = new ServiceTypeHolder(ServiceType.UNKNOWN);

    public static class LocationUnitConfigHolder implements Comparable<LocationUnitConfigHolder> {

        private final UnitConfig config;

        public LocationUnitConfigHolder(final UnitConfig config) {
            this.config = config;
        }

        @Override
        public String toString() {
            if (isNotSpecified()) {
                return "All";
            }
            return config.getLabel();
        }

        public boolean isNotSpecified() {
            return config == null;
        }

        public UnitConfig getConfig() {
            return config;
        }

        @Override
        public int compareTo(LocationUnitConfigHolder o) {
            return toString().compareTo(o.toString());
        }
    }

    private static class UnitTypeHolder implements Comparable<UnitTypeHolder> {

        private UnitType type;

        public UnitTypeHolder(UnitType type) {
            this.type = type;
        }

        @Override
        public String toString() {
            if (type.equals(UnitType.UNKNOWN)) {
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
        public int compareTo(UnitTypeHolder o) {
            return toString().compareTo(o.toString());
        }
    }

    private static class ServiceTypeHolder implements Comparable<ServiceTypeHolder> {

        private ServiceType type;

        public ServiceTypeHolder(ServiceType type) {
            this.type = type;
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

        public ServiceType getType() {
            return type;
        }

        @Override
        public int compareTo(ServiceTypeHolder o) {
            return toString().compareTo(o.toString());
        }
    }

    private class UnitConfigHolder implements Comparable<UnitConfigHolder> {

        private UnitConfig config;

        public UnitConfigHolder(UnitConfig config) {
            this.config = config;
        }

        @Override
        public String toString() {
            if (isNotSpecified()) {
                return "All";
            }
            try {
                return StringProcessor.transformUpperCaseToCamelCase(config.getType().name())
                        + "[" + config.getLabel() + "]"
                        + " @ " + locationRegistryRemote.getLocationConfigById(config.getPlacementConfig().getLocationId()).getLabel();
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
                return StringProcessor.transformUpperCaseToCamelCase(config.getType().name())
                        + "[" + config.getLabel() + "]"
                        + " @ " + config.getPlacementConfig().getLocationId();
            }
        }

        public boolean isNotSpecified() {
            return config == null;
        }

        public UnitConfig getConfig() {
            return config;
        }

        @Override
        public int compareTo(UnitConfigHolder o) {
            return toString().compareTo(o.toString());
        }
    }

    public static class UnitConfigServiceTypeHolder {

        private final UnitConfig config;
        private final ServiceType serviceType;

        public UnitConfigServiceTypeHolder(UnitConfig config, ServiceType serviceType) {
            this.config = config;
            this.serviceType = serviceType;
        }

        public boolean isNotSpecified() {
            return config == null;
        }

        public UnitConfig getConfig() {
            return config;
        }

        public ServiceType getServiceType() {
            return serviceType;
        }
    }
}
