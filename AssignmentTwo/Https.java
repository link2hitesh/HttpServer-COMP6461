package com.computerNetwork;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * * HTTP server in java ServerSocket and Socket class.
 */
public class Https extends Thread {
	// server parameters
	private static Boolean debug = false;
	private static int port = 8080;
	private static String directory = "C:\\Users\\hites\\OneDrive\\Desktop\\ConcordiaProject";

	// client parameters
	private Socket clientSideSocket;
	private String responseStatus = "HTTP/1.1 200 OK\r\n";
	private String fileLocURL = "C:\\Users\\hites\\OneDrive\\Desktop\\ConcordiaProject";
	private String fileNames = "";
	private String fileContent = "";
	private String ContentType = "Content-Type:text/html";
	private String ContentDisposition = "";

	private Https(Socket clientSocket)
	{
		this.clientSideSocket = clientSocket;
	}

	private static void printlog(String message) {
		if (debug) {
			System.out.println(message);
		}
	}

	public void response() {
		try {
			printlog("sending response");
			OutputStream os = this.clientSideSocket.getOutputStream();
			DataOutputStream dataOutputStream = new DataOutputStream(os);
			dataOutputStream.writeBytes(this.responseStatus);
			if (this.ContentType != null && this.ContentType != "") {
				dataOutputStream.writeBytes(this.ContentType + "\r\n");
			}
			if (this.ContentDisposition != null && this.ContentDisposition != "") {
				dataOutputStream.writeBytes(this.ContentDisposition + "\r\n");
			}
			dataOutputStream.writeBytes("\r\n");
			if (this.fileNames != null && this.fileNames != "") {
				dataOutputStream.writeBytes(this.fileNames + "\r\n");
			}
			if (this.fileContent != null && this.fileContent != "") {
				dataOutputStream.writeBytes(this.fileContent + "\r\n");
			}
			clientSideSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public boolean urlValidation(String url) {
		String[] locs = url.trim().split("/");
		for (String loc : locs) {
			if ("..".equalsIgnoreCase(loc)) {
				fileLocURL = fileLocURL.substring(0, fileLocURL.lastIndexOf("\\"));
			} else {
				fileLocURL = fileLocURL + "\\" + loc;
			}
		}
		fileLocURL = fileLocURL.replace('/', '\\');
		if (fileLocURL.contains(directory)) {
			printlog("Accessing URL - " + fileLocURL);
			return true;
		} else {
			responseStatus = "HTTP/1.1 401 UNAUTHORIZED ACCESS\r\n";
			return false;
		}
	}

	private void listFilesName() {
		printlog("Fetching list of files names at - " + this.fileLocURL);
		File folder = new File(this.fileLocURL);
		File[] files = folder.listFiles();
		if (files != null) {
			if (files.length > 0) {
				for (final File fileEntry : files) {
					if (!fileEntry.isDirectory()) {
						this.fileNames = this.fileNames + fileEntry.getName() + "\r\n";
					}
				}
			} else {
				this.fileNames = "No files in the directory\r\n";
				printlog("No files in the directory.");
			}

		} else {
			printlog("DIRECTORY NOT FOUND.");
			responseStatus = "HTTP/1.1 404 DIRECTORY NOT FOUND\r\n";
		}
	}

	public synchronized void saveFileContent(String inlineData) {
		// writes the contents
		try {
			printlog("Saving data in file - " + this.fileLocURL);
			FileWriter file = new FileWriter(this.fileLocURL, true);
			BufferedWriter bufferedWriter = new BufferedWriter(file);
			bufferedWriter.append(System.lineSeparator() + inlineData);
			bufferedWriter.close();
		} catch (IOException e) {
			printlog("Saving unsuccessful");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized void getFileContent() {
		printlog("Fetching content of file - " + this.fileLocURL);
		// This will reference one line at a time
		String line = null;
		// String inlineData = null;
		try {
			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(this.fileLocURL);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			while ((line = bufferedReader.readLine()) != null) {
				if (this.fileContent == null)
					this.fileContent = line + "\r\n";
				else
					this.fileContent = this.fileContent + line + "\r\n";
			}
			// Always close files.
			bufferedReader.close();
		} catch (FileNotFoundException e) {
			printlog("file not found");
			responseStatus = "HTTP/1.1 404 FILE NOT FOUND\r\n";
			e.printStackTrace();
		} catch (IOException e) {
			printlog("file reading error");
			responseStatus = "HTTP/1.1 410 FILE READING ERROR\r\n";
			e.printStackTrace();
		}
	}

	public void run() {
		this.fileLocURL = directory;
		printlog("The Client details: InetAddress - " + clientSideSocket.getInetAddress() + " , port - "
				+ clientSideSocket.getPort());
		String requestType;
		String url;
		try {
			BufferedReader inputBufferedReader = new BufferedReader(
					new InputStreamReader(this.clientSideSocket.getInputStream()));
			String[] request = inputBufferedReader.readLine().trim().split(" ");
			requestType = request[0];
			url = request[1];
			url = url.replace("https://", " ");
			url = url.replace("http://", " ");
			if (!url.contains("/")) {
				url = url + "/";
			}
			url = url.substring(url.indexOf("/"), url.length());

			// Check for unauthorized access
			printlog("Validating for unauthorized access.");
			boolean validation = urlValidation(url);
			if (validation) {
				printlog("Access Authorized.");
			} else {
				printlog("Access Unauthorized.");
			}

			// code to read headers line
			// code to read and print headers
			String headerLine = null;
			while ((headerLine = inputBufferedReader.readLine()).length() != 0) {
				if (headerLine.contains("Content-Type")) {
					this.ContentType = headerLine.trim();
				}
				if (headerLine.contains("Content-Disposition")) {
					this.ContentDisposition = headerLine.trim();
				}
			}

			// fetch other details from request
			StringBuilder payload = new StringBuilder();
			char temp;
			while (inputBufferedReader.ready()) {
				temp = (char) inputBufferedReader.read();
				/*if(temp == '\r' || temp == '\n') {
					payload.append(System.lineSeparator());
				}*/
				payload.append(temp);
			}
			// System.out.println("Payload data is: " + payload.toString());

			if (validation) {
				switch (requestType.trim().toUpperCase()) {
				case "GET":
					if (!url.trim().contains(".txt")) {
						listFilesName();
						printlog("Files presents - " + this.fileNames);
					} else {
						getFileContent();
					}
					break;
				case "POST":
					saveFileContent(payload.toString());
					break;
				}
			}
			response();

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Some error occured at Server");
		}
	}

	public static void main(String args[]) throws IOException {

		// set servers parameters
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-v")) {
				debug = true;
				continue;
			}
			if (args[i].equalsIgnoreCase("-p")) {
				port = Integer.parseInt(args[i + 1]);
				continue;
			}
			if (args[i].equalsIgnoreCase("-d")) {
				directory = args[i + 1];
				continue;
			}
		}

		// start server socket
		ServerSocket server = new ServerSocket(port, 10000, InetAddress.getByName("127.0.0.1"));
		System.out.println("Server start listening for connection on port " + port + " ....");

		// start threads for each client connection
		while (true) {
			Socket clientSocket = server.accept();
			if (clientSocket != null) {
				// new thread for each connection
				printlog("Creating new request thread.");
				new Https(clientSocket).start();
			}
		}
	}
}
