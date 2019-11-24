package org.openbase.app.test.agent;

/*-
 * #%L
 * BCO App Test Framework
 * %%
 * Copyright (C) 2018 - 2019 openbase.org
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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.jp.JPAuthentication;
import org.openbase.bco.dal.remote.action.Actions;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.dal.remote.layer.unit.app.AppRemote;
import org.openbase.bco.dal.remote.layer.unit.util.UnitStateAwaiter;
import org.openbase.bco.dal.test.AbstractBCOTest;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.domotic.state.ActivationStateType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType;
import org.openbase.type.domotic.unit.app.AppClassType.AppClass;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public abstract class AbstractBCOAppManagerTest extends BCOAppTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbstractBCOAgentManagerTest.class);

    protected AppClass appClass = null;
    protected UnitConfig appConfig = null;
    protected AppRemote appRemote = null;

    @BeforeClass
    public static void setUpClass() throws Throwable {
        BCOAppTest.setUpClass();
    }

    @AfterClass
    public static void tearDownClass() throws Throwable {
        BCOAppTest.tearDownClass();
    }

    @Before
    public void setUp() throws Exception {
        try {
            // setup and register app class
            AppClass.Builder appClassBuilder = AppClass.newBuilder();
            LabelProcessor.addLabel(appClassBuilder.getLabelBuilder(), Locale.ENGLISH, getAppClass().getSimpleName().replace("App", ""));
            this.appClass = Registries.getClassRegistry().registerAppClass(appClassBuilder.build()).get(5, TimeUnit.SECONDS);

            UnitConfig.Builder appConfigBuilder = getAppConfig();
            appConfigBuilder.getAppConfigBuilder().setAppClassId(this.appClass.getId());
            appConfigBuilder.setUnitType(UnitTemplateType.UnitTemplate.UnitType.APP);
            // register app
            this.appConfig = Registries.getUnitRegistry().registerUnitConfig(appConfigBuilder.build()).get(5, TimeUnit.SECONDS);
            // retrieve remote and activate app
            this.appRemote = Units.getUnit(this.appConfig, true, Units.APP);
            if (!this.appConfig.getAppConfig().getAutostart()) {
                // activate app if not in auto start
                waitForExecution(this.appRemote.setActivationState(ActivationStateType.ActivationState.newBuilder().setValue(ActivationStateType.ActivationState.State.ACTIVE).build()));
            } else {
                // wait until active
                new UnitStateAwaiter<>(this.appRemote).waitForState(data -> data.getActivationState().getValue() == ActivationStateType.ActivationState.State.ACTIVE);
            }
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, LOGGER);
        }
    }

    @After
    public void tearDown() {
    }

    public abstract Class getAppClass();

    public abstract UnitConfig.Builder getAppConfig() throws CouldNotPerformException;
}
