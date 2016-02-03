/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.visual.unit;

/*
 * #%L
 * DAL Visualisation
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import org.dc.bco.dal.remote.unit.AbstractUnitRemote;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.MultiException;
import org.dc.jul.schedule.SyncObject;
import org.dc.jul.visual.layout.LayoutGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 * @param <RS> The unit remote service to use.
 */
public class GenericUnitCollectionPanel<RS extends AbstractUnitRemote> extends javax.swing.JPanel {

    protected static final Logger logger = LoggerFactory.getLogger(GenericUnitCollectionPanel.class);

    private DeviceRegistryRemote deviceRegistryRemote;
    private Map<String, GenericUnitPanel<RS>> unitPanelMap;
    private final SyncObject unitPanelMapLock = new SyncObject("UnitPanelMapLock");

    /**
     * Creates new form GenericUnitCollectionPanel
     *
     */
    public GenericUnitCollectionPanel() {
        unitPanelMap = new HashMap<>();
        initComponents();
    }

    /**
     * Initialize internal device registry remote. Method should be called after construction and before adding any units.
     *
     * @throws InitializationException
     * @throws java.lang.InterruptedException
     */
    public void init() throws InitializationException, InterruptedException {
        try {
            deviceRegistryRemote = new DeviceRegistryRemote();
            deviceRegistryRemote.init();
            deviceRegistryRemote.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public Collection<GenericUnitPanel<RS>> add(final Collection<String> unitLabelList) throws InitializationException {
        final List<GenericUnitPanel<RS>> unitPanelList = new ArrayList<>();

        MultiException.ExceptionStack exceptionStack = null;

        try {
            // create and add unit panels.
            for (String unitLabel : unitLabelList) {

                try {
                    unitPanelList.addAll(add(unitLabel));
                } catch (Exception ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
            MultiException.checkAndThrow("Could not add all units!", exceptionStack);
        } catch (Exception ex) {
            throw new InitializationException(this, ex);
        }
        return unitPanelList;
    }

    public Collection<GenericUnitPanel<RS>> add(final String unitLabel) throws CouldNotPerformException {
        try {
            final List<GenericUnitPanel<RS>> unitPanelList = new ArrayList<>();
            List<UnitConfig> unitConfigsByLabel = deviceRegistryRemote.getUnitConfigsByLabel(unitLabel);

            MultiException.ExceptionStack exceptionStack = null;

            // process all units with given label.
            for (UnitConfig unitConfig : unitConfigsByLabel) {
                synchronized (unitPanelMapLock) {
                    if (unitPanelMap.containsKey(unitConfig.getId())) {
                        logger.warn("Unit panel for Unit[" + unitConfig.getId() + "] already exist! Ignore...");
                        continue;
                    }
                    try {
                        unitPanelList.add(add(unitConfig));
                    } catch (Exception ex) {
                        exceptionStack = MultiException.push(this, ex, exceptionStack);
                    }
                }
            }
            MultiException.checkAndThrow("Could not proccess all units!", exceptionStack);
            return unitPanelList;
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not add all matching units for Label[" + unitLabel + "]", ex);
        }
    }

    public GenericUnitPanel add(final UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
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

    private void updateDynamicComponents() {
        System.out.println("update " + unitPanelMap.values().size() + " components.");
        synchronized (unitPanelMapLock) {
            contentPanel.removeAll();
            for (JComponent component : unitPanelMap.values()) {
                contentPanel.add(component);
            }
            LayoutGenerator.designList(contentPanel, unitPanelMap.values());
        }
        contentPanel.validate();
        contentPanel.revalidate();
        contentScrollPane.validate();
        contentScrollPane.revalidate();
        this.validate();
        this.revalidate();
    }

    public DeviceRegistryRemote getDeviceRegistryRemote() {
        return deviceRegistryRemote;
    }

    public Map<String, GenericUnitPanel<RS>> getUnitPanelMap() {
        return Collections.unmodifiableMap(unitPanelMap);
    }

    public Collection<GenericUnitPanel<RS>> getUnitPanelList() {
        return Collections.unmodifiableCollection(unitPanelMap.values());
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
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


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel contentPanel;
    private javax.swing.JScrollPane contentScrollPane;
    // End of variables declaration//GEN-END:variables
}
