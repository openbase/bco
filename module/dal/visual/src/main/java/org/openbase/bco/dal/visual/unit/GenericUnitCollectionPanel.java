package org.openbase.bco.dal.visual.unit;

/*
 * #%L
 * BCO DAL Visualisation
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.bco.dal.remote.layer.unit.AbstractUnitRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.visual.swing.layout.LayoutGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import javax.swing.*;
import java.util.*;

/**
 * @param <RS> The unit remote service to use.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class GenericUnitCollectionPanel<RS extends AbstractUnitRemote<Message>> extends javax.swing.JPanel {

    protected static final Logger LOGGER = LoggerFactory.getLogger(GenericUnitCollectionPanel.class);

    private final Map<String, GenericUnitPanel<RS>> unitPanelMap;
    private final SyncObject unitPanelMapLock = new SyncObject("UnitPanelMapLock");
    private final Observer<Object, String> removedObserver;
    // Variables declaration - do not modify                     
    private javax.swing.JPanel contentPanel;
    private javax.swing.JScrollPane contentScrollPane;

    /**
     * Creates new form GenericUnitCollectionPanel
     */
    public GenericUnitCollectionPanel() {
        unitPanelMap = new HashMap<>();
        removedObserver = (source, data) -> {
            synchronized (unitPanelMapLock) {
                unitPanelMap.remove(data);
            }
            updateDynamicComponents();
        };
        initComponents();
    }

    /**
     * Initialize the registry connection. Method should be called after
     * construction and before adding any units.
     *
     * @throws InitializationException
     * @throws java.lang.InterruptedException
     */
    public void init() throws InitializationException, InterruptedException {
        try {
            Registries.waitForData();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public Collection<GenericUnitPanel<RS>> add(final Collection<String> unitLabelList) throws InitializationException, InterruptedException {
        final List<GenericUnitPanel<RS>> unitPanelList = new ArrayList<>();

        MultiException.ExceptionStack exceptionStack = null;

        try {
            // create and add unit panels.
            for (String unitLabel : unitLabelList) {

                try {
                    unitPanelList.addAll(add(unitLabel));
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
            MultiException.checkAndThrow(() ->"Could not add all units!", exceptionStack);
        } catch (MultiException ex) {
            throw new InitializationException(this, ex);
        }
        return unitPanelList;
    }

    public Collection<GenericUnitPanel<RS>> add(final String unitLabel) throws CouldNotPerformException, InterruptedException {
        try {
            final List<GenericUnitPanel<RS>> unitPanelList = new ArrayList<>();
            List<UnitConfig> unitConfigsByLabel = Registries.getUnitRegistry(true).getUnitConfigsByLabel(unitLabel);

            MultiException.ExceptionStack exceptionStack = null;

            // process all units with given label.
            for (UnitConfig unitConfig : unitConfigsByLabel) {
                synchronized (unitPanelMapLock) {
                    if (unitPanelMap.containsKey(unitConfig.getId())) {
                        LOGGER.warn("Unit panel for Unit[" + unitConfig.getId() + "] already exist! Ignore...");
                        continue;
                    }
                    try {
                        unitPanelList.add(add(unitConfig));
                    } catch (CouldNotPerformException ex) {
                        exceptionStack = MultiException.push(this, ex, exceptionStack);
                    }
                }
            }
            MultiException.checkAndThrow(() ->"Could not process all units!", exceptionStack);
            return unitPanelList;
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not add all matching units for Label[" + unitLabel + "]", ex);
        }
    }

    public GenericUnitPanel add(final UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        LOGGER.info("Add " + LabelProcessor.getBestMatch(unitConfig.getLabel()) + " to unit panel.");
        synchronized (unitPanelMapLock) {
            GenericUnitPanel genericUnitPanel;
            try {
                genericUnitPanel = new GenericUnitPanel<>();
                genericUnitPanel.updateUnitConfig(unitConfig);

                unitPanelMap.put(unitConfig.getId(), genericUnitPanel);
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not add Unit[" + unitConfig.getId() + "]", ex);
            }
            updateDynamicComponents();
            return genericUnitPanel;
        }
    }

    public GenericUnitPanel add(final UnitConfig unitConfig, final ServiceType serviceType, final Object serviceState, final boolean removable) throws CouldNotPerformException, InterruptedException {
        LOGGER.info("Add " + LabelProcessor.getBestMatch(unitConfig.getLabel()) + " with " + serviceType.name() + " " + serviceState + "to unit panel.");
        synchronized (unitPanelMapLock) {
            GenericUnitPanel genericUnitPanel;
            try {
                String mapKey = unitConfig.getId() + serviceType.toString();
                genericUnitPanel = new GenericUnitPanel<>();
                if (removable) {
                    RemovableGenericUnitPanel wrapperPanel = new RemovableGenericUnitPanel();
                    wrapperPanel.init(mapKey, (unitConfig.getUnitType() == UnitType.LOCATION) || (unitConfig.getUnitType() == UnitType.UNIT_GROUP));
                    wrapperPanel.addObserver(removedObserver);
                    genericUnitPanel = wrapperPanel;
                }
                if (serviceState == null) {
                    genericUnitPanel.updateUnitConfig(unitConfig, serviceType);
                } else {
                    LOGGER.info("Creating unit panel with command to set a value!");
                    genericUnitPanel.updateUnitConfig(unitConfig, serviceType, serviceState);
                }

                unitPanelMap.put(mapKey, genericUnitPanel);
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not add Unit[" + unitConfig.getId() + "]", ex);
            }
            updateDynamicComponents();
            return genericUnitPanel;
        }
    }

    public GenericUnitPanel add(final UnitConfig unitConfig, final ServiceType serviceType, final boolean removable) throws CouldNotPerformException, InterruptedException {
        return add(unitConfig, serviceType, null, removable);
    }

    public GenericUnitPanel add(final String unitId, final ServiceType serviceType, final Object serviceState, final boolean removable) throws CouldNotPerformException, InterruptedException {
        UnitConfig unitConfig = Registries.getUnitRegistry(true).getUnitConfigById(unitId);
        return add(unitConfig, serviceType, serviceState, removable);
    }

    public GenericUnitPanel add(final String unitId, final ServiceType serviceType, final boolean removable) throws CouldNotPerformException, InterruptedException {
        return add(unitId, serviceType, null, removable);
    }

    private void updateDynamicComponents() {
        LOGGER.debug("update " + unitPanelMap.values().size() + " components.");
        synchronized (unitPanelMapLock) {
            contentPanel.removeAll();
            for (JComponent component : unitPanelMap.values()) {
                contentPanel.add(component);
            }
            LayoutGenerator.generateHorizontalLayout(contentPanel, unitPanelMap.values());
        }
        contentPanel.validate();
        contentPanel.revalidate();
        contentScrollPane.validate();
        contentScrollPane.revalidate();
        this.validate();
        this.revalidate();
    }

    public void clearUnitPanel() {
        unitPanelMap.clear();
        updateDynamicComponents();
    }

    public Map<String, GenericUnitPanel<RS>> getUnitPanelMap() {
        return Collections.unmodifiableMap(unitPanelMap);
    }

    public Collection<GenericUnitPanel<RS>> getUnitPanelList() {
        return Collections.unmodifiableCollection(unitPanelMap.values());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        contentScrollPane = new javax.swing.JScrollPane();
        contentPanel = new javax.swing.JPanel();

        contentScrollPane.setViewportView(contentPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(contentScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(contentScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // End of variables declaration                   
}
