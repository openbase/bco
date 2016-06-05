package org.dc.bco.manager.app.core;

/*
 * #%L
 * COMA AppManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.dc.bco.manager.app.lib.App;
import org.dc.bco.manager.app.lib.AppController;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.extension.rsb.com.AbstractExecutableController;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.homeautomation.control.app.AppDataType.AppData;
import rst.homeautomation.state.ActivationStateType.ActivationState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public abstract class AbstractApp extends AbstractExecutableController<AppData, AppData.Builder, AppConfig> implements AppController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AppData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
    }

    public AbstractApp(boolean autostart) throws org.dc.jul.exception.InstantiationException {
        super(AppData.newBuilder(), autostart);
    }

    @Override
    public void init(final AppConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        logger.info("Initializing " + getClass().getSimpleName() + "[" + config.getId() + "]");
    }

    @Override
    public void registerMethods(final RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(App.class, this, server);
    }
}
