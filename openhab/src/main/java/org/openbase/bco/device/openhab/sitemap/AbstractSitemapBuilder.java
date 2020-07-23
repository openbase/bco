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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.LoggerFactory;
import org.openbase.type.language.LabelType.Label;

import static org.openbase.bco.device.openhab.sitemap.SitemapBuilder.SitemapElementType.*;

public abstract class AbstractSitemapBuilder implements SitemapBuilder {

    protected static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbstractSitemapBuilder.class);
    private static final String OPEN_BODY = " {" + System.lineSeparator();
    private static final String CLOSE_BODY = "}" + System.lineSeparator();
    private final String header;
    private String prefix;
    private String body;
    private int bodyTabLevel;

    public AbstractSitemapBuilder(final String filename, final Label label) throws InstantiationException {
        try {
            this.bodyTabLevel = 1;
            this.prefix = "";
            this.header = "sitemap " + filename + " " + label(LabelProcessor.getBestMatch(label));
            this.body = "";
            this.updatePrefix();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public SitemapBuilder append(final String element) {
        body += prefix + element;
        return this;
    }

    @Override
    public SitemapBuilder openContext(final SitemapElementType sitemapElementType, final String... headerElement) {
        String contextHeader = sitemapElementType.getName();
        for (String value : headerElement) {
            if (value.isEmpty()) {
                continue;
            }
            contextHeader += " " + value;
        }

        body += prefix + contextHeader + OPEN_BODY;
        increaseTabLevel();
        return this;
    }

    @Override
    public SitemapBuilder closeContext() {

        // validate that this context is not empty, otherwise remove the entire context.
        if(body.endsWith(OPEN_BODY)) {

            LOGGER.debug("Empty Context detected which will be removed to guarantee correct sitemap syntax.");

            // remove body part
            body = body.substring(0, body.lastIndexOf(OPEN_BODY));

            // remove context header
            body = body.substring(0, body.lastIndexOf(System.lineSeparator()) + System.lineSeparator().length());

            // reset tab level
            decreaseTabLevel();

            // after removal we are finish with this body.
            return this;
        }

        decreaseTabLevel();
        body += prefix + CLOSE_BODY;
        return this;
    }

    @Override
    public SitemapBuilder addElement(final SitemapElementType sitemapElementType, final String... elementArguments) {
        String element = sitemapElementType.getName();
        for (String value : elementArguments) {
            if (value.isEmpty()) {
                continue;
            }
            element += " " + value;
        }

        body += prefix + element + System.lineSeparator();
        return this;
    }

    @Override
    public String build() {
        return header + OPEN_BODY + body + CLOSE_BODY;
    }

    @Override
    public SitemapBuilder increaseTabLevel() {
        bodyTabLevel++;
        updatePrefix();
        return this;
    }

    @Override
    public SitemapBuilder decreaseTabLevel() {
        bodyTabLevel--;
        updatePrefix();
        return this;
    }

    private void updatePrefix() {
        prefix = StringProcessor.fillWithSpaces("", bodyTabLevel * TAB_SIZE);
    }

    private String str(final String string) {
        return "\"" + string + "\"";
    }

    private String label(final String label) {
        return "label=" + str(label);
    }

    private String item(final String item) {
        if (item.isEmpty() || item == null) {
            return "";
        }
        return "item=" + item;
//        return "item=Date";
    }

    private String icon(SitemapIconType icon) {
        if (icon == null) {
            return "";
        }
        return "icon=" + str(icon.name().toLowerCase());
    }

    @Override
    public SitemapBuilder openTextContext(final String label, final SitemapIconType icon) {
        openContext(TEXT, label(label), icon(icon));
        return this;
    }

    @Override
    public SitemapBuilder openFrameContext(final String label, final SitemapIconType icon) {
        openContext(FRAME, label(label), icon(icon));
        return this;
    }

    @Override
    public SitemapBuilder addTextElement(final String item) {
        return addElement(TEXT, item(item));
    }

    @Override
    public SitemapBuilder addTextElement(final String item, final String label) {
        return addElement(TEXT, item(item), label(label));
    }

    @Override
    public SitemapBuilder addTextElement(final String item, final String label, final SitemapIconType icon) {
        return addElement(TEXT, item(item), label(label), icon(icon));
    }

    @Override
    public SitemapBuilder addSwitchElement(final String item, final SitemapIconType icon) {
        return addElement(SWITCH, item(item), icon(icon));
    }


    @Override
    public SitemapBuilder addSwitchElement(final String item, final String label, final SitemapIconType icon) {
        return addElement(SWITCH, item(item), label(label), icon(icon));
    }

    @Override
    public SitemapBuilder addGroupElement(final String item) {
        return addElement(GROUP, item(item));
    }

    @Override
    public SitemapBuilder addSliderElement(final String item, final String label) {
        return addElement(SLIDER, item(item), label(label));
    }

    @Override
    public SitemapBuilder addSliderElement(final String item, final String label, final SitemapIconType icon) {
        return addElement(SLIDER, item(item), label(label), icon(icon));
    }

    @Override
    public SitemapBuilder addColorpickerElement(final String item, final String label, final SitemapIconType icon) {
        return addElement(COLORPICKER, item(item), label(label), icon(icon));
    }

    @Override
    public SitemapBuilder addDefaultElement(final String item, final String label) {
        return addElement(DEFAULT, item(item), label(label));
    }
}
