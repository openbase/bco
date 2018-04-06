package org.openbase.bco.dal.visual.service;

/*
 * #%L
 * BCO DAL Visualisation
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.visual.util.StatusPanel;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.Remote.ConnectionState;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Future;

/**
 * @param <PS>
 * @param <CS>
 * @param <OS>
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractServicePanel<PS extends ProviderService, CS extends ConsumerService, OS extends OperationService> extends javax.swing.JPanel implements Shutdownable {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String SERVICE_PANEL_SUFFIX = "Panel";

    private PS providerService;
    private CS consumerService;
    private OS operationService;

    private ServiceConfig providerServiceConfig;
    private ServiceConfig consumerServiceConfig;
    private ServiceConfig operationServiceConfig;

    private int PROVIDER = 0;
    private int CONSUMER = 1;
    private int OPERATION = 2;

    /**
     * ServiceConfig slots
     * [0] = providerServiceConfig
     * [1] = consumerServiceConfig
     * [2] = operationServiceConfig
     */
    private ServiceConfig[] serviceConfigs;

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
            this.serviceConfigs = new ServiceConfig[3];
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
                logger.info("enable: " + connectionState.equals(ConnectionState.CONNECTED));
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
        for (ServiceConfig serviceConfig : serviceConfigs) {
            if (serviceConfig != null) {
                return serviceConfig.getServiceDescription().getType().name();
            }
        }
        return "---";
    }

    public synchronized void notifyActionProcessing(final Future future) {
        if (lastActionFuture != null && lastActionFuture.isDone()) {
            lastActionFuture.cancel(true);
        }
        statusPanel.setStatus("Process " + getServiceName() + " action.", StatusPanel.StatusType.INFO, future);
        lastActionFuture = future;
    }

    public PS getProviderService() throws NotAvailableException {
        if (providerService == null) {
            throw new NotAvailableException("ProviderService");
        }
        return providerService;
    }

    public CS getConsumerService() throws NotAvailableException {
        if (consumerService == null) {
            throw new NotAvailableException("ConsumerService");
        }
        return consumerService;
    }

    public OS getOperationService() throws NotAvailableException {
        if (operationService == null) {
            throw new NotAvailableException("OperationService");
        }
        return operationService;
    }

    public void setProviderServiceConfig(final ServiceConfig providerServiceConfig) {
        this.providerServiceConfig = providerServiceConfig;
        this.serviceConfigs[PROVIDER] = providerServiceConfig;
    }

    public void setConsumerServiceConfig(final ServiceConfig consumerServiceConfig) {
        this.consumerServiceConfig = consumerServiceConfig;
        this.serviceConfigs[CONSUMER] = consumerServiceConfig;
    }

    public void setOperationServiceConfig(final ServiceConfig operationServiceConfig) {
        this.operationServiceConfig = operationServiceConfig;
        this.serviceConfigs[OPERATION] = operationServiceConfig;
    }

    public boolean hasProviderService() {
        return providerServiceConfig != null;
    }

    public boolean hasConsumerService() {
        return providerServiceConfig != null;
    }

    public boolean hasOperationService() {
        return providerServiceConfig != null;
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

    /**
     * Initializes this service panel with the given unit remote.
     *
     * @param unitRemote
     * @throws CouldNotPerformException
     * @throws InterruptedException
     */
    public void init(final UnitRemote unitRemote, final ServiceConfig serviceConfig) throws CouldNotPerformException, InterruptedException {
        if (this.unitRemote != null) {
            unitRemote.removeDataObserver(dataObserver);
            unitRemote.removeConnectionStateObserver(connectionStateObserver);
        }
        this.unitRemote = unitRemote;
//        bindServiceConfig(serviceConfig);
//        unitRemote.addDataObserver(dataObserver);
//        unitRemote.addConnectionStateObserver(connectionStateObserver);
//        unitRemote.waitForData();
    }

    public void initObserver() throws CouldNotPerformException, InterruptedException {
        unitRemote.addDataObserver(dataObserver);
        unitRemote.addConnectionStateObserver(connectionStateObserver);
        unitRemote.waitForData();
        updateDynamicComponents();
    }

    /**
     * This method binds a service config to this unit service panel.
     * Make sure the remote unit was initialized before and the service config is compatible with this unit.
     *
     * @param serviceConfig the new service config to bind to this unit remote.
     * @throws CouldNotPerformException is thrown if any error occurs during the binding process.
     * @throws InterruptedException
     */
    public void bindServiceConfig(final ServiceConfig serviceConfig) throws CouldNotPerformException, InterruptedException {
        try {
            if (unitRemote == null) {
                throw new InvalidStateException("The unit remote is unknown!!");
            }
            setServiceConfig(serviceConfig);
//            updateDynamicComponents();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not bind ServiceConfig[" + serviceConfig.getServiceDescription().getType() + "] on UnitRemote[" + unitRemote.getScope() + "]!", ex);
        }
    }

    @Override
    public void shutdown() {
        if (this.unitRemote != null) {
            unitRemote.removeDataObserver(dataObserver);
            unitRemote.removeConnectionStateObserver(connectionStateObserver);
        }
    }

    private void setServiceConfig(final ServiceConfig serviceConfig) throws CouldNotPerformException {
        try {
            try {
                switch (serviceConfig.getServiceDescription().getPattern()) {
                    case OPERATION:
                        if (operationServiceConfig != null) {
                            throw new InvalidStateException("OperationServiceConfig already bind!");
                        }
                        operationService = (OS) unitRemote;
                        setOperationServiceConfig(serviceConfig);
                        break;
                    case PROVIDER:
                        if (providerServiceConfig != null) {
                            throw new InvalidStateException("ProviderServiceConfig already bind!");
                        }
                        providerService = (PS) unitRemote;
                        setProviderServiceConfig(serviceConfig);
                        break;
                    case CONSUMER:
                        if (consumerServiceConfig != null) {
                            throw new InvalidStateException("ConsumerServiceConfig already bind!");
                        }
                        consumerService = (CS) unitRemote;
                        setConsumerServiceConfig(serviceConfig);
                        break;
                    default:
                        throw new EnumNotSupportedException(serviceConfig.getServiceDescription().getPattern(), this);
                }
            } catch (ClassCastException ex) {
                throw new InvalidStateException("Given service is not compatible with registered unit!", ex);
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not set ServiceConfig!", ex);
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
