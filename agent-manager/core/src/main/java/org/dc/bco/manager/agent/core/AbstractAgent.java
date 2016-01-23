/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.core;

import org.dc.bco.manager.agent.lib.Agent;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.com.RSBCommunicationService;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.agent.AgentDataType;
import rst.homeautomation.control.agent.AgentDataType.AgentData;
import rst.homeautomation.state.ActivationStateType.ActivationState;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public abstract class AbstractAgent extends RSBCommunicationService<AgentDataType.AgentData, AgentDataType.AgentData.Builder> implements AgentController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
    }

    protected boolean executing;
    protected AgentConfig config;
    private final boolean autostart;

    public AbstractAgent(boolean autostart) throws InstantiationException {
        super(AgentDataType.AgentData.newBuilder());
        this.autostart = autostart;
    }

    @Override
    public void init(final AgentConfig config) throws InitializationException {
        this.config = config;
        this.executing = false;
        logger.info("Initializing " + getClass().getSimpleName() + "[" + config.getId() + "]");
        super.init(config.getScope());
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Agent.class, this, server);
    }

    @Override
    public void setActivationState(final ActivationState activation) throws CouldNotPerformException {
        if (activation.getValue().equals(ActivationState.State.UNKNOWN)) {
            throw new InvalidStateException("Unknown is not a valid state!");
        }

        if (activation.getValue().equals(getData().getActivationState().getValue())) {
            return;
        }

        try (ClosableDataBuilder<AgentData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setActivationState(activation);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply data change!", ex);
        }

        try {
            if (activation.getValue().equals(ActivationState.State.ACTIVE)) {
                if (executing) {
                    executing = true;
                    execute();
                } else {
                    executing = true;
                }
            } else {
                if (executing) {
                    executing = false;
                    stop();
                } else {
                    executing = false;
                }

            }
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update execution state!", ex), logger);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        if (autostart) {
            setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.ACTIVE).build());
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        executing = false;
        setActivationState(ActivationState.newBuilder().setValue(ActivationState.State.DEACTIVE).build());
        super.deactivate();
    }

    protected abstract void execute() throws CouldNotPerformException, InterruptedException;

    protected abstract void stop() throws CouldNotPerformException, InterruptedException;

    @Override
    public String getId() throws NotAvailableException {
        return config.getId();
    }

    @Override
    public AgentConfig getConfig() throws NotAvailableException {
        return config;
    }

    @Override
    public AbstractAgent update(AgentConfig config) throws CouldNotPerformException {
        this.config = config;
        return this;
    }
}
