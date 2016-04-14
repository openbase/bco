package org.dc.bco.manager.agent.binding.openhab;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.bco.manager.agent.binding.openhab.comm.OpenHABCommunicatorImpl;
import org.dc.jps.exception.JPNotAvailableException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.extension.openhab.binding.AbstractOpenHABBinding;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public class OpenHABBindingImpl extends AbstractOpenHABBinding {

    public OpenHABBindingImpl() throws InstantiationException, JPNotAvailableException {
        super(new OpenHABCommunicatorImpl());
    }
}
