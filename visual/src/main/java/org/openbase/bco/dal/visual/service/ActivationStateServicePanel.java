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
import java.awt.Color;
import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.bco.dal.lib.layer.service.operation.ActivationStateOperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ActivationStateProviderService;
import org.openbase.bco.dal.lib.state.States;
import org.openbase.bco.dal.lib.state.States.Activation;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ActivationStateServicePanel extends AbstractServicePanel<ActivationStateProviderService, ConsumerService, ActivationStateOperationService> {

    /**
     * Creates new form BrightnessService
     *
     * @throws org.openbase.jul.exception.InstantiationException
     */
    public ActivationStateServicePanel() throws org.openbase.jul.exception.InstantiationException {
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

        activationButton = new javax.swing.JButton();
        activationStatePanel = new javax.swing.JPanel();
        activationStatusLabel = new javax.swing.JLabel();

        activationButton.setText("ActivationButton");
        activationButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                activationButtonActionPerformed(evt);
            }
        });

        activationStatePanel.setBackground(new java.awt.Color(204, 204, 204));
        activationStatePanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 5, true));
        activationStatePanel.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N

        activationStatusLabel.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        activationStatusLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        activationStatusLabel.setText("ActivationState");
        activationStatusLabel.setFocusable(false);
        activationStatusLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout activationStatePanelLayout = new javax.swing.GroupLayout(activationStatePanel);
        activationStatePanel.setLayout(activationStatePanelLayout);
        activationStatePanelLayout.setHorizontalGroup(
            activationStatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(activationStatusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
        );
        activationStatePanelLayout.setVerticalGroup(
            activationStatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(activationStatusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(activationStatePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(activationButton, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(activationStatePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(activationButton))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void activationButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_activationButtonActionPerformed
        try {
            switch (getOperationService().getActivationState().getValue()) {
                case ACTIVE:
                    notifyActionProcessing(getOperationService().setActivationState(Activation.INACTIVE));
                    break;
                case INACTIVE:
                case UNKNOWN:
                    notifyActionProcessing(getOperationService().setActivationState(Activation.ACTIVE));
                    break;
                default:
                    throw new InvalidStateException("State[" + getProviderService().getActivationState().getValue() + "] is unknown.");
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not set activation state!", ex), logger);
        }
    }//GEN-LAST:event_activationButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton activationButton;
    private javax.swing.JPanel activationStatePanel;
    private javax.swing.JLabel activationStatusLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    protected void updateDynamicComponents() {
        try {
            logger.debug("state: " + getProviderService().getActivationState().getValue().name());
            switch (getProviderService().getActivationState().getValue()) {
                case ACTIVE:
                    activationStatusLabel.setForeground(Color.BLACK);
                    activationStatePanel.setBackground(Color.GREEN.darker());
                    activationButton.setText("Deactivate");
                    break;
                case INACTIVE:
                    activationStatusLabel.setForeground(Color.LIGHT_GRAY);
                    activationStatePanel.setBackground(Color.GRAY.darker());
                    activationButton.setText("Activate");
                    break;
                case UNKNOWN:
                    activationStatusLabel.setForeground(Color.BLACK);
                    activationStatePanel.setBackground(Color.ORANGE.darker());
                    activationButton.setText("Activate");
                    break;
                default:
                    throw new InvalidStateException("State[" + getProviderService().getActivationState().getValue() + "] is unknown.");
            }
            activationStatusLabel.setText("Current ActivationState = " + StringProcessor.transformUpperCaseToPascalCase(getProviderService().getActivationState().getValue().name()));
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }
}
