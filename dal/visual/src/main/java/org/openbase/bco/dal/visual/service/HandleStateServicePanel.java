package org.openbase.bco.dal.visual.service;

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
import org.openbase.bco.dal.lib.layer.service.provider.HandleStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;

import java.awt.Color;
import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;

/**
 *
 * * @author kengelma
 */
public class HandleStateServicePanel extends AbstractServicePanel<HandleStateProviderService, ConsumerService, OperationService> {

    /**
     * Creates new form ReedSwitchProviderPanel
     *
     * @throws org.openbase.jul.exception.InstantiationException can't
     * instantiate
     */
    public HandleStateServicePanel() throws org.openbase.jul.exception.InstantiationException {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        handleStatePanel = new javax.swing.JPanel();
        handleStateLabel = new javax.swing.JLabel();

        handleStatePanel.setBackground(new java.awt.Color(204, 204, 204));
        handleStatePanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 5, true));
        handleStatePanel.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N

        handleStateLabel.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        handleStateLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        handleStateLabel.setText("HandleState");
        handleStateLabel.setFocusable(false);
        handleStateLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout handleStatePanelLayout = new javax.swing.GroupLayout(handleStatePanel);
        handleStatePanel.setLayout(handleStatePanelLayout);
        handleStatePanelLayout.setHorizontalGroup(
            handleStatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(handleStateLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
        handleStatePanelLayout.setVerticalGroup(
            handleStatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(handleStateLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        handleStateLabel.getAccessibleContext().setAccessibleName("ReedSwitchState");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(handleStatePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(handleStatePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel handleStateLabel;
    private javax.swing.JPanel handleStatePanel;
    // End of variables declaration//GEN-END:variables

    @Override
    protected void updateDynamicComponents() {
        try {
            switch (getProviderService().getHandleState().getPosition()) {
                case 0:
                    handleStateLabel.setForeground(Color.WHITE);
                    handleStatePanel.setBackground(Color.BLUE);
                    break;
                case 180:
                    handleStateLabel.setForeground(Color.BLACK);
                    handleStatePanel.setBackground(Color.CYAN);
                    break;
                case 90:
                    handleStateLabel.setForeground(Color.WHITE);
                    handleStatePanel.setBackground(Color.GREEN);
                    break;
                default:
                    throw new InvalidStateException("State[" + getProviderService().getHandleState().getPosition() + "] is unknown.");
            }
            handleStateLabel.setText("Current HandleState = " + getProviderService().getHandleState().getPosition());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
    }
}