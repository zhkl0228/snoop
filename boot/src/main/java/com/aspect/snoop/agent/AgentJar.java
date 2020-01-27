/**
 * 
 */
package com.aspect.snoop.agent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author zhkl0228
 *
 */
public class AgentJar {
	
	private static final Log log = LogFactory.getLog(AgentJar.class);
	
	private final String agentJarPath;
	private final Collection<String> classpathEntry;
	private final File tmpDir;
	public AgentJar(String agentJarPath, Collection<String> classpathEntry, File tmpDir) {
		super();
		this.agentJarPath = agentJarPath;
		this.classpathEntry = classpathEntry;
		this.tmpDir = tmpDir;
	}
	public String getAgentJarPath() {
		return agentJarPath;
	}
	
	public String getClasspath() {
		FileWriter writer = null;
		try {
			File tmp = File.createTempFile("classpath", ".tmp", tmpDir);
			writer = new FileWriter(tmp);
			for(String entry : classpathEntry) {
				writer.write(entry);
				writer.write('\n');
			}
			writer.flush();
			return tmp.getAbsolutePath();
		} catch(IOException e) {
			log.warn(e.getMessage(), e);
			throw new IllegalStateException(e);
		} finally {
			if(writer != null) {
				try { writer.close(); } catch(IOException e) {}
			}
		}
	}

}
