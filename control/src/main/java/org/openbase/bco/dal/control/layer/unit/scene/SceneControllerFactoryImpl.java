package org.openbase.bco.dal.control.layer.unit.scene;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.bco.dal.lib.layer.unit.scene.Scene;
import org.openbase.bco.dal.lib.layer.unit.scene.SceneController;
import org.openbase.bco.dal.lib.layer.unit.scene.SceneControllerFactory;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneControllerFactoryImpl implements SceneControllerFactory {

    protected final Logger logger = LoggerFactory.getLogger(SceneControllerFactoryImpl.class);
    private static SceneControllerFactoryImpl instance;

    public synchronized static SceneControllerFactoryImpl getInstance() {

        if (instance == null) {
            instance = new SceneControllerFactoryImpl();
        }
        return instance;
    }

    private SceneControllerFactoryImpl() {

    }

    @Override
    public SceneController newInstance(final UnitConfig config) throws org.openbase.jul.exception.InstantiationException {
        SceneController scene;
        try {
            if (config == null) {
                throw new NotAvailableException("UnitConfig");
            }
            scene = new SceneControllerImpl();
            scene.init(config);
        } catch (CouldNotPerformException | SecurityException | IllegalArgumentException | InterruptedException ex) {
            throw new org.openbase.jul.exception.InstantiationException(Scene.class, config.getId(), ex);
        }
        return scene;
    }
}
