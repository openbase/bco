/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.scene.core;

import org.dc.bco.manager.scene.lib.Scene;
import org.dc.bco.manager.scene.lib.SceneController;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBCommunicationService;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.control.scene.SceneDataType.SceneData;

/**
 *
 * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class SceneControllerImpl extends RSBCommunicationService<SceneData, SceneData.Builder> implements SceneController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SceneData.getDefaultInstance()));
    }

    protected SceneConfig config;

    public SceneControllerImpl() throws org.dc.jul.exception.InstantiationException {
        super(SceneData.newBuilder());
    }

    @Override
    public void init(final SceneConfig config) throws InitializationException, InterruptedException {
        this.config = config;
        logger.info("Initializing " + getClass().getSimpleName() + "[" + config.getId() + "]");
        super.init(config.getScope());
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Scene.class, this, server);
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
    }

    @Override
    public String getId() throws NotAvailableException {
        return config.getId();
    }

    @Override
    public SceneConfig getConfig() throws NotAvailableException {
        return config;
    }

    @Override
    public SceneConfig updateConfig(SceneConfig config) throws CouldNotPerformException {
        this.config = config;
        return config;
    }
}
