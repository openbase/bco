package org.openbase.bco.registry.message.lib.jp;

/*
 * #%L
 * BCO Registry Message Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.communication.controller.jp.JPScope;
import org.openbase.type.communication.ScopeType.Scope;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class JPMessageRegistryScope extends JPScope {

    public final static String[] COMMAND_IDENTIFIERS = {"--message-registry-scope"};

    public JPMessageRegistryScope() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    protected Scope getPropertyDefaultValue() throws JPNotAvailableException {
        return super.getPropertyDefaultValue().toBuilder().addComponent("registry").addComponent("message").build();
    }

    @Override
    public String getDescription() {
        return "Setup the message registry scope which is used for communication.";
    }
}
