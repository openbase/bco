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

import org.openbase.bco.dal.lib.layer.service.consumer.ConsumerService;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.bco.dal.lib.layer.service.provider.ProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.visual.util.StatusPanel;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.*;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.controller.Remote;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.service.ServiceDescriptionType;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Future;

/**
 * @param <PS>
 * @param <CS>
 * @param <OS>
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractServicePanel<PS extends ProviderService, CS extends ConsumerService, OS extends OperationService> extends javax.swing.JPanel implements Shutdownable {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String SERVICE_PANEL_SUFFIX = "Panel";

    private PS providerService;
    private CS consumerService;
    private OS operationService;

    private ServiceDescriptionType.ServiceDescription providerServiceDescription;
    private ServiceDescriptionType.ServiceDescription consumerServiceDescription;
    private ServiceDescriptionType.ServiceDescription operationServiceDescription;

    private int PROVIDER = 0;
    private int CONSUMER = 1;
    private int OPERATION = 2;

    /**
     * ServiceConfig slots
     * [0] = providerServiceConfig
     * [1] = consumerServiceConfig
     * [2] = operationServiceConfig
     */
    private ServiceDescriptionType.ServiceDescription[] serviceConfigs;

    private UnitRemote unitRemote;
    private final Observer dataObserver;
    private final Observer<Remote, ConnectionState.State> connectionStateObserver;
    protected StatusPanel statusPanel;
    private final SyncObject executerSync = new SyncObject("ExecuterSync");
    private String unitId = "";
    private ServiceTemplateType.ServiceTemplate.ServiceType serviceType;
    private Future lastActionDescription;
//    private final RecurrenceEventFilter<Future> recurrenceActionFilter;

    /**
     * Creates new form AbstractServiceView
     *
     * @throws org.openbase.jul.exception.InstantiationException
     */
    public AbstractServicePanel() throws InstantiationException {
        try {
            this.serviceConfigs = new ServiceDescriptionType.ServiceDescription[3];
            this.statusPanel = StatusPanel.getInstance();
//            this.recurrenceActionFilter = new RecurrenceEventFilter<Future>() {
//
//                @Override
//                public void relay() throws Exception {
//
//                }
//            };
            this.dataObserver = (source, data) -> {
                updateDynamicComponents();
            };
            this.connectionStateObserver = (source, connectionState) -> {
                enableComponents(this, connectionState.equals(ConnectionState.State.CONNECTED));
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
        for (ServiceDescriptionType.ServiceDescription serviceDescription : serviceConfigs) {
            if (serviceDescription != null) {
                return serviceDescription.getServiceType().name();
            }
        }
        return "---";
    }

    protected UnitRemote getUnitRemote() {
        return this.unitRemote;
    }

    public synchronized void notifyActionProcessing(final Future future) {
        if (lastActionDescription != null && lastActionDescription.isDone()) {
            lastActionDescription.cancel(true);
        }
        statusPanel.setStatus("Process " + getServiceName() + " action.", StatusPanel.StatusType.INFO, future);
        lastActionDescription = future;
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

    public void setProviderServiceDescription(final ServiceDescriptionType.ServiceDescription providerServiceConfig) {
        this.providerServiceDescription = providerServiceConfig;
        this.serviceConfigs[PROVIDER] = providerServiceConfig;
    }

    public void setConsumerServiceDescription(final ServiceDescriptionType.ServiceDescription consumerServiceConfig) {
        this.consumerServiceDescription = consumerServiceConfig;
        this.serviceConfigs[CONSUMER] = consumerServiceConfig;
    }

    public void setOperationServiceDescription(final ServiceDescriptionType.ServiceDescription operationServiceConfig) {
        this.operationServiceDescription = operationServiceConfig;
        this.serviceConfigs[OPERATION] = operationServiceConfig;
    }

    public boolean hasProviderService() {
        return providerServiceDescription != null;
    }

    public boolean hasConsumerService() {
        return providerServiceDescription != null;
    }

    public boolean hasOperationService() {
        return providerServiceDescription != null;
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
     *
     * @throws CouldNotPerformException
     * @throws InterruptedException
     */
    public void init(final UnitRemote unitRemote) throws CouldNotPerformException, InterruptedException {
        if (this.unitRemote != null) {
            unitRemote.removeDataObserver(dataObserver);
            unitRemote.removeConnectionStateObserver(connectionStateObserver);
        }
        this.unitRemote = unitRemote;
    }

    public void initObserver() throws CouldNotPerformException, InterruptedException {
        unitRemote.addDataObserver(dataObserver);
        unitRemote.addConnectionStateObserver(connectionStateObserver);
        unitRemote.waitForData();
        updateDynamicComponents();
    }

    /**
     * This method binds a service description to this unit service panel.
     * Make sure the remote unit was initialized before and the service description is compatible with this unit.
     *
     * @param serviceDescription the new service description to bind to this unit remote.
     *
     * @throws CouldNotPerformException is thrown if any error occurs during the binding process.
     * @throws InterruptedException
     */
    public void bindServiceConfig(final ServiceDescriptionType.ServiceDescription serviceDescription) throws CouldNotPerformException, InterruptedException {
        try {
            if (unitRemote == null) {
                throw new InvalidStateException("The unit remote is unknown!!");
            }
            setServiceConfig(serviceDescription);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not bind ServiceConfig[" + serviceDescription.getServiceType() + "] on UnitRemote[" + ScopeProcessor.generateStringRep(unitRemote.getScope()) + "]!", ex);
        }
    }

    @Override
    public void shutdown() {
        if (this.unitRemote != null) {
            unitRemote.removeDataObserver(dataObserver);
            unitRemote.removeConnectionStateObserver(connectionStateObserver);
        }
    }

    private void setServiceConfig(final ServiceDescriptionType.ServiceDescription serviceDescription) throws CouldNotPerformException {
        try {
            try {
                switch (serviceDescription.getPattern()) {
                    case OPERATION:
                        if (operationServiceDescription != null) {
                            throw new InvalidStateException("OperationServiceConfig already bind!");
                        }
                        operationService = (OS) unitRemote;
                        setOperationServiceDescription(serviceDescription);
                        break;
                    case PROVIDER:
                        if (providerServiceDescription != null) {
                            throw new InvalidStateException("ProviderServiceConfig already bind!");
                        }
                        providerService = (PS) unitRemote;
                        setProviderServiceDescription(serviceDescription);
                        break;
                    case CONSUMER:
                        if (consumerServiceDescription != null) {
                            throw new InvalidStateException("ConsumerServiceConfig already bind!");
                        }
                        consumerService = (CS) unitRemote;
                        setConsumerServiceDescription(serviceDescription);
                        break;
                    default:
                        throw new EnumNotSupportedException(serviceDescription.getPattern(), this);
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
