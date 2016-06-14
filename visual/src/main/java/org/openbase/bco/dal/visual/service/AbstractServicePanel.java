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
import org.openbase.bco.dal.visual.util.StatusPanel;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.jul.pattern.Observable;
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
    private final Observer observer;
    protected StatusPanel statusPanel;
    private ScheduledExecutorService serviceExecuterService;
    private final SyncObject executerSync = new SyncObject("ExecuterSync");
    private String unitId = "";
    private ServiceTemplateType.ServiceTemplate.ServiceType serviceType;

    /**
     * Creates new form AbstractServiceView
     *
     * @throws org.openbase.jul.exception.InstantiationException
     */
    public AbstractServicePanel() throws InstantiationException {
        try {
            this.serviceExecuterService = Executors.newSingleThreadScheduledExecutor();
            this.observer = (Observer) (Observable source, Object data) -> {
                updateDynamicComponents();
            };
            this.statusPanel = StatusPanel.getInstance();
            new Thread() {

                @Override
                public void run() {
                    while (!isInterrupted()) {
                        synchronized (executerSync) {
                            if (lastCallable == null) {
                                try {
                                    executerSync.wait();
                                } catch (InterruptedException ex) {
                                    break;
                                }
                            }
                            try {
                                statusPanel.setStatus("Apply " + getService() + " update.", StatusPanel.StatusType.INFO, serviceExecuterService.submit(lastCallable));
                            } catch (NotAvailableException ex) {
                                ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
                            }
                            lastCallable = null;
                        }
                    }
                }
            }.start();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public String getServiceName() {
        if (service == null) {
            return "---";
        }
        return serviceConfig.getType().name();
    }

    private Callable<Void> lastCallable;

    public void execute(Callable<Void> callable) {
        synchronized (executerSync) {
            lastCallable = callable;
            executerSync.notifyAll();
        }

//        try {
////            try {
//            if (serviceExecuterService.isTerminated()) {
//                lastCallable = null;
//                statusPanel.setStatus("Apply " + getService() + " update.", StatusPanel.StatusType.INFO, serviceExecuterService.submit(callable));
//            } else {
//                lastCallable = callable;
//            }
////            } catch (InterruptedException ex) {
////
////            }
//       
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

    public void initService(ServiceConfig serviceConfig, S service, UnitRemote unitRemote) {
        if (this.unitRemote != null) {
            unitRemote.removeDataObserver(observer);
        }
        this.unitRemote = unitRemote;
        this.service = service;
        this.serviceConfig = serviceConfig;
        unitRemote.addDataObserver(observer);
        updateDynamicComponents();
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
