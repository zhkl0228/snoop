package com.aspect.snoop.plugin;

import com.fuzhu8.inspector.plugin.AbstractPlugin;
import com.fuzhu8.inspector.plugin.Appender;
import com.fuzhu8.inspector.plugin.InspectorPlugin;
import com.fuzhu8.inspector.plugin.Plugin;

@InspectorPlugin
@SuppressWarnings("unused")
public class JebPlugin extends AbstractPlugin implements Plugin {

    public JebPlugin(Appender appender) {
        super(appender);
    }

    @Override
    protected void initialize() {
        System.out.println("Initialize crack jeb plugin");

        registerClassTransformer("com/pnfsoftware/jeb/client/AbstractContext", new AbstractContextTransformer(appender));
        registerClassTransformer("com/pnfsoftware/jeb/client/JebNet", new JebNetTransformer(appender));
        registerClassTransformer("com/pnfsoftware/jeb/util/base/Flags", new FlagsTransformer(appender));
        registerClassTransformer("com/pnfsoftware/jeb/client/Licensing", new LicensingTransformer(appender));
    }

}
