/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.core;

import org.dc.bco.manager.agent.lib.Agent;
import org.dc.bco.manager.agent.lib.AgentController;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
import org.dc.jul.extension.rsb.com.AbstractEnableableConfigurableController;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.agent.AgentDataType;
import rst.homeautomation.control.agent.AgentDataType.AgentData;
import rst.homeautomation.state.ActivationStateType;
import rst.homeautomation.state.ActivationStateType.ActivationState;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public abstract class AbstractAgent extends AbstractEnableableConfigurableController<AgentDataType.AgentData, AgentDataType.AgentData.Builder, AgentConfig> implements AgentController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
    }

    protected boolean executing;

    private final boolean autostart;

    public AbstractAgent(boolean autostart) throws InstantiationException {
        super(AgentDataType.AgentData.newBuilder());
        this.autostart = autostart;
    }

    @Override
    public void init(final AgentConfig config) throws InitializationException, InterruptedException {
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
                if (!executing) {
                    executing = true;
                    execute();
                }
            } else {
                if (executing) {
                    executing = false;
                    stop();
                }
            }
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update execution state!", ex), logger);
        }
    }

    protected abstract void execute() throws CouldNotPerformException, InterruptedException;

    protected abstract void stop() throws CouldNotPerformException, InterruptedException;

    @Override
    public void enable() throws CouldNotPerformException, InterruptedException {
        super.enable();
        if (autostart) {
            setActivationState(ActivationStateType.ActivationState.newBuilder().setValue(ActivationStateType.ActivationState.State.ACTIVE).build());
        }
    }

    @Override
    public void disable() throws CouldNotPerformException, InterruptedException {
        executing = false;
        setActivationState(ActivationStateType.ActivationState.newBuilder().setValue(ActivationStateType.ActivationState.State.DEACTIVE).build());
        super.disable();
    }
}
