package org.openbase.bco.app.openhab.sitemap.element;

import org.openbase.jul.iface.Initializable;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public abstract class AbstractUnitSitemapElement extends AbstractSitemapElement implements Initializable<UnitConfig> {

    protected UnitConfig unitConfig;

    public AbstractUnitSitemapElement() {
    }

    public AbstractUnitSitemapElement(AbstractSitemapElement parentElement) {
        super(parentElement);
    }

    @Override
    public void init(UnitConfig unitConfig) {
        this.unitConfig = unitConfig;
    }
}
