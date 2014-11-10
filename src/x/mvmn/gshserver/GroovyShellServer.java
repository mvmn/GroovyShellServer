package x.mvmn.gshserver;

import groovy.lang.Binding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;

public class GroovyShellServer {

	private final ConcurrentHashMap<Integer, ServerSocket> serverSockets = new ConcurrentHashMap<Integer, ServerSocket>();
	private final ConcurrentHashMap<Integer, Set<Socket>> clientSockets = new ConcurrentHashMap<Integer, Set<Socket>>();
	private final ConcurrentHashMap<Object, Object> sharedMap = new ConcurrentHashMap<Object, Object>();

	public void listen(final Integer port) throws Exception {
		new Thread() {
			public void run() {
				try {
					final int portVal = port != null ? port.intValue() : 54321;

					System.out.println("GSH-Server - listening on port " + portVal);

					final ServerSocket serverSocket = new ServerSocket(portVal);
					serverSockets.put(portVal, serverSocket);
					clientSockets.putIfAbsent(portVal, new HashSet<Socket>());
					try {
						while (true) {
							final Socket clientSocket = serverSocket.accept();
							new Thread() {
								public void run() {
									try {
										clientSockets.get(portVal).add(clientSocket);
										System.out.println("GSH-Server - connected at " + portVal);
										final InputStream clientSocketInputStream = clientSocket.getInputStream();
										final OutputStream clientSocketOutputStream = clientSocket.getOutputStream();
										// final PrintStream oldOut = System.out;

										// System.setIn(clientSocketInputStream);
										// System.setOut(new PrintStream(clientSocketOutputStream, true));
										// System.setErr(new PrintStream(clientSocketOutputStream, true));

										final IO gshIo = new IO(clientSocketInputStream, clientSocketOutputStream, clientSocketOutputStream);
										final Binding binding = new Binding();
										binding.setVariable("gshServer", GroovyShellServer.this);
										binding.setVariable("gshClient", GroovyShellServer.this);
										final Groovysh groovySh = new Groovysh(binding, gshIo);

										groovySh.run("\"Welcome to remote Groovy Shell on port " + portVal + ".\"");
										groovySh.run("");
										// System.setOut(oldOut);
										System.out.println("GSH-Server - disconnected at " + portVal);
										clientSockets.get(portVal).remove(clientSocket);
										clientSocket.close();
									} catch (final Exception exception) {
										exception.printStackTrace();
									}
								}
							}.start();
						}
					} catch (SocketException socExc) {
						if (!serverSocket.isClosed()) {
							throw socExc;
						} else {
							System.out.println("GSH-Server - released port " + portVal);
						}
					} finally {
						clientSockets.remove(clientSockets);
					}
				} catch (final Exception serverException) {
					serverException.printStackTrace();
				}
			}
		}.start();
	}

	public void stopListening(int port) {
		try {
			serverSockets.get(port).close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stopListening() {
		final Set<Integer> currentPorts = new HashSet<Integer>(serverSockets.keySet());
		for (final Integer port : currentPorts) {
			stopListening(port);
		}
	}

	public static void main(final String args[]) throws Exception {
		Integer port = null;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}
		new GroovyShellServer().listen(port);
	}

	public ConcurrentHashMap<Integer, ServerSocket> getServerSockets() {
		return serverSockets;
	}

	public ConcurrentHashMap<Integer, Set<Socket>> getClientSockets() {
		return clientSockets;
	}

	public ConcurrentHashMap<Object, Object> getSharedMap() {
		return sharedMap;
	}
}
