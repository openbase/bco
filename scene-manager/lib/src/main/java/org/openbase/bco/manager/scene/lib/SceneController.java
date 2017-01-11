package org.openbase.bco.manager.scene.lib;

/*
 * #%L
 * BCO Manager Scene Library
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.protobuf.MessageController;
import org.openbase.jul.iface.Enableable;
import org.openbase.jul.iface.Identifiable;
import rst.domotic.unit.scene.SceneDataType.SceneData;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface SceneController extends Identifiable<String>, Enableable, Scene, MessageController<SceneData, SceneData.Builder> {

    public void init(final UnitConfig config) throws InitializationException, InterruptedException;
}
