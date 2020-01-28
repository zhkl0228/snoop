/*
 * Copyright, Aspect Security, Inc.
 *
 * This file is part of JavaSnoop.
 *
 * JavaSnoop is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JavaSnoop is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaSnoop.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aspect.snoop.agent;

import com.aspect.snoop.agent.manager.InstrumentationManager;
import com.aspect.snoop.ui.JavaSnoopView;
import de.robv.android.xposed.XposedBridge;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;

public class SnoopAgent {

    private static Instrumentation inst;
    private static InstrumentationManager manager;

    private static JavaSnoopView mainView;
    private static final int defaultGuiDelay = 5;
    
    public static JavaSnoopView getMainView() {
        return mainView;
    }

    public static void premain(String args, Instrumentation instrumentation) {
        install(args,instrumentation);
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        turnOffSecurity();
        install(args,instrumentation);
    }

    @SuppressWarnings("restriction")
    private static void install(String args, Instrumentation instrumentation) {
    	ClassLoader classLoader = SnoopAgent.class.getClassLoader();
    	while(classLoader != null) {
    		classLoader = classLoader.getParent();
    	}
    	
        String[] arguments = args.split("\\|");
        File tmpFile = new File(arguments[0]);
        AgentLogger.setLogDir(tmpFile.getParentFile());

        /*
         * We need to add some of the libraries to be eventually
         * loaded by the system class loader instead of the
         * boostrap class loader, which is represented as null in
         * Java land. This causes problems.
         */
        AgentLogger.debug("Adding runtime jars to system classloader...");

        List<File> classFiles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(tmpFile))) {
            String entry;
            while ((entry = reader.readLine()) != null) {
                if (entry.endsWith(".jar")) {
                    AgentLogger.debug("Adding runtime jar: " + entry);
                    File file = new File(entry);
                    classFiles.add(file);
                    if (entry.contains("log4j")) {
                        instrumentation.appendToSystemClassLoaderSearch(new JarFile(file));
                    } else {
                        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(file));
                    }
                }
            }
        } catch (IOException ex) {
            AgentLogger.fatal(ex);
        } finally {

            if (!tmpFile.delete()) {
                tmpFile.deleteOnExit();
            }
        }
		/*URL[] urls = sun.misc.Launcher.getBootstrapClassPath().getURLs();
        for (URL url : urls) {
            // log.debug("bootstrapClassPath=" + url.toExternalForm());
        }*/
        
        inst = instrumentation;
        AgentLogger.debug("Loading manager...");
        manager = new InstrumentationManager(inst, classFiles);
        XposedBridge.initialize(inst);
        AgentLogger.debug("Done with manager, XposedBridge loaded by: " + XposedBridge.class.getClassLoader());

        turnOffSecurity();

        AgentLogger.debug("Turned off security...");

        int guiDelay = defaultGuiDelay;

        try {
            guiDelay = Integer.parseInt(arguments[1]);
        } catch (Exception e) {
            AgentLogger.error("Gui delay wasn't supplied in agent arguments: " +
                    "defaulting to " + defaultGuiDelay,e);
        }

        final int delay = guiDelay;

        final String lookAndFeelOverride;
        
        if ( arguments.length > 2 ) {
            lookAndFeelOverride = arguments[2];
        } else {
            lookAndFeelOverride = null;
        }
        
        boolean showUI = true;
        if(arguments.length > 3) {
        	showUI = Boolean.parseBoolean(arguments[3]);
        }
        
        String mainClass = arguments[4];
        int pid = Integer.parseInt(arguments[5]);
        String libraryPath = arguments[6];
        
        if(pid != -1) {
        	try {
        		Class<?> starterClass = SnoopAgent.class.getClassLoader().loadClass("com.aspect.snoop.agent.InspectorStarter");
                Method method = starterClass.getDeclaredMethod("startInspector", File.class, String.class, int.class, String.class, Instrumentation.class);
                method.setAccessible(true);
                method.invoke(null, tmpFile.getParentFile(), mainClass, pid, libraryPath, instrumentation);
        	} catch(Exception e) {
        		AgentLogger.error(e);
        	}
        }
        
        if(!showUI) {
        	return;
        }

        AgentLogger.trace("Queueing GUI to run...");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                /*
                 * I apologize profusely for this hack, but I can't think of a
                 * way to avoid it.
                 * 
                 * Lemma 1: The JavaSnoop GUI must run within the targeted
                 *          process so its reflection model and objects can
                 *          be accessed directly.
                 * Lemma 2: You can't have 2 different Look and Feels operating
                 *          at the same time in a single JVM.
                 *
                 * This means that our GUIs will crash. If I set the look and
                 * feel, it may override what the target process developers
                 * intended. This would suck. So what we do to avoid it is
                 * pause for a few seconds (default is 5), and give the
                 * target process time to initialize Swing. If the process
                 * has a long non-UI buildup, you may have to increase this
                 * value.
                 */
                if (delay!=0)
                    AgentLogger.trace("Delaying " + delay + " seconds...");
                
                try {
                    Thread.sleep(delay * 1000);
                } catch (InterruptedException ignored) {}

                /*
                 * Sometimes the application's custom LookAndFeel messes up
                 * how JavaSnoop components are displayed because they're not
                 * builto to the types of components we use. So we allow the
                 * JavaSnoop user to override the target application's L&F.
                 */
                if (lookAndFeelOverride != null && !"".equals(lookAndFeelOverride)) {
                   try {
                        for (LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
                            if (lookAndFeelOverride.equals(laf.getName())) {
                                UIManager.setLookAndFeel(laf.getClassName());
                                break;
                            }
                        }
                        
                    } catch (Exception ex) {
                        AgentLogger.error("Problem overriding LookAndFeel",ex);
                    }
                }

                AgentLogger.debug("Running GUI...");
                JavaSnoopView view = new JavaSnoopView(manager);
                mainView = view;
                AgentLogger.debug("GUI created. Running JavaSnoop!");
                view.setVisible(true);
            }
        });
    }

	private static void turnOffSecurity() {
        /*
         * Test if we're inside an applet. We should be inside
         * an applet if the System property ("package.restrict.access.sun")
         * is not null and is set to true.
         */

        boolean restricted = System.getProperty("package.restrict.access.sun") != null;

        /*
         * If we're in an applet, we need to change the System properties so
         * as to avoid class restrictions. We go through the current properties
         * and remove anything related to package restriction.
         */
        if ( restricted ) {

            Properties newProps = new Properties();

            Properties sysProps = System.getProperties();

            for(String prop : sysProps.stringPropertyNames()) {
                if ( prop != null && ! prop.startsWith("package.restrict.") ) {
                    newProps.setProperty(prop,sysProps.getProperty(prop));
                }
            }

            System.setProperties(newProps);
        }

        /*
         * Should be the final nail in (your) coffin.
         */
        System.setSecurityManager(null);
    }

    public static Instrumentation getInstrumentation() {
        return inst;
    }

    public static InstrumentationManager getAgentManager() {
        return manager;
    }

}