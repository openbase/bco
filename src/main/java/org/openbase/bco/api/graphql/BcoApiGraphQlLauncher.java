package org.openbase.bco.api.graphql;

import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.authentication.lib.jp.JPCredentialsDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jul.extension.rsb.com.jp.*;
import org.openbase.jul.pattern.launch.AbstractLauncher;

public class BcoApiGraphQlLauncher extends AbstractLauncher<BcoApiGraphQlSpringLaunchable>  {

    public BcoApiGraphQlLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(BcoApiGraphQlLauncher.class, BcoApiGraphQlSpringLaunchable.class);
    }

    @Override
    protected void loadProperties() {
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPCredentialsDirectory.class);
        JPService.registerProperty(JPRSBHost.class);
        JPService.registerProperty(JPRSBPort.class);
        JPService.registerProperty(JPRSBTransport.class);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        BCO.printLogo();
        AbstractLauncher.main(BCO.class, BcoApiGraphQlLauncher.class, args, BcoApiGraphQlLauncher.class);
    }
}
