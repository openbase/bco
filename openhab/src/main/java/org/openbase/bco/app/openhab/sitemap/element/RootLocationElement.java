package org.openbase.bco.app.openhab.sitemap.element;

import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.rst.processing.LabelProcessor;

public class RootLocationElement extends AbstractUnitSitemapElement {

    public RootLocationElement() throws InstantiationException {
        super();
        try {
            init(Registries.getUnitRegistry().getRootLocationConfig());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    protected String serialize(String sitemap) throws CouldNotPerformException {
        sitemap += prefix + "sitemap bco label=\"" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "\" {" + System.lineSeparator();
        sitemap += prefix + tab() + "Frame label=\"Locations\" {" + System.lineSeparator();
        for (String childId : unitConfig.getLocationConfig().getChildIdList()) {
            sitemap = new LocationElement(childId, this).appendElement(sitemap);
        }
        sitemap += prefix + tab() + "}" + System.lineSeparator();
        sitemap += prefix + "}" + System.lineSeparator();
        return sitemap;
    }
}