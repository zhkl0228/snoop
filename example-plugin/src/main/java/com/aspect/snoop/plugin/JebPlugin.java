package com.aspect.snoop.plugin;

import com.fuzhu8.inspector.plugin.AbstractPlugin;
import com.fuzhu8.inspector.plugin.Appender;
import com.fuzhu8.inspector.plugin.SnoopPlugin;
import com.fuzhu8.inspector.plugin.Plugin;

import java.lang.instrument.Instrumentation;

@SnoopPlugin
@SuppressWarnings("unused")
public class JebPlugin extends AbstractPlugin implements Plugin {

    public JebPlugin(Instrumentation inst, Appender appender) {
        super(inst, appender);

        this.debug = false;
    }

    @Override
    protected void initialize() {
        System.out.println("Initialize crack jeb plugin");

        registerClassTransformer("com/pnfsoftware/jeb/client/AbstractContext", new AbstractContextTransformer(appender));
        registerClassTransformer("com/pnfsoftware/jeb/client/JebNet", new JebNetTransformer(appender));
        registerClassTransformer("com/pnfsoftware/jeb/util/base/Flags", new FlagsTransformer(appender));
        registerClassTransformer("com/pnfsoftware/jeb/client/Licensing", new LicensingTransformer(appender));

        registerClassTransformer("java/lang/Runtime", new RuntimeTransformer(appender));
    }

}
