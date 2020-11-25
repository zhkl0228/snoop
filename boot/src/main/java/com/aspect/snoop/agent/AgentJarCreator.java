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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.aspect.snoop.util.RandomUtil;

public class AgentJarCreator {

    private static String nl = System.getProperty("line.separator");

    public static final String[] jarsToNotBootClasspath = {
      "appframework-1.0.3.jar",
      "swing-worker-1.1.jar",
      "xom-1.1.jar",
      "rsyntaxtextarea-1.4.1.jar",
      "xstream-1.4.14-jdk7.jar"
    };

    private static Log logger = LogFactory.getLog(AgentJarCreator.class);

    public static AgentJar createAgentJar(boolean attachingOnStartup) throws IOException {

        URL url = ClassLoader.getSystemClassLoader().getResource("");
        
        String file;
        boolean testing;

        if ( url == null ) {
        	File classFile = new File(AgentJarCreator.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            file = classFile.getAbsolutePath();
            testing = classFile.isDirectory();
        } else {
            testing = true;
            file = url.getFile();
        }
        
        if(logger.isDebugEnabled()) {
        	Properties properties = System.getProperties();
            for(Object key : properties.keySet()) {
            	String value = properties.getProperty(String.valueOf(key));
                // logger.debug("createAgentJar key=" + key + ", value=" + value);
            }
        }
        
        logger.debug("createAgentJar url=" + url + ", file=" + file + ", testing=" + testing + ", attachingOnStartup=" + attachingOnStartup);

        if (testing) {
            // this is a test environment, get it from the working
            // directory.
            String buildDirectory = file;
            File f = new File(System.getProperty("java.io.tmpdir"),
                    "JavaSnoop" + RandomUtil.randomString(6) + ".jar");
            file = f.getAbsolutePath();
            
            zip(file, buildDirectory);
            f.deleteOnExit();
        }

        // step #1: create a Manifest that uses the Agent

        StringBuilder sbuf = new StringBuilder();

        sbuf.append("Manifest-Version: 1.0").append(nl);

        /*
         * Doesn't hurt to add both.
         */
        sbuf.append("Premain-Class: ").append(SnoopAgent.class.getName()).append(nl);
        sbuf.append("Agent-Class: ").append(SnoopAgent.class.getName()).append(nl);
        
        sbuf.append("Can-Redefine-Classes: true").append(nl);
        sbuf.append("Can-Retransform-Classes: true").append(nl);

        Collection<String> classpathEntry = new HashSet<String>();
        sbuf.append("Boot-Class-Path: ").append(getJarPaths(testing, "Boot-Class-Path: ".length(), classpathEntry));

        // step #2: unzip the jar we're using right now to modify
        File tmpDir;
        if ( testing ) {
            tmpDir = File.createTempFile("snoop", "");
        } else {
            tmpDir = new File(file, "working");
        }

        if ( tmpDir.exists() ) {
            boolean didDelete = tmpDir.delete();
            if ( ! didDelete ) {
                logger.error("Warning - could not delete working directory!");
            }
        }

        boolean success = tmpDir.mkdir();

        if ( ! success ) {
            logger.error("Could not create dir: " + tmpDir.getAbsolutePath());
        }
        
        unzip(file, tmpDir.getAbsolutePath());

        // step #3: overwrite the manifest
        String metaInfDir = new File(tmpDir, "META-INF").getAbsolutePath();
        String manifestLocation = metaInfDir +  File.separator + "MANIFEST.MF";

        File metaDir = new File(metaInfDir);
        metaDir.mkdirs();

        File newManifestFile = new File(manifestLocation);

        if ( ! newManifestFile.exists() ) {
            newManifestFile.createNewFile();
            newManifestFile.deleteOnExit();
        }

        new FileOutputStream(newManifestFile).write(sbuf.toString().getBytes());

        // step #4: zip it back up
        if ( ! testing ) {
            // have to set it to a new location so
            // as not to overwrite the same jar we
            // ran from.
            file = new File(tmpDir, "JavaSnoop.jar").getAbsolutePath();
        }
        zip(file, tmpDir.getAbsolutePath());

        // step #5: clean up all temp files
        tmpDir.deleteOnExit();

        // step #6: return the location of said zip
        return new AgentJar(file, classpathEntry, tmpDir);
    }

    private static void zip(String zipFileName, String dir) throws IOException  {
        File dirObj = new File(dir);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
        addDir(dirObj.getAbsolutePath(), dirObj, out);
        out.close();
    }

    private static void addDir(String root, File dirObj, ZipOutputStream out) throws IOException {

        File[] files = dirObj.listFiles();
        byte[] tmpBuf = new byte[1024];

        for (int i = 0; files != null && i < files.length; i++) {

            if (files[i].isDirectory()) {
                addDir(root, files[i], out);
                continue;
            }

            if ( root.equals(dirObj.getAbsolutePath()) && files[i].getName().equals("JavaSnoop.jar")) {
                continue;
            }

            FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
            String fileWithoutRootDir = files[i].getAbsolutePath();
            fileWithoutRootDir = fileWithoutRootDir.substring(root.length()+1);
            fileWithoutRootDir = fileWithoutRootDir.replaceAll("\\\\", "/");

            ZipEntry entry = new ZipEntry(fileWithoutRootDir);

            out.putNextEntry(entry);
            int len;
            while ((len = in.read(tmpBuf)) > 0) {
                out.write(tmpBuf, 0, len);
            }
            out.closeEntry();
            in.close();
            
        }
    }

    public static void unzip(String zip, String dir) throws IOException {
    	logger.debug("unzip zip=" + zip + ", dir=" + dir);

        ZipFile zipFile = new ZipFile(zip);
        Enumeration enumeration = zipFile.entries();

        while (enumeration.hasMoreElements()) {

            ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
            BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(zipEntry));
            int size;
            byte[] buffer = new byte[2048];

            File newFile = new File(dir + File.separator + zipEntry.getName());
            
            boolean isDir = zipEntry.getName().endsWith("/");
    
            if ( isDir ) {
                newFile.mkdir();
                continue;
            }

            if ( ! newFile.getParentFile().exists() ) {
                newFile.getParentFile().mkdirs();
            }

            newFile.createNewFile();
            
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(newFile), buffer.length);

            while ((size = bis.read(buffer, 0, buffer.length)) != -1) {
                bos.write(buffer, 0, size);
            }

            bos.flush();
            bos.close();
            bis.close();
        }
    }

    private static String getJarPaths(boolean testing, int prefixLength, Collection<String> classpathEntry) {

        if ( ! testing ) { // TODO classpath entry
            try {

                Class<?> clazz = AgentJarCreator.class;

                String className = clazz.getSimpleName();
                String classFileName = className + ".class";
                String pathToThisClass = clazz.getResource(classFileName).toString();

                int mark = pathToThisClass.indexOf("!") ;
                String pathToManifest = pathToThisClass.toString().substring(0,mark+1) ;
                pathToManifest += "/META-INF/MANIFEST.MF" ;
                Manifest m = new Manifest(new URL(pathToManifest).openStream());

                Attributes attrs = m.getMainAttributes();
                String cp = attrs.getValue("Class-Path");
                cp = cp.replaceAll("lib/","../lib/");

                String[] entries = cp.split("\\s");
                StringBuilder cpBuff = new StringBuilder();
                for(int i=0;i<entries.length;i++) {
                    String entry = entries[i];
                    boolean shouldIgnore = false;
                    for(String ignoreJar : jarsToNotBootClasspath) {
                        if (entry.endsWith(ignoreJar)) {
                            shouldIgnore = true;
                            break;
                        }
                    }
                    
                    if ( ! shouldIgnore ) {
                        cpBuff.append(entry);
                        if ( i != entries.length-1 )
                            cpBuff.append(" ");
                    }
                }

                cp = cpBuff.toString();
                //System.out.println("Afterwards: " + cp);

                return getManifestRepresentation( (72-(nl.length()+prefixLength)), cp );
            } catch (IOException ex) {
                logger.fatal(ex.getMessage(), ex);
            }
        }

        String classpath = System.getProperty("java.class.path");

        String[] entries = classpath.split(File.pathSeparator);

        StringBuilder sb = new StringBuilder();
        for ( String entry : entries ) {
        	if(!entry.endsWith(".jar") ||
        			entry.endsWith("tools.jar")) {
        		continue;
        	}

        	logger.debug("getJarPaths entry=" + entry);
        	
        	/*if(entry.contains("commons-logging") ||
        			entry.contains("javassist")) {
            	sb.append( entry.replaceAll("\\\\", "/") ).append(' ');
        	} else {
        		classpathEntry.add(entry);
        	}*/
        	
        	classpathEntry.add(entry);
        }

        if(sb.length() > 0) {
        	sb.deleteCharAt(sb.length() - 1);
        }

        String cp = sb.toString();

        cp = getManifestRepresentation( (72-(nl.length()+prefixLength)), cp );
        
        return cp;
    }

    public static String getManifestRepresentation(int firstRowLength, String payload) {
        
        StringBuilder sb = new StringBuilder();

        if ( payload.length() <= firstRowLength ) {
            return payload;
        }
        
        sb.append(payload, 0, firstRowLength).append(nl);

        int currentIndex = firstRowLength;
        int whatsLeft = payload.length()-currentIndex;

        while( whatsLeft >= (72-(1 + nl.length())) ) {
            sb.append(" ").append(payload.substring(currentIndex, currentIndex + (72 - (1 + nl.length())))).append(nl);
            whatsLeft -= (72-(1 + nl.length()));
            currentIndex += (72-(1+nl.length()));
        }

        if ( whatsLeft > 0 ) {
            sb.append(" ").append(payload.substring(currentIndex)).append(nl);
        }

        return sb.toString();
    }

}
