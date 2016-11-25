package org.openbase.bco.dal.visual.unit;

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
import com.google.protobuf.GeneratedMessage;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.bco.dal.visual.service.AbstractServicePanel;
import static org.openbase.bco.dal.visual.service.AbstractServicePanel.SERVICE_PANEL_SUFFIX;
import org.openbase.bco.dal.visual.util.StatusPanel;
import org.openbase.bco.dal.visual.util.StatusPanel.StatusType;
import org.openbase.bco.dal.visual.util.UnitRemoteView;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.Remote.ConnectionState;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalExecutionService;
import org.openbase.jul.visual.swing.layout.LayoutGenerator;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <RS>
 */
public class GenericUnitPanel<RS extends AbstractUnitRemote> extends UnitRemoteView<RS> {

    private final Observer<UnitConfig> unitConfigObserver;
    private final Observer<ConnectionState> connectionStateObserver;
    private boolean autoRemove;
    private List<JComponent> componentList;
    private StatusPanel statusPanel;

    /**
     * Creates new form AmbientLightView
     */
    public GenericUnitPanel() {
        super();

        // init status panel
        try {
            statusPanel = StatusPanel.getInstance();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new org.openbase.jul.exception.InstantiationException(this, ex), logger);
        }

        // init unit config observer
        this.unitConfigObserver = (Observable<UnitConfig> source, UnitConfig config) -> {
            updateUnitConfig(config);
        };

        // init connection observer
        this.connectionStateObserver = (Observable<ConnectionState> source, ConnectionState connectionState) -> {
            updateConnectionState(connectionState);
        };
        initComponents();
        autoRemove = true;
        componentList = new ArrayList<>();
    }

    public void setAutoRemove(boolean autoRemove) {
        this.autoRemove = autoRemove;
    }

    public Observer<UnitConfig> getUnitConfigObserver() {
        return unitConfigObserver;
    }

    private void updateConnectionState(final ConnectionState connectionState) throws InterruptedException {
        try {
            // build unit label
            String remoteLabel;
            try {
                UnitConfig unitConfig = ((UnitConfig) getRemoteService().getConfig());
                remoteLabel = unitConfig.getLabel()
                        + " (" + StringProcessor.transformUpperCaseToCamelCase(unitConfig.getType().name()) + ")"
                        + " @ " + CachedUnitRegistryRemote.getRegistry().getUnitConfigById(unitConfig.getPlacementConfig().getLocationId()).getLabel()
                        + " of " + CachedUnitRegistryRemote.getRegistry().getUnitConfigById(((UnitConfig) getRemoteService().getConfig()).getUnitHostId()).getLabel()
                        + (unitConfig.getDescription().isEmpty() ? "" : "[" + unitConfig.getDescription() + "]");
            } catch (CouldNotPerformException ex) {
                remoteLabel = "?";
            }

            Color textColor = Color.BLACK;
            String textSuffix = "";

            switch (connectionState) {
                case CONNECTED:
                    textSuffix = "connected";
                    break;
                case CONNECTING:
                    textColor = Color.ORANGE.darker();
                    textSuffix = "waiting for connection...";
                    GlobalExecutionService.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                statusPanel.setStatus("Waiting for " + StringProcessor.transformUpperCaseToCamelCase(getRemoteService().getType().name()) + " connection...", StatusType.INFO, true);
                                getRemoteService().waitForConnectionState(ConnectionState.CONNECTED);
                                statusPanel.setStatus("Connection to " + StringProcessor.transformUpperCaseToCamelCase(getRemoteService().getType().name()) + " established.", StatusType.INFO, 3);
                            } catch (NotAvailableException | InterruptedException ex) {
                                statusPanel.setError(ex);
                            }
                        }
                    });
                    break;
                case DISCONNECTED:
                    textColor = Color.RED.darker();
                    textSuffix = "disconnected";
                    break;
                case UNKNOWN:
                    textColor = Color.YELLOW.darker();
                    textSuffix = "";
                    break;
            }

//            TitledBorder titledBorder = BorderFactory.createTitledBorder(StringProcessor.transformUpperCaseToCamelCase(getRemoteService().getType().name()) + ":" + getRemoteService().getId() + " [" + textSuffix + "]");
            TitledBorder titledBorder = BorderFactory.createTitledBorder(remoteLabel + " (" + textSuffix + ")");
            titledBorder.setTitleColor(textColor);
            setBorder(titledBorder);
        } catch (NullPointerException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update connection state!", ex), logger);
        }
    }

    public void updateUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        updateUnitConfig(unitConfig, ServiceType.UNKNOWN);
    }

    public void updateUnitConfig(UnitConfig unitConfig, ServiceType serviceType) throws CouldNotPerformException, InterruptedException {
        try {

            CachedLocationRegistryRemote.waitForData();
            try {
                getRemoteService().removeConnectionStateObserver(connectionStateObserver);

            } catch (NotAvailableException ex) {
                // skip deregistration
            }

            UnitRemote unitRemote = setUnitRemote(unitConfig);
            unitRemote.addConnectionStateObserver(connectionStateObserver);
            updateConnectionState(unitRemote.getConnectionState());
            if (autoRemove) {
                contextPanel.removeAll();
            }
            componentList = new ArrayList<>();
            JPanel servicePanel;
            HashMap<ServiceType, AbstractServicePanel> servicePanelMap = new HashMap<>();

            for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                try {
                    // filter by service type
                    // in case the service type is unknown, all services are loaded, in case the service type is defined only this type will be loaded and all other types are filtered.
                    if (serviceType != ServiceType.UNKNOWN && serviceConfig.getServiceTemplate().getType() != serviceType) {
                        continue;
                    }

                    // skip consumer
                    if (serviceConfig.getServiceTemplate().getPattern().equals(ServicePattern.CONSUMER)) {
                        continue;
                    }

                    //TODO rst type change: getServiceTemplate().getServicePattern() instead of getPattern()
                    //TODO rst type change: getServiceTemplate().getServiceType() instead of getType()
                    // check if service type is already selected.
                    if (!servicePanelMap.containsKey(serviceConfig.getServiceTemplate().getType())) {
                        try {
                            servicePanel = new JPanel();
                            servicePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(StringProcessor.transformUpperCaseToCamelCase(serviceConfig.getServiceTemplate().getType().name()) + " " + ScopeGenerator.generateStringRep(unitConfig.getScope())));
                            AbstractServicePanel abstractServicePanel = instantiatServicePanel(serviceConfig, loadServicePanelClass(serviceConfig.getServiceTemplate().getType()), getRemoteService());
                            abstractServicePanel.setUnitId(unitConfig.getId());
                            abstractServicePanel.setServiceType(serviceConfig.getServiceTemplate().getType());
                            servicePanel.add(abstractServicePanel);
                            servicePanelMap.put(serviceConfig.getServiceTemplate().getType(), abstractServicePanel);
                            componentList.add(servicePanel);
                        } catch (CouldNotPerformException ex) {
                            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not load service panel for ServiceType[" + serviceConfig.getServiceTemplate().getType().name() + "]", ex), logger, LogLevel.ERROR);
                        }
                    }
                    servicePanelMap.get(serviceConfig.getServiceTemplate().getType()).bindServiceConfig(serviceConfig);
                } catch (CouldNotPerformException | NullPointerException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not configure service panel for ServiceType[" + serviceConfig.getServiceTemplate().getType().name() + "]", ex), logger, LogLevel.ERROR);
                }

            }

            LayoutGenerator.generateHorizontalLayout(contextPanel, componentList);
            contextPanel.validate();
            contextPanel.revalidate();
            contextScrollPane.validate();
            contextScrollPane.revalidate();
            validate();
            revalidate();
        } catch (CouldNotPerformException | NullPointerException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update config for unit panel!", ex), logger);
        }
    }

    public void updateUnitConfig(UnitConfig unitConfig, ServiceType serviceType, Object serviceAttribute) throws CouldNotPerformException, InterruptedException {
        updateUnitConfig(unitConfig, serviceType);
        String methodName = "get" + StringProcessor.transformUpperCaseToCamelCase(serviceType.toString()).replaceAll("Service", "");
        logger.info("Calling method [" + methodName + "] on remote [" + getRemoteService().getId() + "]");
        getRemoteService().callMethod("set" + StringProcessor.transformUpperCaseToCamelCase(serviceType.toString()).replaceAll("Service", ""), serviceAttribute);
    }

    private Class<? extends AbstractServicePanel> loadServicePanelClass(final ServiceType serviceType) throws CouldNotPerformException {
        String remoteClassName = AbstractServicePanel.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToCamelCase(serviceType.name()) + SERVICE_PANEL_SUFFIX;
        try {
            return (Class<? extends AbstractServicePanel>) getClass().getClassLoader().loadClass(remoteClassName);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not detect service panel class for ServiceType[" + serviceType.name() + "]", ex);
        }
    }

    private AbstractServicePanel instantiatServicePanel(final ServiceConfig serviceConfig, Class<? extends AbstractServicePanel> servicePanelClass, AbstractUnitRemote unitRemote) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            AbstractServicePanel instance = servicePanelClass.newInstance();
            instance.initUnit(unitRemote);
            return instance;
        } catch (NullPointerException | InstantiationException | CouldNotPerformException | IllegalAccessException ex) {
            throw new org.openbase.jul.exception.InstantiationException("Could not instantiate service panel out of ServicePanelClass[" + servicePanelClass.getSimpleName() + "]!", ex);
        }
    }

    @Override
    protected void updateDynamicComponents(GeneratedMessage data) {

//               remoteView.setEnabled(false);
//        remoteView.setUnitConfig(unitConfig);
//        remoteView.setEnabled(true);
    }

    public List<JComponent> getComponentList() {
        return componentList;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        contextScrollPane = new javax.swing.JScrollPane();
        contextPanel = new javax.swing.JPanel();

        setBorder(javax.swing.BorderFactory.createTitledBorder("Remote Control"));

        contextScrollPane.setBorder(null);

        javax.swing.GroupLayout contextPanelLayout = new javax.swing.GroupLayout(contextPanel);
        contextPanel.setLayout(contextPanelLayout);
        contextPanelLayout.setHorizontalGroup(
            contextPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 490, Short.MAX_VALUE)
        );
        contextPanelLayout.setVerticalGroup(
            contextPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 469, Short.MAX_VALUE)
        );

        contextScrollPane.setViewportView(contextPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(contextScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(contextScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel contextPanel;
    private javax.swing.JScrollPane contextScrollPane;
    // End of variables declaration//GEN-END:variables

}
