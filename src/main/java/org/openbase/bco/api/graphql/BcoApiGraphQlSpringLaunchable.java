package org.openbase.bco.api.graphql;

import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class BcoApiGraphQlSpringLaunchable implements Launchable<Void>, VoidInitializable {

    private static Logger LOGGER = LoggerFactory.getLogger(BcoApiGraphQlSpringLaunchable.class);

    private ConfigurableApplicationContext context;

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        LOGGER.info("Connect to bco...");
        Registries.waitUntilReady();

        LOGGER.info("Login to bco...");
        BCOLogin.getSession().autoLogin(true);

        LOGGER.info("Start webserver...");
        context = SpringApplication.run(BcoGraphQlApiSpringBootApplication.class);
    }

    @Override
    public void deactivate() {

        LOGGER.info("Login to bco...");
        BCOLogin.getSession().logout();

        LOGGER.info("Start webserver...");
        SpringApplication.exit(context);
        context = null;
    }

    @Override
    public boolean isActive() {
        return context != null;
    }

    @Override
    public void init() {
        // nothing to initialize
    }
}
