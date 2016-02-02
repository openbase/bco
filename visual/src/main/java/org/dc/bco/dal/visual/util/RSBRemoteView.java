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

import com.google.protobuf.GeneratedMessage;
import org.dc.bco.dal.remote.unit.AbstractUnitRemote;
import org.dc.bco.dal.remote.unit.DALRemoteService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.dc.jul.processing.StringProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;
import rst.rsb.ScopeType.Scope;

/**
 *
 * @author mpohling
 */
public abstract class RSBRemoteView<RS extends AbstractUnitRemote> extends javax.swing.JPanel implements Observer<GeneratedMessage> {
//public abstract class RSBRemoteView<M extends GeneratedMessage, R extends DALRemoteService<M>> extends javax.swing.JPanel implements Observer<M> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private RS remoteService;

    /**
     * Creates new form RSBViewService
     */
    public RSBRemoteView() {
        this.initComponents();
    }

    private synchronized void setRemoteService(final RS remoteService) {

        if (this.remoteService != null) {
            this.remoteService.shutdown();
        }

        this.remoteService = remoteService;
        remoteService.addObserver(this);
    }

    public synchronized void shutdown() {
        if (remoteService == null) {
            return;
        }

        remoteService.shutdown();
    }

    @Override
    public void update(Observable<GeneratedMessage> source, GeneratedMessage data) {
        updateDynamicComponents(data);
    }

    public RS getRemoteService() throws NotAvailableException {
        if (remoteService == null) {
            throw new NotAvailableException("remoteService");
        }
        return remoteService;
    }
//    public M getData() throws CouldNotPerformException {
//        return getRemoteService().getData();
//    }
    public void setUnitRemote(final UnitTemplateType.UnitTemplate.UnitType unitType, final Scope scope) throws CouldNotPerformException, InterruptedException {
        logger.info("Setup unit remote: " + unitType + ".");
        try {
            Class<? extends RS> remoteClass = loadUnitRemoteClass(unitType);
            RS unitRemote = instantiatUnitRemote(remoteClass);
            initUnitRemote(unitRemote, scope);
            unitRemote.activate();
            setRemoteService(unitRemote);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not setup unit remote config!", ex);
        }
    }

    public void setUnitRemote(final UnitConfigType.UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        logger.info("Setup unit remote: " + unitConfig.getId());
        try {
            Class<? extends RS> remoteClass = loadUnitRemoteClass(unitConfig.getType());
            RS unitRemote = instantiatUnitRemote(remoteClass);
            initUnitRemote(unitRemote, unitConfig);
            unitRemote.activate();
            setRemoteService(unitRemote);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not setup unit remote config!", ex);
        }
    }

    private Class<? extends RS> loadUnitRemoteClass(UnitTemplateType.UnitTemplate.UnitType unitType) throws CouldNotPerformException {
        String remoteClassName = DALRemoteService.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToCamelCase(unitType.name()) + "Remote";
        try {
            return (Class<? extends RS>) getClass().getClassLoader().loadClass(remoteClassName);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not detect remote class for UnitType[" + unitType.name() + "]", ex);
        }
    }

    public RS instantiatUnitRemote(Class<? extends RS> remoteClass) throws InstantiationException {
        try {
            return remoteClass.newInstance();
        } catch (Exception ex) {
            throw new InstantiationException("Could not instantiate unit remote out of RemoteClass[" + remoteClass.getSimpleName() + "]!", ex);
        }
    }

    private void initUnitRemote(AbstractUnitRemote unitRemote, UnitConfig config) throws CouldNotPerformException {
        try {
            unitRemote.init(config);
        } catch (InitializationException ex) {
            throw new CouldNotPerformException("Could not init " + unitRemote + "!", ex);
        }
    }

    private void initUnitRemote(AbstractUnitRemote unitRemote, Scope scope) throws CouldNotPerformException {
        try {
            unitRemote.init(scope);
        } catch (InitializationException ex) {
            throw new CouldNotPerformException("Could not init " + unitRemote + "!", ex);
        }
    }

    protected abstract void updateDynamicComponents(GeneratedMessage data);

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
