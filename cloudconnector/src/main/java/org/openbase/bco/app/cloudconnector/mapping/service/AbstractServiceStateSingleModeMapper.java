package org.openbase.bco.app.cloudconnector.mapping.service;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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
import org.openbase.bco.app.cloudconnector.mapping.lib.Mode;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractServiceStateSingleModeMapper<SERVICE_STATE extends Message> extends AbstractServiceStateModesMapper<SERVICE_STATE> {

    public AbstractServiceStateSingleModeMapper(ServiceType serviceType) {
        super(serviceType);
    }

    @Override
    public SERVICE_STATE getServiceState(final Map<String, String> modeNameSettingNameMap) throws CouldNotPerformException {
        if (modeNameSettingNameMap.size() > 1) {
            throw new CouldNotPerformException("Mapper for single modes received command with [" + modeNameSettingNameMap.size() + "] modes");
        }

        return getServiceState(modeNameSettingNameMap.get(getMode().getName()));
    }

    @Override
    public Map<String, String> getSettings(final SERVICE_STATE serviceState) throws CouldNotPerformException {
        final Map<String, String> modeSettingMap = new HashMap<>();
        modeSettingMap.put(getMode().getName(), getSetting(serviceState));
        return modeSettingMap;
    }

    @Override
    public List<Mode> getModes() {
        return Collections.singletonList(getMode());
    }

    public abstract Mode getMode();

    public abstract String getSetting(final SERVICE_STATE serviceState) throws CouldNotPerformException;

    public abstract SERVICE_STATE getServiceState(final String settingName) throws CouldNotPerformException;
}
