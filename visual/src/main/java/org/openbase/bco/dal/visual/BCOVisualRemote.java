package org.openbase.bco.dal.visual;

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

import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.dal.lib.jp.JPProviderControlMode;
import org.openbase.bco.dal.visual.util.LoginDialog;
import org.openbase.bco.registry.lib.jp.JPBCOAutoLoginUser;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ExceptionProcessor;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOVisualRemote extends javax.swing.JFrame {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BCOVisualRemote.class);

    private static BCOVisualRemote instance;

    public synchronized static BCOVisualRemote getInstance() throws NotAvailableException {
        if (instance == null) {
            throw new NotAvailableException(BCOVisualRemote.class.getSimpleName());
        }
        return instance;
    }

    /**
     * Creates new form BCOVisualRemote
     *
     * @throws org.openbase.jul.exception.InstantiationException
     * @throws java.lang.InterruptedException
     */
    public BCOVisualRemote() throws InstantiationException, InterruptedException {
        try {
            instance = this;

            initComponents();
            loadImage();

            selectorPanel.addObserver(genericUnitPanel.getUnitConfigObserver());
            init();
            SessionManager.getInstance().addLoginObserver((o, t) -> {
                java.awt.EventQueue.invokeAndWait(this::updateDynamicComponents);
            });
            updateDynamicComponents();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private void updateDynamicComponents() {
        if (SessionManager.getInstance().isLoggedIn()) {
            loginMenuItem.setText("Switch User");
            try {
                String userId = SessionManager.getInstance().getUserClientPair().getUserId();
                if (userId.isEmpty()) {
                    userId = SessionManager.getInstance().getUserClientPair().getClientId();
                }
                logoutMenuItem.setText("Logout User: " + Registries.getUnitRegistry().getUnitConfigById(userId).getUserConfig().getUserName());
            } catch (CouldNotPerformException ex) {
                logoutMenuItem.setText("Logout");
            }
            logoutMenuItem.setVisible(true);
        } else {
            loginMenuItem.setText("Login");
            logoutMenuItem.setVisible(false);
        }
    }

    private void loadImage() {
        try {
            setIconImage(new ImageIcon(ClassLoader.getSystemResource("dal-visual-remote.png")).getImage());
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not load app icon!", ex), LOGGER, LogLevel.WARN);
        }
    }

    public final void init() throws InterruptedException, CouldNotPerformException {
        GlobalCachedExecutorService.execute(() -> {
            try {
                selectorPanel.init();
            } catch (InterruptedException ex) {
                // just finish task
            } catch (CouldNotPerformException ex) {
                if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                    ExceptionPrinter.printHistory(ex, LOGGER);
                }
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCheckBoxMenuItem1 = new javax.swing.JCheckBoxMenuItem();
        statusPanel = new org.openbase.bco.dal.visual.util.StatusPanel();
        genericUnitPanel = new org.openbase.bco.dal.visual.unit.GenericUnitPanel();
        jPanel1 = new javax.swing.JPanel();
        jSeparator1 = new javax.swing.JSeparator();
        try {
            selectorPanel = new org.openbase.bco.dal.visual.util.SelectorPanel();
        } catch (org.openbase.jul.exception.InstantiationException e1) {
            e1.printStackTrace();
        }
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        loginMenuItem = new javax.swing.JMenuItem();
        logoutMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        exitMenuItem = new javax.swing.JMenuItem();

        jCheckBoxMenuItem1.setSelected(true);
        jCheckBoxMenuItem1.setText("jCheckBoxMenuItem1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("BCO Visual Remote");
        setLocationByPlatform(true);

        jPanel1.add(jSeparator1);
        jPanel1.add(selectorPanel);

        jMenu1.setText("Session");
        jMenu1.setToolTipText("Click to open the login dialog.");

        loginMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        loginMenuItem.setText("Login");
        loginMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loginMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(loginMenuItem);

        logoutMenuItem.setText("Logout");
        logoutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoutMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(logoutMenuItem);
        jMenu1.add(jSeparator2);

        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(exitMenuItem);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(statusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 905, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(genericUnitPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(genericUnitPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(statusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void loginMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginMenuItemActionPerformed
        LoginDialog.showDialog();
    }//GEN-LAST:event_loginMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        setVisible(false);
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void logoutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoutMenuItemActionPerformed
        SessionManager.getInstance().logout();
    }//GEN-LAST:event_logoutMenuItemActionPerformed

    /**
     * @param args the command line arguments
     *
     * @throws java.lang.InterruptedException
     * @throws java.lang.reflect.InvocationTargetException
     */
    public static void main(String[] args) throws InterruptedException, InvocationTargetException {
        BCO.printLogo();
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | java.lang.InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not setup look and feel!", ex), LOGGER, LogLevel.WARN);
        }
        //</editor-fold>
        //</editor-fold>

        //</editor-fold>
        JPService.setApplicationName(BCOVisualRemote.class);
        JPService.registerProperty(JPAuthentication.class);
        JPService.registerProperty(JPBCOAutoLoginUser.class);
        JPService.registerProperty(JPProviderControlMode.class);
        JPService.parseAndExitOnError(args);

        BCOLogin.getSession().autoLogin(true);

        BCOVisualRemote bcoVisualRemote;
        /* Create and display the form */
        java.awt.EventQueue.invokeAndWait(() -> {
            try {
                new BCOVisualRemote();
            } catch (Exception ex) {
                if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                    ExceptionPrinter.printHistory(ex, LOGGER);
                }
                System.exit(1);
            }
        });

        java.awt.EventQueue.invokeAndWait(() -> {
            try {
                if(!getInstance().isVisible()) {
                    getInstance().setVisible(true);
                }
            } catch (Exception ex) {
                if (!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                    ExceptionPrinter.printHistory(ex, LOGGER);
                }
                System.exit(1);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem exitMenuItem;
    private org.openbase.bco.dal.visual.unit.GenericUnitPanel genericUnitPanel;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JMenuItem loginMenuItem;
    private javax.swing.JMenuItem logoutMenuItem;
    private org.openbase.bco.dal.visual.util.SelectorPanel selectorPanel;
    private org.openbase.bco.dal.visual.util.StatusPanel statusPanel;
    // End of variables declaration//GEN-END:variables
}
