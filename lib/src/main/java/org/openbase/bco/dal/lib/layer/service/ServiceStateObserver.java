package org.openbase.bco.dal.lib.layer.service;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;

/**
 * An observer which is can be set to filter empty service updates.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class ServiceStateObserver implements Observer<Message> {

    private final boolean filterEmptyUpdates;

    public ServiceStateObserver(final boolean filterEmptyUpdates) {
        this.filterEmptyUpdates = filterEmptyUpdates;
    }

    @Override
    public void update(Observable<Message> source, Message serviceData) throws Exception {
        if(filterEmptyUpdates && ProtoBufFieldProcessor.isMessageEmpty(serviceData)) {
            return;
        }

        updateServiceData(source, serviceData);
    }

    public abstract void updateServiceData(Observable<Message> source, Message data) throws Exception;
}
