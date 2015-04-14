/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.visual.util;

import com.google.protobuf.GeneratedMessage;
import de.citec.jul.rsb.com.RSBRemoteService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;

/**
 *
 * @author mpohling
 * @param <M>
 * @param <R>
 */
public abstract class RSBRemoteView<M extends GeneratedMessage, R extends RSBRemoteService<M>> extends javax.swing.JPanel implements Observer<M> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Class<R> remoteServiceClass;
    private R remoteService;

    /**
     * Creates new form RSBViewService
     */
    public RSBRemoteView() {
        this.initComponents();
        this.remoteServiceClass = null;
        logger.warn("DO NOT USE THIS CONSTRUCTOR! This constructor is just for netbeans gui gen support.");
    }

    public RSBRemoteView(Class<R> remoteServiceClass) {
        this.initComponents();
        this.remoteServiceClass = remoteServiceClass;
    }

    private synchronized void setRemoteService(final R remoteService) {

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
    public void update(Observable<M> source, M data) {
        updateDynamicComponents(data);
    }

    public R getRemoteService() throws NotAvailableException {
        if (remoteService == null) {
            throw new NotAvailableException("remoteService");
        }
        return remoteService;
    }

    public M getData() throws CouldNotPerformException {
        return getRemoteService().getData();
    }

    public void setScope(final Scope scope) throws CouldNotPerformException {
        logger.info("Update Scope: " + scope);

        R service;

        try {
            if (remoteServiceClass == null) {
                throw new InvalidStateException("RemoteService is not configurated!");
            }

            try {
                service = remoteServiceClass.newInstance();
                service.init(scope);
            } catch (java.lang.InstantiationException | IllegalAccessException ex) {
                throw new InstantiationException("RemoteService could not be instaniated!", ex);
            }
            
        } catch (InstantiationException | InvalidStateException ex) {
            throw new CouldNotPerformException("Could not setup scope!", ex);
        }

        service.activate();
        setRemoteService(service);
    }

    protected abstract void updateDynamicComponents(M data);

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
