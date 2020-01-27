package com.aspect.snoop.util;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;

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

import com.aspect.snoop.SnoopSession;
import com.aspect.snoop.agent.AgentCommunicationException;
import com.aspect.snoop.agent.AgentJar;
import com.aspect.snoop.agent.AgentJarCreator;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import cn.banny.utils.StringUtils;

/**
 * Utility methods for launching JVMs and using the Attach API.
 * 
 * @author David Anderson
 */
public class AttachUtil {

    private static Logger logger = Logger.getLogger(AttachUtil.class);

    public static void attachToVM() throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException, AgentCommunicationException {
        // Use the process id of this VM
    	AgentJar agentJar = AgentJarCreator.createAgentJar(false);
        loadAgentInOtherVM(agentJar, ManagementFactory.getRuntimeMXBean().getName().split("@")[0], null);
    }

    public static void loadAgentInOtherVM(AgentJar agentJar, String pid, String mainClass) throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException, AgentCommunicationException {

        VirtualMachine vm = VirtualMachine.attach(pid);

        /*
         * Agent is expecting arguments in the form of:
         * <javasnoop install dir>|number|[LookAndFeelClass]
         * Where number represents the number of seconds to wait before
         * loading the JavaSnoop GUI. Attaching to an existing process
         * requires no waiting, so we hardcode to 0.
         */
        String libraryPath = System.getProperty("java.library.path");
        String agentArgs = agentJar.getClasspath() + "|0|" + SnoopSession.findLookAndFeel() + "|false|" +
        		(StringUtils.isEmpty(mainClass) ? "" : mainClass) + '|' + pid + '|' + (StringUtils.isEmpty(libraryPath) ? "" : libraryPath);
        logger.debug("loadAgentInOtherVM agentArgs=" + agentArgs + ", libraryPath=" + libraryPath);
        vm.loadAgent(agentJar.getAgentJarPath(), agentArgs);
        vm.detach();
    }

    public static void launchInNewVM(AgentJar agentJar, SnoopSession session) throws AttachNotSupportedException, ClassNotFoundException, NoSuchMethodException, IOException, AttachNotSupportedException,AgentCommunicationException {

        boolean isJar = session.getMainClass().trim().length() == 0 &&
                        session.getClasspathString().trim().length() > 0 &&
                        session.getClasspathString().trim().endsWith(".jar");

        String javaHome = System.getProperty("java.home") + File.separator + "bin";
        List<String> arguments = new ArrayList<>();

        String command = javaHome + File.separator + "java";

        arguments.add(command);

        String cp = session.getClasspathString();

        /*
         * Agent is expecting arguments in the form of:
         * <javasnoop install dir>|number
         * Where number represents the number of seconds to wait before
         * loading the JavaSnoop GUI. Starting up a new process requires
         * to wait for the application to initialize its own GUI. The user
         * specified this value in the NewProcessInfoView form.
         */
        String libraryPath = System.getProperty("java.library.path");
        String mainClass = isJar ? cp : session.getMainClass();
        String agent =
                "-javaagent:" + agentJar.getAgentJarPath() +
                "=" + agentJar.getClasspath() + 
                "|" + session.getGuiDelay()  +
                "|" + session.getLookAndFeel() +
                "|false|" + mainClass +
                "|" + (ThreadLocalRandom.current().nextInt(1000) + 100) + "|" + (StringUtils.isEmpty(libraryPath) ? "" : libraryPath);
        
        arguments.add(agent);

        String javaArgs = session.getJavaArguments().trim();
        
        if ( javaArgs.length() > 0) {
            String[] args = parseArguments(javaArgs);
            arguments.addAll(Arrays.asList(args));
        }

        if ( cp.trim().length() >0) {
            if ( isJar ) {
                arguments.add("-jar");
            } else {
                arguments.add("-cp");
            }
            arguments.add(cp);
        }

        if ( ! isJar ) {
            arguments.add(session.getMainClass());
        }
        
        if (session.getArguments().length()>0) {
            String[] args = parseArguments(session.getArguments());
            arguments.addAll(Arrays.asList(args));
        }

        String[] commandArgs = arguments.toArray( new String[]{} );

        StringBuilder sb = new StringBuilder();
        for(String s : commandArgs) {
            sb.append(s).append(" ");
        }
        
        String workingDir = new File(".").getPath();

        if ( session.getWorkingDir().trim().length() > 0 ) {
            workingDir = session.getWorkingDir().trim();
        }

        sb = new StringBuilder();
        for(String arg : commandArgs) {
            sb.append(arg);
            sb.append(" ");
        }
        logger.debug(sb.toString());
        
        final String fWorkingDir = workingDir;
        final String[] fCommandArgs = commandArgs;

        new Thread("Executing ") {
            @Override
            public void run() {
                try {
                    Process p = Runtime.getRuntime().exec(fCommandArgs, null, new File(fWorkingDir));
                    JadUtil.doWaitFor(p);
                } catch (IOException ex) { 
                    logger.error(ex);
                }
            }
        }.start();
    }
    
    private static String[] parseArguments(String args) {

        List<String> arguments = new ArrayList<>();

        boolean quoted = false;
        String currentArg = "";

        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (!quoted && c == ' ') {
                arguments.add(currentArg);
                currentArg = "";
            } else if (quoted && c == '"') {
                arguments.add(currentArg);
                currentArg = "";
                quoted = false;
                i++; // skip over the space
            } else if (c == '"') {
                quoted = true;
            } else {
                currentArg += c;
            }
            if ( i == args.length()-1 ) {
                arguments.add(currentArg);
            }
        }

        String[] toReturn = new String[arguments.size()];

        for (int i = 0; i < arguments.size(); i++) {
            toReturn[i] = arguments.get(i);
        }

        return toReturn;
    }
}
