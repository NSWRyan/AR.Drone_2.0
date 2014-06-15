package id.r5xscn.ardrone;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class tcphandle extends Thread {
	Socket socket;
	byte[] data;

	ServerSocket ss;

	public tcphandle(byte[] data2, Socket socket2) {
		socket = socket2;
		data = data2;
	}

	public void run() {
		PrintStream outToClient;
		try {
			outToClient = new PrintStream(socket.getOutputStream());
			outToClient.write(data, 0, data.length);

			outToClient.flush();
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
