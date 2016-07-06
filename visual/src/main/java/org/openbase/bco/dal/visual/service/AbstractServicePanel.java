package org.openbase.bco.dal.visual.service;

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
import java.awt.Component;
import java.awt.Container;
import org.openbase.bco.dal.visual.util.StatusPanel;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import java.util.concurrent.Future;
import javax.swing.JComponent;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Remote.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType;

/**
 *
 * @author mpohling
 * @param <S>
 */
public abstract class AbstractServicePanel<S extends Service> extends javax.swing.JPanel {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private S service;
    private ServiceConfig serviceConfig;
    private UnitRemote unitRemote;
    private final Observer<Object> dataObserver;
    private final Observer<ConnectionState> connectionStateObserver;
    protected StatusPanel statusPanel;
    private final SyncObject executerSync = new SyncObject("ExecuterSync");
    private String unitId = "";
    private ServiceTemplateType.ServiceTemplate.ServiceType serviceType;
    private Future lastActionFuture;
//    private final RecurrenceEventFilter<Future> recurrenceActionFilter;

    /**
     * Creates new form AbstractServiceView
     *
     * @throws org.openbase.jul.exception.InstantiationException
     */
    public AbstractServicePanel() throws InstantiationException {
        try {
            this.statusPanel = StatusPanel.getInstance();
//            this.recurrenceActionFilter = new RecurrenceEventFilter<Future>() {
//
//                @Override
//                public void relay() throws Exception {
//
//                }
//            };
            this.dataObserver = (Observer) (Observable source, Object data) -> {
                updateDynamicComponents();
            };
            this.connectionStateObserver = (Observable<ConnectionState> source, ConnectionState connectionState) -> {
                enableComponents(this, connectionState.equals(ConnectionState.CONNECTED));
                System.out.println("enable: " + connectionState.equals(ConnectionState.CONNECTED));
            };
//            GlobalExecutionService.submit(() -> {
//                while (!Thread.currentThread().isInterrupted()) {
//                    synchronized (executerSync) {
//                        if (lastCallable == null) {
//                            try {
//                                executerSync.wait();
//                            } catch (InterruptedException ex) {
//                                break;
//                            }
//                        }
//                        try {
//
//                        } catch (NotAvailableException ex) {
//                            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
//                        }
//                        lastCallable = null;
//                    }
//                }
//            });
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public final void enableComponents(JComponent component, boolean enable) {
        synchronized (getTreeLock()) {
            for (Component childComponent : component.getComponents()) {
                childComponent.setEnabled(enable);
                if (childComponent instanceof JComponent) {
                    enableComponents((JComponent) childComponent, enable);
                }
            }
        }
    }

    public String getServiceName() {
        if (service == null) {
            return "---";
        }
        return serviceConfig.getType().name();
    }

    public synchronized void notifyActionProcessing(final Future future) {
        if (lastActionFuture != null && lastActionFuture.isDone()) {
            lastActionFuture.cancel(true);
        }
        statusPanel.setStatus("Process " + getServiceName() + " action.", StatusPanel.StatusType.INFO, future);
        lastActionFuture = future;
    }

    public S getService() throws NotAvailableException {
        if (service == null) {
            throw new NotAvailableException("service");
        }
        return service;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public ServiceTemplateType.ServiceTemplate.ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceTemplateType.ServiceTemplate.ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public void initService(ServiceConfig serviceConfig, S service, UnitRemote unitRemote) throws CouldNotPerformException, InterruptedException {
        try {
            if (this.unitRemote != null) {
                unitRemote.removeDataObserver(dataObserver);
                unitRemote.removeConnectionStateObserver(connectionStateObserver);
            }
            this.unitRemote = unitRemote;
            this.service = service;
            this.serviceConfig = serviceConfig;
            unitRemote.addDataObserver(dataObserver);
            unitRemote.addConnectionStateObserver(connectionStateObserver);
            unitRemote.waitForData();
            updateDynamicComponents();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(unitRemote.getClass(), ex);
        }
    }

    protected abstract void updateDynamicComponents();

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
            .addGap(0, 41, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 38, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
