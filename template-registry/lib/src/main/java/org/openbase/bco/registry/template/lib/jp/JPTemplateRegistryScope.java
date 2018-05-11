package org.openbase.bco.registry.template.lib.jp;

import org.openbase.jul.extension.rsb.scope.jp.JPScope;
import rsb.Scope;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class JPTemplateRegistryScope extends JPScope {

    public final static String[] COMMAND_IDENTIFIERS = {"--template-registry-scope"};

    public JPTemplateRegistryScope() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    protected Scope getPropertyDefaultValue() {
        return super.getPropertyDefaultValue().concat(new Scope("/registry/template"));
    }

    @Override
    public String getDescription() {
        return "Setup the template registry scope which is used for the rsb communication.";
    }
}
