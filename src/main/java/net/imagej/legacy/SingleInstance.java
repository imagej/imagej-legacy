/*
 * #%L
 * ImageJ2 software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2024 ImageJ2 developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.imagej.legacy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.scijava.log.LogService;

/**
 * This class tries to contact another instance on the same machine, started
 * by the current user. If such an instance is found, the arguments are
 * sent to that instance. If no such an instance is found, listen for clients.
 * 
 * No need for extra security, as the stub (and its serialization) contain
 * a hard-to-guess hash code.
 *
 *@author Johannes Schindelin
 *@author Mark Hiner
 */
public class SingleInstance {

	private final int port;
	private final LogService log;
	private final IJ1Helper helper;
	private final boolean isWindows;

	public SingleInstance(final int port, final LogService log, final IJ1Helper helper) {
		this.port = port;
		this.log = log;
		this.helper = helper;
		final String osName = System.getProperty("os.name");
		isWindows = osName != null && osName.toLowerCase().indexOf("win") >= 0;
	}

	private interface ImageJInstance extends Remote {
		void sendArgument(String arg) throws RemoteException;
	}

	private class Implementation implements ImageJInstance {
		@Override
		public void sendArgument(String cmd) {
			log.debug("SocketServer.sendArgument: \""+ cmd+"\"");
			if (cmd.startsWith("open "))
				IJ1Helper.openAndAddToRecent(new File(cmd.substring(5)));
			else if (cmd.startsWith("macro ")) {
				String name = cmd.substring(6);
				String name2 = name;
				String arg = null;
				if (name2.endsWith(")")) {
					int index = name2.lastIndexOf("(");
					if (index>0) {
						name = name2.substring(0, index);
						arg = name2.substring(index+1, name2.length()-1);
					}
				}
				helper.runMacroFile(name, arg);
			} else if (cmd.startsWith("run "))
				helper.run(cmd.substring(4));
			else if (cmd.startsWith("eval ")) {
				String rtn = helper.runMacro(cmd.substring(5));
				if (rtn!=null)
					System.out.print(rtn);
			} else if (cmd.startsWith("user.dir "))
				helper.setDefaultDirectory(new File(cmd.substring(9)));
		}
	}

	public String getStubPath() {
		String display = System.getenv("DISPLAY");
		if (display != null) {
			// avoid problems with non-existing directories
			display = display.replace(':', '_');
			display = display.replace('/', '_');
			// The display value after a "." refers to monitor index. If ImageJ is active on any
			// monitor we want to trigger the single instance listener.
			if (display.contains(".")) display = display.substring(0, display.indexOf('.'));
		}
		String tmpDir = System.getProperty("java.io.tmpdir");
		if (!tmpDir.endsWith(File.separator))
			tmpDir = tmpDir + File.separator;

		return tmpDir + "ImageJ-"
			+ System.getProperty("user.name") + "-"
			+ (display == null ? "" : display + "-")
			+ port + ".stub";
	}

	public void makeFilePrivate(String path) {
		try {
			File file = new File(path);
			file.deleteOnExit();

			file.setReadable(true, true);
			file.setWritable(false);
			return;
		} catch (Exception e) {
			log.error("Java < 6 detected," + " trying chmod 0600 " + path, e);
		}

		if (!isWindows) {
			try {
				String[] command = {
					"chmod", "0600", path
				};
				Runtime.getRuntime().exec(command);
			} catch (Exception e) {
				log.error("Even chmod failed.", e);
			}
		}
	}

	/**
	 * @return true iff this method connected to an existing server. If false,
	 *         creates a new server instance.
	 */
	public boolean sendArguments(String[] args) {
		boolean sent = false;
		final File file = new File(getStubPath());

		// If there is no stub, then the server hasn't
		// started yet. So start it up and return false.
		if (file.exists()) {
			try (FileInputStream in = new FileInputStream(file);
					ObjectInputStream objIn = new ObjectInputStream(in)) {
				// Try recovering the remote instance
				ImageJInstance instance = (ImageJInstance)objIn.readObject();

				if (instance != null) {
					sendArguments(args, instance);

					// Instance was non-null and arguments sent
					sent = true;
				}
			} catch (Exception e) {
				log.error(e);
				// If any problems, we need a new server instance
			}
		}

		if (!sent) {
			if (file.exists()) {
				log.error(
						"Could not connect to existing ImageJ instance. If problem persists, please delete file: " + file.getAbsolutePath());
				file.deleteOnExit();
			}
			else {
				// Only start a new server if we aren't going to clash with an existing file.
				startServer();
				log.debug("sendArguments: return false ");
			}

			return false;
		}

		log.debug("sendArguments: return true ");
		return true;
	}

	/**
	 * Sends arguments to specified ImageJ instance
	 */
	private void sendArguments(String[] args, ImageJInstance instance) throws RemoteException {
		log.debug("sendArguments: " + instance);
		instance.sendArgument("user.dir " + System.getProperty("user.dir"));
		int macros = 0;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg == null)
				continue;
			String cmd = null;
			if (macros == 0 && arg.endsWith(".ijm")) {
				cmd = "macro " + arg;
				macros++;
			} else if (arg.startsWith("-macro") && i + 1 < args.length) {
				String macroArg = i + 2 < args.length ? "(" + args[i + 2] + ")" : "";
				cmd = "macro " + args[i + 1] + macroArg;
				instance.sendArgument(cmd);
				break;
			} else if (arg.startsWith("-eval") && i + 1 < args.length) {
				cmd = "eval " + args[i + 1];
				args[i + 1] = null;
			} else if (arg.startsWith("-run") && i + 1 < args.length) {
				cmd = "run " + args[i + 1];
				args[i + 1] = null;
			} else if (arg.indexOf("ij.ImageJ") == -1 && !arg.startsWith("-"))
				cmd = "open " + arg;
			if (cmd != null)
				instance.sendArgument(cmd);
		}
	}

	private static ImageJInstance stub;
	private static Implementation implementation;

	private void startServer() {
		// TODO: not thread safe
		log.debug("SingleInstance: starting server");
		final String path = getStubPath();
		try (FileOutputStream out = new FileOutputStream(path);
				ObjectOutputStream objOut = new ObjectOutputStream(out)) {
			implementation = new Implementation();
			stub = (ImageJInstance)UnicastRemoteObject.exportObject(implementation, 0);

			// Write serialized object
			makeFilePrivate(path);
			objOut.writeObject(stub);

			log.debug("SingleInstance: server ready");
		} catch (Exception e) {
			log.error(e);
		}
	}

	/**
	 * Clean up remote objects
	 */
	public static void shutDown() {
		try {
			if (SingleInstance.implementation != null) {
				UnicastRemoteObject.unexportObject(SingleInstance.implementation, true);
			}
		} catch (final NoSuchObjectException exc) {
			// No-op
		}
		try {
			if (SingleInstance.stub != null) {
				UnicastRemoteObject.unexportObject(SingleInstance.stub, true);
			}
		} catch (final NoSuchObjectException exc) {
			// No-op
		}
	}
}
