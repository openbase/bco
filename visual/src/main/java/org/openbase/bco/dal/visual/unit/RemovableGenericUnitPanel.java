package org.openbase.bco.dal.visual.unit;

/*
 * #%L
 * DAL Visualisation
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class RemovableGenericUnitPanel extends GenericUnitPanel<AbstractUnitRemote> {

    private final ObservableImpl<String> removedObservable;
    private String mapId;

    /**
     * Creates new form TestPanel
     * @throws org.openbase.jul.exception.InstantiationException
     */
    public RemovableGenericUnitPanel() {
        removedObservable = new ObservableImpl<>();
        initComponents();
    }

    public void init(String mapId) {
        this.mapId = mapId;
    }

    public String getMapId() {
        return mapId;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        genericUnitPanel = new org.openbase.bco.dal.visual.unit.GenericUnitPanel();
        removeButton = new javax.swing.JButton();

        removeButton.setFont(new java.awt.Font("Dialog", 1, 8)); // NOI18N
        removeButton.setText("X");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(removeButton))
            .addComponent(genericUnitPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(removeButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(genericUnitPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 272, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        try {
            removedObservable.notifyObservers(mapId);
        } catch (MultiException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.WARN);
        }
    }//GEN-LAST:event_removeButtonActionPerformed

    public void addObserver(Observer<String> removedObserver) {
        removedObservable.addObserver(removedObserver);
    }

    public void removeObserver(Observer<String> removedObserver) {
        removedObservable.removeObserver(removedObserver);
    }

    public GenericUnitPanel getGenericUnitPanel() {
        return genericUnitPanel;
    }

    @Override
    public void updateUnitConfig(UnitConfig unitConfig, ServiceType serviceType) throws CouldNotPerformException, InterruptedException {
        genericUnitPanel.updateUnitConfig(unitConfig, serviceType);
    }

    @Override
    public void updateUnitConfig(UnitConfig unitConfig, ServiceType serviceType, Object serviceAttribute) throws CouldNotPerformException, InterruptedException {
        genericUnitPanel.updateUnitConfig(unitConfig, serviceType, serviceAttribute);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.openbase.bco.dal.visual.unit.GenericUnitPanel genericUnitPanel;
    private javax.swing.JButton removeButton;
    // End of variables declaration//GEN-END:variables
}
