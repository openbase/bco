package org.openbase.bco.app.cloud.connector.mapping.service;

import com.google.protobuf.Message;
import org.openbase.bco.app.cloud.connector.mapping.lib.Mode;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.util.*;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractSingleModeServiceStateProviderMapper<SERVICE_STATE extends Message> extends AbstractModeServiceStateProviderMapper<SERVICE_STATE> {


    public AbstractSingleModeServiceStateProviderMapper(ServiceType serviceType) {
        super(serviceType);
    }

    @Override
    public Map<String, String> getSettings(SERVICE_STATE serviceState) throws CouldNotPerformException {
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
}
