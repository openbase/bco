/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.visual.service;

import de.citec.dal.hal.service.Service;
import de.citec.dal.visual.DalVisualRemote;
import de.citec.dal.visual.util.StatusPanel;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.schedule.SyncObject;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.service.ServiceTemplateType;

/**
 *
 * @author mpohling
 * @param <S>
 */
public abstract class AbstractServicePanel<S extends Service> extends javax.swing.JPanel {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private S service;
    private Observable observable;
    private final Observer observer;
    protected StatusPanel statusPanel;
    private ScheduledExecutorService serviceExecuterService;
    private final SyncObject executerSync = new SyncObject("ExecuterSync");
    private String unitId = "";
    private ServiceTemplateType.ServiceTemplate.ServiceType serviceType;
    
    /**
     * Creates new form AbstractServiceView
     *
     * @throws de.citec.jul.exception.InstantiationException
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
        return service.getServiceType().name();
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
    
    public void initService(S service, Observable observable) {
        if (this.observable != null) {
            observable.removeObserver(observer);
        }
        this.observable = observable;
        this.service = service;
        observable.addObserver(observer);
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
