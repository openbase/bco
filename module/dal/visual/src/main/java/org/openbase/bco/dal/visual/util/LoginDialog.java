package org.openbase.bco.dal.visual.util;

import com.google.protobuf.ByteString;
import org.openbase.bco.authentication.lib.EncryptionHelper;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ExceptionProcessor;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;

/*-
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * @author divine
 */
public class LoginDialog extends javax.swing.JFrame {

    protected static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LoginDialog.class);

    private static LoginDialog instance;

    public synchronized static LoginDialog getInstance() {
        if (instance == null) {
            instance = new LoginDialog();
        }
        return instance;
    }

    public static void showDialog() {
        getInstance().setVisible(true);
        getInstance().toFront();
        getInstance().requestFocus();
    }

    public static void hideDialog() {
        getInstance().setVisible(false);
    }

    /**
     * Creates new form TestLoginPane
     */
    public LoginDialog() {
        initComponents();
        updateDymamicComponents();

        SessionManager.getInstance().addLoginObserver((o, t) -> {
            updateDymamicComponents();
        });
    }

    private void updateDymamicComponents() {
        if (SessionManager.getInstance().isLoggedIn()) {
            passwordField.setEnabled(false);
            userTextField.setEnabled(false);
            savePasswordCheckBox.setEnabled(false);
            try {
                String userId = SessionManager.getInstance().getUserClientPair().getUserId();
                if (userId.isEmpty()) {
                    userId = SessionManager.getInstance().getUserClientPair().getClientId();
                }
                userTextField.setText(Registries.getUnitRegistry().getUnitConfigById(userId).getUserConfig().getUserName());
            } catch (CouldNotPerformException ex) {
                userTextField.setText("???");
                ExceptionPrinter.printHistory(ex, LOGGER);

            }
            passwordField.setText("******");
            loginButton.setText("Logout");
        } else {
            userTextField.setText("");
            passwordField.setText("");
            userTextField.setEnabled(true);
            savePasswordCheckBox.setEnabled(true);
            passwordField.setEnabled(true);
            loginButton.setText("Login");
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

        jLabel1 = new javax.swing.JLabel();
        userTextField = new javax.swing.JTextField();
        passwordField = new javax.swing.JPasswordField();
        jLabel2 = new javax.swing.JLabel();
        loginButton = new javax.swing.JButton();
        savePasswordCheckBox = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        statusLabel = new javax.swing.JLabel();

        setTitle("BCO Login");
        setAlwaysOnTop(true);
        setLocationByPlatform(true);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("Username");

        userTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                userTextFieldActionPerformed(evt);
            }
        });

        passwordField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                passwordFieldKeyPressed(evt);
            }
        });

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Password");

        loginButton.setText("Login");
        loginButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loginButtonActionPerformed(evt);
            }
        });

        savePasswordCheckBox.setText("Remember");

        jLabel3.setText("Status:");

        statusLabel.setText("Initialized");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addComponent(savePasswordCheckBox, javax.swing.GroupLayout.DEFAULT_SIZE, 207, Short.MAX_VALUE)
                                                .addGap(18, 18, 18)
                                                .addComponent(loginButton))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jLabel1))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(userTextField)
                                                        .addComponent(passwordField)))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel3)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(userTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel1))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(passwordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(loginButton)
                                        .addComponent(savePasswordCheckBox))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 17, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel3)
                                        .addComponent(statusLabel))
                                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void userTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_userTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_userTextFieldActionPerformed

    private void loginButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginButtonActionPerformed

        if (SessionManager.getInstance().isLoggedIn()) {
            logout();
        } else {
            login();
        }
    }//GEN-LAST:event_loginButtonActionPerformed

    private void passwordFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_passwordFieldKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            login();
        }
    }//GEN-LAST:event_passwordFieldKeyPressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JButton loginButton;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JCheckBox savePasswordCheckBox;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JTextField userTextField;
    // End of variables declaration//GEN-END:variables

    private void login() {
        try {
            loginButton.setEnabled(false);
            statusLabel.setForeground(Color.BLACK);
            statusLabel.setText("Processing...");
            final String userId = Registries.getUnitRegistry().getUserUnitIdByUserName(userTextField.getText());
            final LoginCredentials loginCredentials = LoginCredentials.newBuilder().setId(userId)
                    .setAdmin(false).setSymmetric(true).setCredentials(ByteString.copyFrom(EncryptionHelper.hash(new String(passwordField.getPassword())))).build();
            SessionManager.getInstance().loginUser(userId, loginCredentials, savePasswordCheckBox.isSelected());

            if (savePasswordCheckBox.isSelected()) {
                SessionManager.getInstance().storeCredentials(userId, loginCredentials);
            }

            if (savePasswordCheckBox.isSelected()) {
                BCOLogin.getSession().setLocalDefaultUser(userId);
            }
            statusLabel.setForeground(Color.GREEN.darker().darker().darker());
            statusLabel.setText("Login Successful");
            GlobalCachedExecutorService.submit(() -> {
                Registries.getUnitRegistry().waitUntilReadyFuture().get(1000, TimeUnit.DAYS);
                hideDialog();
                return null;
            });

        } catch (CouldNotPerformException ex) {
            statusLabel.setForeground(Color.RED.darker().darker().darker());
            statusLabel.setText(ExceptionProcessor.getInitialCauseMessage(ex));
        } finally {
            loginButton.setEnabled(true);
        }
    }

    private void logout() {
        try {
            loginButton.setEnabled(false);
            statusLabel.setForeground(Color.BLACK);
            statusLabel.setText("Processing...");

            SessionManager.getInstance().logout();
            statusLabel.setForeground(Color.BLUE.darker().darker().darker());
            statusLabel.setText("Logout Successful");
        } finally {
            loginButton.setEnabled(true);
        }

    }
}