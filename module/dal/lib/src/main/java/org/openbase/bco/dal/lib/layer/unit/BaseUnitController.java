package org.openbase.bco.dal.lib.layer.unit;

/*-
 * #%L
 * BCO DAL Library
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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * @param <D> the data type of this unit used for the state synchronization.
 * @param <DB> the builder used to build the unit data instance.
 */
public interface BaseUnitController<D extends AbstractMessage, DB extends D.Builder<DB>> extends UnitController<D, DB>, BaseUnit<D> {

}
