package org.openbase.bco.device.openhab.sitemap;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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

import org.openbase.bco.device.openhab.sitemap.element.SitemapElement;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.LoggerFactory;

public interface SitemapBuilder {

    int TAB_SIZE = 4;
    String TAB = StringProcessor.fillWithSpaces("", TAB_SIZE);

    SitemapBuilder append(final String element);

    default SitemapBuilder append(final SitemapElement element) {
        try {
            element.serialize(this);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not serialize sitemap Element[" + this + "]", ex, LoggerFactory.getLogger(SitemapBuilder.class));
        }
        return this;
    }

    String build();

    SitemapBuilder increaseTabLevel();

    SitemapBuilder decreaseTabLevel();

    SitemapBuilder openContext(final SitemapElementType sitemapElementType, final String... headerElement);

    SitemapBuilder addElement(final SitemapElementType sitemapElementType, final String... elementArguments);

    SitemapBuilder closeContext();

    SitemapBuilder openTextContext(final String label, final SitemapIconType icon);

    SitemapBuilder openFrameContext(final String label, final SitemapIconType icon);

    default SitemapBuilder openFrameContext(final String label) {
        return openFrameContext(label, null);
    }

    SitemapBuilder addTextElement(final String item);

    SitemapBuilder addTextElement(final String item, final String label);

    SitemapBuilder addTextElement(final String item, final String label, final SitemapIconType icon);

    SitemapBuilder addSwitchElement(final String item, final SitemapIconType icon);

    SitemapBuilder addSwitchElement(final String item, final String label, final SitemapIconType icon);

    SitemapBuilder addGroupElement(final String item);

    default SitemapBuilder addSliderElement(final String item) {
        return addSliderElement(item, "");
    }

    SitemapBuilder addSliderElement(final String item, final String label);

    SitemapBuilder addSliderElement(final String item, final String label, final SitemapIconType icon);

    SitemapBuilder addColorpickerElement(final String item, final String label, final SitemapIconType icon);

    SitemapBuilder addDefaultElement(final String item, final String label);

    enum SitemapIconType {
        BATTERY,
        CHART,
        COLORWHEEL,
        STATUS,
        DOOR,
        WINDOW,
        CONTACT,
        CORRIDOR,
        ENERGY,
        PRESSURE,
        ERROR,
        FIRE,
        FLOW,
        HEATING,
        LIGHT,
        MOTION,
        NONE,
        ROLLERSHUTTER,
        SETTINGS,
        SIREN,
        SUN,
        SWITCH,
        TEMPERATURE,
        VIDEO,
        SHIELD,
        WALLSWITCH;
    }

    enum SitemapElementType {
        CHART,
        COLORPICKER,
        DEFAULT,
        FRAME,
        GROUP,
        IMAGE,
        MAPVIEW,
        SELECTION,
        SETPOINT,
        SLIDER,
        SWITCH,
        TEXT,
        VIDEO,
        WEBVIEW;

        public String getName() {
            return StringProcessor.transformFirstCharToUpperCase(name().toLowerCase());
        }
    }

}
