package org.openbase.bco.app.openhab.sitemap.element;

import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.rst.processing.LabelProcessor;

public class LocationElement extends AbstractUnitSitemapElement {

    public LocationElement(final String unitId, AbstractSitemapElement parentElement)throws InstantiationException {
        super(parentElement);
        try {
            init(Registries.getUnitRegistry().getUnitConfigById(unitId));
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    protected String serialize(String sitemap) throws CouldNotPerformException {
        sitemap += prefix + "Text item=\"" +unitConfig.getAlias(0)+"\" label=\""+LabelProcessor.getBestMatch(unitConfig.getLabel())+"\" icon=\"video\" {" + System.lineSeparator();
       // sitemap += prefix + tab() + "Switch item="+unitConfig.getUnitType().name()+" icon=\"Light\""  + System.lineSeparator();
        sitemap += prefix + tab() + "Switch item=\"" +unitConfig.getAlias(0)+"\" label=\"Power\" icon=\"light\""  + System.lineSeparator();
        for (String childId : unitConfig.getLocationConfig().getChildIdList()) {
            sitemap = new LocationElement(childId, this).appendElement(sitemap);
        }
        sitemap += prefix + "}" + System.lineSeparator();
        return sitemap;
    }
}