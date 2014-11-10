package x.mvmn.gshserver;

import groovy.lang.Binding;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.groovy.tools.shell.Groovysh;
import org.codehaus.groovy.tools.shell.IO;

public class GroovyShellServer {

	private ConcurrentHashMap<Integer, ServerSocket> servers = new ConcurrentHashMap<Integer, ServerSocket>();
	private ConcurrentHashMap<Integer, Set<Socket>> clients = new ConcurrentHashMap<Integer, Set<Socket>>();

	private volatile boolean stopRequested;

	public void listen(Integer port) throws Exception {
		final int portVal = port != null ? port.intValue() : 54321;

		System.out.println("GSH-Server - listening on port " + portVal);

		final ServerSocket server = new ServerSocket(portVal);
		servers.put(portVal, server);
		clients.put(portVal, new HashSet<Socket>());
		while (!stopRequested) {
			final Socket clientSocket = server.accept();
			clients.get(portVal).add(clientSocket);
			System.out.println("GSH-Server - connected at " + portVal);
			final InputStream clientSocketInputStream = clientSocket.getInputStream();
			final OutputStream clientSocketOutputStream = clientSocket.getOutputStream();
			final PrintStream oldOut = System.out;

			final IO gshIo = new IO(clientSocketInputStream, clientSocketOutputStream, clientSocketOutputStream);
			final Binding binding = new Binding();
			binding.setVariable("gshAllServers", servers);
			binding.setVariable("gshAllClients", clients);
			binding.setVariable("gshThisClient", clientSocket);
			final Groovysh groovySh = new Groovysh(binding, gshIo);
			// groovy.lang.Binding binding = (groovy.lang.Binding) groovySh.getProperty("binding");
			// binding.setVariable("gshClientSocket", clientSocket);
			System.setOut(new PrintStream(clientSocketOutputStream, true));
			groovySh.run("");// println \"Welcome to remote Groovy Shell on port " + portVal + "\"");
			System.setOut(oldOut);
			System.out.println("GSH-Server - disconnected at " + portVal);
			clientSocket.close();
		}
	}

	public static void main(final String args[]) throws Exception {
		Integer port = null;
		if (args.length > 0) {
			port = Integer.parseInt(args[0]);
		}
		new GroovyShellServer().listen(port);
	}
}
