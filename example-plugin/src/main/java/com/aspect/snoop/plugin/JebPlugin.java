package com.aspect.snoop.plugin;

import com.fuzhu8.inspector.plugin.AbstractPlugin;
import com.fuzhu8.inspector.plugin.Appender;
import com.fuzhu8.inspector.plugin.Plugin;
import com.fuzhu8.inspector.plugin.SnoopPlugin;

import java.lang.instrument.Instrumentation;

@SnoopPlugin
public class JebPlugin extends AbstractPlugin implements Plugin {

    public JebPlugin(Instrumentation inst, Appender appender) {
        super(inst, appender);

        this.debug = false;
    }

    @Override
    protected void initialize() {
        System.out.println("Initialize crack jeb plugin");

        registerClassTransformer(new AbstractContextTransformer(appender));
        registerClassTransformer(new JebNetTransformer(appender));
        registerClassTransformer(new FlagsTransformer(appender));
        registerClassTransformer(new LicensingTransformer(appender));

        registerClassTransformer(new RuntimeTransformer(appender));
    }

}
