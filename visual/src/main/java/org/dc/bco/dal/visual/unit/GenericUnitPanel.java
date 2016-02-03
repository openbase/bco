/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.visual.unit;

import com.google.protobuf.GeneratedMessage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.dc.bco.dal.remote.unit.AbstractUnitRemote;
import org.dc.jul.extension.rsb.com.AbstractIdentifiableRemote;
import org.dc.bco.dal.visual.service.AbstractServicePanel;
import org.dc.bco.dal.visual.util.RSBRemoteView;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import org.dc.jul.processing.StringProcessor;
import org.dc.jul.visual.layout.LayoutGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author mpohling
 */
public class GenericUnitPanel<RS extends AbstractUnitRemote> extends RSBRemoteView<RS> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Observer<UnitConfig> unitConfigObserver;
    private boolean autoRemove;
    private List<JComponent> componentList;

    /**
     * Creates new form AmbientLightView
     */
    public GenericUnitPanel() {
        super();
        this.unitConfigObserver = (Observable<UnitConfig> source, UnitConfig data) -> {
            updateUnitConfig(data);
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

    public void updateUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        updateUnitConfig(unitConfig, ServiceType.UNKNOWN);

    }

    public void updateUnitConfig(UnitConfig unitConfig, ServiceType serviceType) throws CouldNotPerformException, InterruptedException {
        setUnitRemote(unitConfig);

        if (autoRemove) {
            contextPanel.removeAll();
        }
        componentList = new ArrayList<>();
        JPanel servicePanel;

        for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {

            if (serviceType != ServiceType.UNKNOWN && serviceConfig.getType() != serviceType) {
                continue;
            }

            try {
                servicePanel = new JPanel();
                servicePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(StringProcessor.transformUpperCaseToCamelCase(serviceConfig.getType().name()) + ":" + unitConfig.getId()));
                AbstractServicePanel abstractServicePanel = instantiatServicePanel(serviceConfig, loadServicePanelClass(serviceConfig.getType()), getRemoteService());
                abstractServicePanel.setUnitId(unitConfig.getId());
                abstractServicePanel.setServiceType(serviceType);
                servicePanel.add(abstractServicePanel);
                componentList.add(servicePanel);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not load service panel for ServiceType[" + serviceConfig.getType().name() + "]", ex), logger, LogLevel.ERROR);
            }
        }

        String remoteLabel = StringProcessor.transformUpperCaseToCamelCase(unitConfig.getType().name())
                + "[" + unitConfig.getLabel() + "]"
                + " @ " + unitConfig.getPlacementConfig().getLocationId()
                + " of " + unitConfig.getDeviceId()
                + " : " + unitConfig.getDescription();

        setBorder(BorderFactory.createTitledBorder("Remote Control - " + remoteLabel));

        LayoutGenerator.designList(contextPanel, componentList);
        contextPanel.validate();
        contextPanel.revalidate();
        contextScrollPane.validate();
        contextScrollPane.revalidate();
        validate();
        revalidate();
    }

    private Class<? extends AbstractServicePanel> loadServicePanelClass(final ServiceType serviceType) throws CouldNotPerformException {
        String remoteClassName = AbstractServicePanel.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToCamelCase(serviceType.name()) + "Panel";
        try {
            return (Class<? extends AbstractServicePanel>) getClass().getClassLoader().loadClass(remoteClassName);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not detect service panel class for ServiceType[" + serviceType.name() + "]", ex);
        }
    }

    private AbstractServicePanel instantiatServicePanel(final ServiceConfig serviceConfig, Class<? extends AbstractServicePanel> servicePanelClass, AbstractUnitRemote unitRemote) throws org.dc.jul.exception.InstantiationException {
        try {
            AbstractServicePanel instance = servicePanelClass.newInstance();
            instance.initService(serviceConfig, unitRemote, unitRemote);
            return instance;
        } catch (NullPointerException | InstantiationException | IllegalAccessException ex) {
            throw new org.dc.jul.exception.InstantiationException("Could not instantiate service panel out of ServicePanelClass[" + servicePanelClass.getSimpleName() + "]!", ex);
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
