package org.openbase.bco.app.openhab.manager.transform;

/*-
 * #%L
 * BCO Openhab App
 * %%
 * Copyright (C) 2018 openbase.org
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

import com.google.protobuf.Message;
import org.eclipse.smarthome.core.types.Command;
import org.openbase.jul.exception.NotAvailableException;

import java.lang.reflect.InvocationTargetException;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceStateCommandTransformerFactory {

    private static final String TRANSFORMER_CLASSNAME_POSTFIX = "Transformer";

    private static ServiceStateCommandTransformerFactory instance;

    public static synchronized ServiceStateCommandTransformerFactory getInstance() {
        if (instance == null) {
            instance = new ServiceStateCommandTransformerFactory();
        }
        return instance;
    }

    // TODO: load transformer from map if already available
//    private final Map<Message, Com>

    public <S extends Message, C extends Command> ServiceStateCommandTransformer<S, C> getTransformer(S serviceType, C command) throws NotAvailableException {
        final String simpleClassName = serviceType.getClass().getSimpleName() + command.getClass().getSimpleName() + TRANSFORMER_CLASSNAME_POSTFIX;
        final String className = getClass().getPackage().getName() + "." + simpleClassName;
        try {
            return (ServiceStateCommandTransformer<S, C>) getClass().getClassLoader().loadClass(className).getConstructor().newInstance();
        } catch (InstantiationException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException ex) {
            throw new NotAvailableException("ServiceStateCommandTransformer for serviceState[" + serviceType.getClass().getSimpleName() + "] and command[" + command.getClass().getSimpleName() + "]", ex);
        }
    }
}
