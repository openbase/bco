/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.visual.util;

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

import org.dc.jul.exception.NotAvailableException;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class StatusPanel extends javax.swing.JPanel {

    protected static final Logger logger = LoggerFactory.getLogger(StatusPanel.class);

    public static StatusPanel instance;
    private final Timer timer;

    public enum StatusType {

        WARN, INFO, ERROR
    }

    /**
     * Creates new form StatePanel
     */
    public StatusPanel() {
        initComponents();
        this.timer = new Timer(0, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });
        timer.setRepeats(false);
        instance = this;
    }

    /**
     * Returns the last created instance of the StatusPanel.
     *
     * @return
     * @throws NotAvailableException If no instance was constructed until know
     */
    public static StatusPanel getInstance() throws NotAvailableException {
        if (instance == null) {
            throw new NotAvailableException("statuspanel", "Instance is not constructed yet!");
        }
        return instance;
    }

    public void reset() {
        timer.stop();
        statusLabel.setText("");
        progressBar.setValue(0);
        progressBar.setIndeterminate(false);
    }

    public void setWarning(final String message) {
        setStatus(message, StatusType.WARN, 5);
    }
    
    public void setError(Exception ex) {
        setStatus(ex.getLocalizedMessage(), StatusType.ERROR, 5);
    }

    public void setStatus(String text, StatusType type, boolean ongoing) {
        reset();
        setText(text, type);
        progressBar.setIndeterminate(ongoing);
    }

    public void setStatus(String text, StatusType type, int validity) {
        setStatus(text, type, validity, 100);
    }

    public void setStatus(String text, StatusType type, int validity, int progress) {
        reset();
        timer.setInitialDelay(validity * 1000);
        setText(text, type);

        progressBar.setValue(progress);
        progressBar.setIndeterminate(progress <= 100);

        timer.start();
    }

    private Future lastFuture;

    public void setStatus(final String text, final StatusType type, final Future future) {
        reset();
        lastFuture = future;
        cancelButton.setEnabled(true);
        progressBar.setIndeterminate(true);
        setText(text, type);
        Executors.newSingleThreadExecutor().execute(new Runnable() {

            @Override
            public void run() {
                try {
                    future.get();
                    reset();
                } catch (CancellationException ex) {
                    setStatus("Canceled by user!", StatusType.WARN, 1);
                } catch (ExecutionException | InterruptedException ex) {
                    setError(ex);
                }
                cancelButton.setEnabled(false);
            }
        });

    }

    private void setText(final String text, StatusType type) {
        switch (type) {
            case INFO:
                statusLabel.setForeground(Color.BLACK);
                logger.info("Status: " + text);
                break;
            case WARN:
                statusLabel.setForeground(Color.ORANGE.darker().darker().darker());
                logger.warn("Status: " + text);
                break;
            case ERROR:
                statusLabel.setForeground(Color.RED.darker().darker());
                logger.error("Status: " + text);
                break;
        }
        statusLabel.setText(text);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        progressBar = new javax.swing.JProgressBar();
        statusLabel = new javax.swing.JLabel();
        cancelButton = new javax.swing.JButton();

        statusLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        statusLabel.setText("Status");

        cancelButton.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        cancelButton.setText("x");
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 555, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelButton)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(cancelButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        lastFuture.cancel(true);
        cancelButton.setEnabled(false);
        setStatus("Canceled", StatusType.WARN, 3);
    }//GEN-LAST:event_cancelButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusLabel;
    // End of variables declaration//GEN-END:variables
}
