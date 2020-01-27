package com.fuzhu8.inspector.advisor;

import cn.banny.utils.StringUtils;
import com.alibaba.dcm.DnsCache;
import com.alibaba.dcm.DnsCacheEntry;
import com.alibaba.dcm.DnsCacheManipulator;
import com.fuzhu8.inspector.ClientConnectListener;
import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.bytecode.DexFileManager;
import com.fuzhu8.inspector.completer.DefaultServerCommandCompleter;
import com.fuzhu8.inspector.completer.ServerCommandCompleter;
import com.fuzhu8.inspector.http.HttpUtils;
import com.fuzhu8.inspector.io.Console;
import com.fuzhu8.inspector.io.*;
import com.fuzhu8.inspector.plugin.Appender;
import com.fuzhu8.inspector.script.LuaScriptManager;
import com.strobel.assembler.metadata.*;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.BraceStyle;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;
import de.robv.android.xposed.XposedBridge;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author zhkl0228
 *
 */
public abstract class AbstractInspector extends AbstractAdvisor implements
		Inspector, ClientConnectListener, Appender {
	
	protected final DexFileManager dexFileManager;
	protected final LuaScriptManager luaScriptManager;
	private final ServerCommandCompleter global;
	private final ServerCommandCompleter inspector;
	
	private final String mainClass;
	private final int pid;

	public AbstractInspector(ModuleContext context, DexFileManager dexFileManager,
			LuaScriptManager luaScriptManager, Hooker<?> hooker, String mainClass, int pid) {
		super(context, hooker);
		
		this.dexFileManager = dexFileManager;
		this.luaScriptManager = luaScriptManager;
		this.mainClass = mainClass;
		this.pid = pid;
		
		this.initializeLogServer();
		
		executeMyHook();
		
		global = this.createCommandCompleter(null);
		global.addCommandHelp("log", "log(msg); -- log to console");
		global.addCommandHelp("where", "where(msg?); -- print the stack trace to console");
		global.addCommandHelp("hook", "hook(class, method, ..., callback?); -- hook api, method = '*' means all method.",
				"hook(\"java.lang.String\", \"equalsIgnoreCase\", \"java.lang.String\", function(old, this, anotherString)",
				"    local ret = old:invoke(this, anotherString);",
				"    log(\"equalsIgnoreCase this=\" .. this .. \", anotherString=\" .. anotherString .. \", ret=\" .. tostring(ret));",
				"    return ret;",
				"end);");
		
		inspector = this.createCommandCompleter("inspector:");
		inspector.addCommandHelp("inspector:println", "inspector:println(msg); -- print msg");
		inspector.addCommandHelp("inspector:decompile", "inspector:decompile(className); -- decompile java bytecode to source.");
		inspector.addCommandHelp("inspector:dumpClass", "inspector:dumpClass(filter?); -- dump loaded class");
		inspector.addCommandHelp("inspector:hookClass", "inspector:hookClass(classFilter); -- hook classes");
		
		inspector.addCommandHelp("inspector:dumpField", "inspector:dumpField(clazz); -- dump fields");
		inspector.addCommandHelp("inspector:dumpMethod", "inspector:dumpMethod(clazz); -- dump methods");
		inspector.addCommandHelp("inspector:dumpClassCode", "inspector:dumpClassCode(clazz); -- dump class method code");
		inspector.addCommandHelp("inspector:dump", "inspector:dump(startAddr, length?endAddr); -- dump memory");
		inspector.addCommandHelp("inspector:mem", "inspector:mem(startAddr, length?endAddr); -- inspect memory");
		inspector.addCommandHelp("inspector:info()", "inspector:info(); -- print phone information");
		inspector.addCommandHelp("inspector:kill()", "inspector:kill(); -- kill the process");
		inspector.addCommandHelp("inspector:setDebug()", "inspector:setDebug(); -- set debug");

		inspector.addCommandHelp("inspector:clearDnsCache()", "inspector:clearDnsCache(); -- clear dns cache");
		inspector.addCommandHelp("inspector:listAllDnsCache()", "inspector:listAllDnsCache(); -- list all dns cache");

		inspector.addCommandHelp("inspector:listAllDevs()", "inspector:listAllDevs(); -- list all iface");

		inspector.addCommandHelp("inspector:lynx", "inspector:lynx(url); -- print url content.");
		inspector.addCommandHelp("inspector:interrupt()", "inspector:interrupt() -- interrupt current worker thread.");
		inspector.addCommandHelp("inspector:threads()", "inspector:threads() -- list all threads.");
		inspector.addCommandHelp("inspector:thread", "inspector:thread(threadId) -- list the thread stack trace.");
	}

	@SuppressWarnings("unused")
	public void decompile(final String className) {
		try {
			long start = System.currentTimeMillis();
			DecompilerSettings settings = new DecompilerSettings();
			settings.setForceExplicitImports(true);
			JavaFormattingOptions formattingOptions = JavaFormattingOptions.createDefault();
			formattingOptions.ClassBraceStyle = BraceStyle.EndOfLine;
			formattingOptions.InterfaceBraceStyle = BraceStyle.EndOfLine;
			formattingOptions.EnumBraceStyle = BraceStyle.EndOfLine;
			formattingOptions.PlaceCatchOnNewLine = false;
			formattingOptions.PlaceElseIfOnNewLine = false;
			formattingOptions.PlaceElseOnNewLine = false;
			formattingOptions.PlaceFinallyOnNewLine = false;
			formattingOptions.PlaceWhileOnNewLine = false;
			settings.setJavaFormattingOptions(formattingOptions);
			List<ITypeLoader> typeLoaders = new ArrayList<>();
			for(Class<?> clazz : dexFileManager.getLoadedClasses()) {
				if(clazz.getName().startsWith(className)) {
					byte[] bytes = dexFileManager.getClassBytes(clazz.getName());
					if(isDebug()) {
						inspect(bytes, clazz.getName());
					}
					typeLoaders.add(new ArrayTypeLoader(bytes));
				}
			}
			if(typeLoaders.isEmpty()) {
				throw new Exception("find " + className + " failed.");
			}
			ITypeLoader typeLoader = new CompositeTypeLoader(typeLoaders.toArray(new ITypeLoader[0]));
			MetadataSystem metadataSystem = new MetadataSystem(typeLoader);
			DecompilationOptions options = new DecompilationOptions();
			options.setSettings(settings);
			options.setFullDecompilation(true);
			
			String internalName = className.replace('.', '/');
			TypeReference type = metadataSystem.lookupType(internalName);
			TypeDefinition resolvedType;
			if (type == null || ((resolvedType = type.resolve()) == null)) {
				throw new Exception("Unable to resolve type: " + internalName);
			}
			Writer writer = new StringWriter();
			settings.getLanguage().decompileType(resolvedType,
					new PlainTextOutput(writer),
					options);
			writer.flush();
			
			String java = writer.toString();
			println("\n" + java);
			
			println("decompile " + className + " successfully, use time " + (System.currentTimeMillis() - start) + "ms");
			writeToConsole(new StringCache("procyon/" + internalName + ".java", java));
		} catch(Exception e) {
			println(e);
		}
	}

	@SuppressWarnings("unused")
	public void dumpClass() {
		dumpClass(null);
	}
	
	public void dumpClass(String filter) {
		boolean added = false;
		for(Class<?> clazz : dexFileManager.getLoadedClasses()) {
			String name = clazz.getCanonicalName();
			if(StringUtils.isEmpty(name)) {
				continue;
			}
			
			if(filter == null || name.contains(filter)) {
				println(name);
				added = true;
			}
		}
		if(!added) {
			println("dump class finished! ");
		}
	}
	
	public void thread(int threadId) {
		Map<Thread, StackTraceElement[]> threadMap = Thread.getAllStackTraces();
		for(Map.Entry<Thread, StackTraceElement[]> entry : threadMap.entrySet()) {
			Thread thread = entry.getKey();
			if(thread.getId() != threadId) {
				continue;
			}
			
			println(thread.getName());
			println(thread.getState());
			for(StackTraceElement element : entry.getValue()) {
				println(element);
			}
			break;
		}
	}

	@SuppressWarnings("unused")
	public void threads() {
		ServerCommandCompleter completer = createCommandCompleter("inspector:thread");
		Map<Thread, StackTraceElement[]> threadMap = Thread.getAllStackTraces();
		StringBuffer buffer = new StringBuffer();
		for(Thread thread : threadMap.keySet()) {
			buffer.setLength(0);
			if(thread.getState() == State.BLOCKED) {
				buffer.append('*');
			}
			buffer.append(thread.getId()).append(',').append(thread.getName()).append(',').append(thread.getState());
			println(buffer);
			completer.addCommandHelp("inspector:thread(" + thread.getId() + "); -- " + thread.getName(), "inspector:thread(threadId) -- list stack trace for thread: " + thread.getName());
		}
		completer.commit();
		
	}

	@SuppressWarnings("unused")
	public void lynx(String url) throws IOException {
		byte[] data = HttpUtils.sendGet(url);
		inspect(data, url);
	}

	@SuppressWarnings("unused")
	public void clearDnsCache() {
		DnsCacheManipulator.clearDnsCache();
	}

	@SuppressWarnings("unused")
	public void listAllDnsCache() {
		DnsCache cache = DnsCacheManipulator.getWholeDnsCache();
		for(DnsCacheEntry entry : cache.getCache()) {
			println(entry.getHost() + " -> " + Arrays.asList(entry.getIps()));
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while(!canStop) {
			try {
				Object obj;
				if(serverSocket == null) {
					break;
				}
				
				Socket socket = serverSocket.accept();
				if(socket == null) {
					break;
				}
				
				socket.setKeepAlive(true);
				// socket.setSoTimeout(0);
				socket.setKeepAlive(true);
				Console console = new SocketConsole();
				this.console = console;
				obj = socket;
				
				console.open(obj);
				onConnected(console);
				
				println("Connect to console[" + console.getClass().getSimpleName() + "] successfully! ");
				InspectCache cache;
				while((cache = cacheQueue.poll()) != null) {
					try {
						cache.writeTo(console);
					} catch(IOException ignored) {}
				}
				
				Command command;
				StringBuffer lua = new StringBuffer();
				while(this.console != null && (command = this.console.readCommand()) != null) {
					// log("Received command: " + command);
					
					if(command.isHelp()) {
						printHelp();
						continue;
					}
					
					command.execute(lua, this);
				}
				
			} catch(SocketTimeoutException e) {
				// ignore
			} catch (IOException e) {
				super.log(e);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ignored) {
				}
			} catch(Exception t) {
				log(t);
			} finally {
				closeConsole();
				
				sendBroadcast();
			}
		}
	}
	
	@Override
	public void evalLuaScript(String script) {
		try {
			if(script == null || script.trim().length() < 1) {
				return;
			}
			
			println('\n' + script);
			luaScriptManager.eval(script);
			println("eval lua script successfully!");
		} catch (Throwable e) {
			println("evalLuaScript lua=\n" + script);
			println(e);
		}
	}

	private void printHelp() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(global.describeHelp());
		buffer.append(inspector.describeHelp());
		println(buffer);
	}
	
	public void kill() {
		System.exit(-1);
	}

	@SuppressWarnings("unused")
	private void out(String obj) {
		String msg = null == obj ? "null" : obj;
		// super.log(msg);
		writeToConsole(new LargeMessageCache(msg, true));
	}

	@SuppressWarnings("unused")
	private void err(String obj) {
		String msg = null == obj ? "null" : obj;
		// super.log(msg);
		writeToConsole(new LargeMessageCache(msg, false));
	}
	
	private final Queue<InspectCache> cacheQueue = new LinkedBlockingQueue<>();
	
	@Override
	public final void writeToConsole(InspectCache cache) {
		if(console == null) {
			cacheQueue.offer(cache);
			while(cacheQueue.size() > 512) {
				cacheQueue.poll();
			}
			return;
		}
		
		try {
			cache.writeTo(console);
		} catch(IOException ioe) {
			ioe.printStackTrace();
			closeConsole();
		}
	}
	
	@Override
	public final void onConnected(Console console) {
		long currentTimeMillis = System.currentTimeMillis();
		global.commit();
		println("initialized global: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();
		
		inspector.commit();
		println("initialized inspector: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();

		ServerCommandCompleter completer = createCommandCompleter("inspector:decompile(");
		for(Class<?> clazz : dexFileManager.getLoadedClasses()) {
			if(clazz.isAnnotation() || clazz.isAnonymousClass() || clazz.isArray() || clazz.isEnum() || clazz.isMemberClass() || clazz.isSynthetic()) {
				continue;
			}
			String str = clazz.getCanonicalName();
			completer.addCommandHelp("inspector:decompile(\"" + str + "\")", "inspector:decompile(className); -- decompile java bytecode to source.");
		}
		completer.commit();
		println("initialized decompile: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");

		completer = createCommandCompleter("inspector:dumpMethod(");
		for(Class<?> clazz : dexFileManager.getLoadedClasses()) {
			if(clazz.isAnnotation() || clazz.isAnonymousClass() || clazz.isArray() || clazz.isEnum() || clazz.isMemberClass() || clazz.isSynthetic()) {
				continue;
			}
			String str = clazz.getCanonicalName();
			completer.addCommandHelp("inspector:dumpMethod(\"" + str + "\")", "inspector:dumpMethod(clazz); -- dump methods");
		}
		completer.commit();
		println("initialized dumpMethod: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();

		completer = createCommandCompleter("inspector:dumpField(");
		for(Class<?> clazz : dexFileManager.getLoadedClasses()) {
			if(clazz.isAnnotation() || clazz.isAnonymousClass() || clazz.isArray() || clazz.isEnum() || clazz.isMemberClass() || clazz.isSynthetic()) {
				continue;
			}
			String str = clazz.getCanonicalName();
			completer.addCommandHelp("inspector:dumpField(\"" + str + "\")", "inspector:dumpField(clazz); -- dump fields");
		}
		completer.commit();
		println("initialized dumpField: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();

		completer = createCommandCompleter(null);
		for(Class<?> clazz : dexFileManager.getLoadedClasses()) {
			if(clazz.isAnnotation() || clazz.isAnonymousClass() || clazz.isArray() || clazz.isEnum() || clazz.isMemberClass() || clazz.isSynthetic()) {
				continue;
			}
			String str = clazz.getCanonicalName();
			completer.addCommandHelp("hook(\"" + str + "\", ", "hook(class, method, ..., callback?); -- hook api, method = nil means constructor, method = '*' means all constructor and method.");
		}
		completer.commit();
		println("initialized hook: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
	}

	@Override
	public final void onClosed(Console console) {
	}

	private void closeConsole() {
		if(console != null) {
			onClosed(console);
			
			console.close();
			console = null;
		}
	}

	@SuppressWarnings("unused")
	public void hookClass(String classFilter) {
		try {
			String[] hooked = dexFileManager.requestHookClasses(classFilter);
			
			boolean added = false;
			for(String clazz : hooked) {
				println("hooked: " + clazz);
				added = true;
			}
			if(!added) {
				println("hook classes finished! ");
			}
		} catch(Throwable t) {
			println(t);
		}
	}
	
	private Thread thread;

	@SuppressWarnings("unused")
	public void interrupt() throws InterruptedException {
		if(thread != null) {
			thread.interrupt();
			thread.join();
			thread = null;
		}
	}

	public void info() {
		final String newLine = System.getProperty("line.separator");

		StringBuilder sb = new StringBuilder();
		sb.append("Process ID: ");
		sb.append(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		sb.append(newLine);
		sb.append("Working directory: ");
		sb.append(new File(".").getAbsolutePath());
		sb.append(newLine);
		sb.append("Classes loaded: ");
		sb.append(dexFileManager.getLoadedClasses().size());
		sb.append(newLine);
		sb.append("System properties: ");
		sb.append(newLine);
		Properties p = System.getProperties();
		Set<?> keySet = p.keySet();
		for(Object obj : keySet) {
			String key = (String) obj;
			if ( "line.separator".equals(key) )
				continue;
			sb.append("   ");
			sb.append(key);
			sb.append("=");
			sb.append(p.getProperty(key));
			sb.append(newLine);
		}
		println(sb);

		Runtime runtime = Runtime.getRuntime();
		println("totalMemory: " + toMB(runtime.totalMemory()));
		println("freeMemory: " + toMB(runtime.freeMemory()));
		println("maxMemory: " + toMB(runtime.maxMemory()));
		println("availableProcessors: " + runtime.availableProcessors());
	}

	private String toMB(long memory) {
		return (memory * 100 / (1024 * 1024)) / 100F + "MB";
	}

	@SuppressWarnings("unused")
	public void dumpField(String clazz) {
		for(Class<?> cls : dexFileManager.getLoadedClasses()) {
			if(!clazz.equals(cls.getCanonicalName())) {
				continue;
			}
			
			Field[] fields = cls.getDeclaredFields();
			for(Field field : fields) {
				if(Modifier.isStatic(field.getModifiers())) {
					boolean isAccessible = field.isAccessible();
					field.setAccessible(true);
					Object val;
					try {
						val = field.get(null);
					} catch(Throwable e) {
						val = e;
					}
					field.setAccessible(isAccessible);
					println(field + "=" + val);
					continue;
				}
				
				println(field);
			}
			println("\n");
			break;
		}
	}

	@SuppressWarnings("unused")
	public void dumpMethod(String clazz) {
		for(Class<?> cls : dexFileManager.getLoadedClasses()) {
			if(!clazz.equals(cls.getCanonicalName())) {
				continue;
			}
			Method[] methods = cls.getDeclaredMethods();
			for(Method method : methods) {
				println(method);
			}
			println("\n");
			break;
		}
	}

	@SuppressWarnings("unused")
	public void dump(int startAddr, int lengthOrEndAddr) {
		int length = lengthOrEndAddr;
		if(lengthOrEndAddr >= startAddr) {
			length = lengthOrEndAddr - startAddr;
		}
		
		if(length < 1) {
			println("length must big than zero");
			return;
		}
		
		ByteBuffer memory = this.dexFileManager.dumpMemory(startAddr, length);
		writeToConsole(new ByteBufferCache("dump_" + Integer.toHexString(startAddr).toUpperCase() + '-' + Integer.toHexString(startAddr + length).toUpperCase() + ".dat", memory));
	}

	@SuppressWarnings("unused")
	public void mem(int startAddr, int lengthOrEndAddr) {
		int length = lengthOrEndAddr;
		if(lengthOrEndAddr >= startAddr) {
			length = lengthOrEndAddr - startAddr;
		}
		
		if(length < 1) {
			println("length must big than zero");
			return;
		}
		
		ByteBuffer memory = this.dexFileManager.dumpMemory(startAddr, length);
		inspect(memory, "memory 0x" + Integer.toHexString(startAddr).toUpperCase() + "-0x" + Integer.toHexString(startAddr + length).toUpperCase());
	}

	@Override
	public void out_print(Object msg) {
		writeToConsole(new LargeMessageCache(String.valueOf(msg), true));
	}

	@Override
	public void err_print(Object msg) {
		writeToConsole(new LargeMessageCache(String.valueOf(msg), false));
	}

	@Override
	public void printStackTrace(Throwable throwable) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		throwable.printStackTrace(printWriter);
		err_println(writer.toString());
	}

	@Override
	public void out_println(Object msg) {
		writeToConsole(new LargeMessageCache(String.valueOf(msg) + '\n', true));
	}

	@Override
	public void err_println(Object msg) {
		writeToConsole(new LargeMessageCache(String.valueOf(msg) + '\n', false));
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.Inspector#print(java.lang.Object)
	 */
	@Override
	public void println(Object msg) {
		if (!(msg instanceof Throwable)) {
			out_println(String.valueOf(msg));
			return;
		}

		printStackTrace((Throwable) msg);
	}

	@Override
	protected void executeHook() {
	}

	private void executeMyHook() {
		try {
			hook(Thread.class, "suspend");
		} catch (NoSuchMethodException e) {
			log(e);
		}
	}

	@SuppressWarnings("unused")
	void suspend(Thread thread) {
		StringBuilder buffer = new StringBuilder();
		buffer.append('"').append(thread.getName()).append('"');
		if(thread.isDaemon()) {
			buffer.append(" daemon");
		}
		buffer.append(" prio=").append(thread.getPriority());
		buffer.append(" tid=").append(thread.getId());
		buffer.append(" SUSPENDED");
		println(buffer.toString());
	}

	@SuppressWarnings("unused")
	public void listAllDevs() {
		try {
			Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
			List<NetworkInterface> list = new ArrayList<>();
			while(enumeration.hasMoreElements()) {
				NetworkInterface interface1 = enumeration.nextElement();
				if(!interface1.isUp() ||
						interface1.isVirtual() ||
						interface1.getInterfaceAddresses().isEmpty()) {
					continue;
				}
				list.add(interface1);
			}
			if(list.isEmpty()) {
				println("listAllDevs devs is empty.");
				return;
			}
			
			for(NetworkInterface networkInterface : list) {
				println(networkInterface.getName() + ": " + networkInterface.getInterfaceAddresses());
			}
		} catch(Throwable t) {
			log(t);
		}
	}

	private static final int WPE = 16;

	/**
	 * 侦察发送的数据
	 */
	public void inspect(byte[] data, String label) {
		inspect(label, data == null ? null : ByteBuffer.wrap(data), WPE);
	}

	@Override
	public void inspect(ByteBuffer data, String label) {
		inspect(label, data, WPE);
	}

	/**
	 * 侦察发送的数据
	 */
	public void inspect(byte[] data, boolean send) {
		inspect(send ? "发送数据" : "接收数据", data == null ? null : ByteBuffer.wrap(data), WPE);
	}

	/**
	 * 侦察发送的数据
	 */
	public void inspect(int type, byte[] data, boolean send) {
		String ts = Integer.toHexString(type).toUpperCase();
		inspect(send ? "发送数据：0x" + ts : "接收数据：0x" + ts, data == null ? null : ByteBuffer.wrap(data), WPE);
	}
	
	private void inspect(String label, ByteBuffer data, int mode) {
		inspect(null, label, data, mode);
	}
	
	private void inspect(Date date, String label, ByteBuffer buffer, int mode) {
		writeToConsole(new ByteBufferInspectCache(date, label, buffer, mode));
	}
	
	public void inspect(short[] data, String label) {
		writeToConsole(new ShortBufferInspectCache(null, label, data == null ? null : ShortBuffer.wrap(data), 16));
	}

	private boolean canStop;

	@SuppressWarnings("unused")
	private void stop() {
		canStop = true;
	}

	private final byte[] buffer = new byte[256];
	
	private final static int UDP_PORT = 20000;
	
	private long lastSendBroadcast;
	
	private Console console;

	private synchronized void sendBroadcast() {
		long current = System.currentTimeMillis();
		if(console != null &&
				current - lastSendBroadcast < 60000) {//如果已经有客户端了，则等60秒发一次广播
			return;
		}
		
		DatagramSocket datagramSocket = null;
		ByteArrayOutputStream baos = null;
		DataOutputStream dos = null;
		try {
			baos = new ByteArrayOutputStream();
			dos = new DataOutputStream(baos);
			datagramSocket = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			
			OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
			dos.writeShort(serverSocketPort);
			dos.writeUTF(operatingSystemMXBean.getName() + '/' + operatingSystemMXBean.getArch());
			dos.writeByte(0);
			dos.writeInt(pid);
			dos.writeUTF(StringUtils.isEmpty(mainClass) ? Integer.toString(pid) : mainClass);
			
			packet.setData(baos.toByteArray());
			packet.setLength(baos.size());
			packet.setPort(UDP_PORT);
			
			InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");
			packet.setAddress(broadcastAddr);
			datagramSocket.send(packet);
			lastSendBroadcast = current;
		} catch (Exception e) {
			super.log(e);
		} finally {
			IOUtils.closeQuietly(dos);
			IOUtils.closeQuietly(baos);
			if(datagramSocket != null) {
				datagramSocket.close();
			}
		}
	}
	
	private ServerSocket serverSocket;
	private int serverSocketPort;
	
	@Override
	public int getListenerPort() {
		return serverSocketPort;
	}

	private void initializeLogServer() {
		try {
			serverSocket = new ServerSocket();
			serverSocket.setSoTimeout(5000);
			serverSocket.setReuseAddress(true);
			Random random = new Random();
			int times = 0;
			while(times++ < 10) {
				try {
					serverSocketPort = 20000 + random.nextInt(5000);
					serverSocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), serverSocketPort));
					break;
				} catch(BindException e) {
					Thread.sleep(1000);
				}
			}

			println("Begin accept on port " + serverSocketPort);
		} catch (Exception e) {
			println("initializeLogServer failed.");
			log(e);
		}
	}

	@Override
	public void log(Object msg) {
		// super.log(msg);
		
		println(msg);
	}
	
	private boolean debug;

	@SuppressWarnings("unused")
	public void setDebug() {
		setDebug(true);
	}

	@Override
	public void setDebug(boolean debug) {
		this.debug = debug;
		if(debug) {
			XposedBridge.setDebug();
		}
	}

	@Override
	public boolean isDebug() {
		return debug;
	}

	@Override
	public ServerCommandCompleter createCommandCompleter(String prefix) {
		return new DefaultServerCommandCompleter(this, prefix);
	}

}
