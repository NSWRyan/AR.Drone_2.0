package id.r5xscn.ardrone;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class Video extends Thread {

	ServerSocket ssocket;
	Socket socket;
	String sentence;
	String capitalizedSentence;
	boolean run = true, run2 = false;
	OutputStream out;
	DataInputStream input;
	JoyStick js;
	byte[] data;
	BufferedReader inFromClient;
	InetAddress ard;
	BigInteger bi;
	ARDrone main;
	ServerSocket ss;

	static final int signature = 0;
	static final int version8 = 4;
	static final int video_codec8 = 5;
	static final int header_size16 = 6;
	static final int payload_size32 = 8;
	static final int encoded_stream_width16 = 12;
	static final int encoded_stream_height16 = 14;
	static final int display_width16 = 16;
	static final int display_height16 = 18;
	static final int frame_number32 = 20;
	static final int timestamp32 = 24;
	static final int total_chunks8 = 28;
	static final int chunk_index8 = 29;
	static final int frame_type8 = 30;
	static final int control8 = 31;
	static final int stream_byte_position_lw32 = 32;
	static final int stream_byte_position_uw32 = 36;
	static final int stream_id16 = 40;
	static final int total_slices8 = 42;
	static final int slice_index8 = 43;
	static final int header1_size8 = 44;
	static final int header2_size8 = 45;

	public Video(Object... params) {
		main = (ARDrone) params[0];
		js = main.JStick;
	}

	public int uint8(byte[] convert, int offset) {
		return convert[offset] & 0xFF;
	}

	public int uint16LE(byte[] convert, int offset) {
		return ((convert[offset + 1] & 0xFF) << 8) | (convert[offset] & 0xFF);
	}

	public long uint32LE(byte[] convert, int offset) {
		return ((convert[offset + 3] & 0xFF) << 24)
				| ((convert[offset + 2] & 0xFF) << 16)
				| ((convert[offset + 1] & 0xFF) << 8)
				| (convert[offset] & 0xFF);
	}

	public byte[] pave() {
		byte[] paveh = new byte[4];
		paveh[0] = (byte) (paveh[0] | 0x50);
		paveh[1] = (byte) (paveh[1] | 0x61);
		paveh[2] = (byte) (paveh[2] | 0x56);
		paveh[3] = (byte) (paveh[3] | 0x45);
		return paveh;
	}

	@Override
	public void run() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}

		while (run) {
			try {
				ard = InetAddress.getByName("192.168.1.1");
				socket = new Socket(ard, 5555);
				ss = new ServerSocket(1234);
				input = new DataInputStream(socket.getInputStream());
				out = new ByteArrayOutputStream();

			} catch (UnknownHostException e1) {
				e1.printStackTrace();
				js.status = js.status + "\nerror1";
			} catch (IOException e) {
				e.printStackTrace();
				js.status = js.status + "\nConnection error";
				run = false;
				this.interrupt();
			}
			byte[] check;
			int count = 0;
			int len = 0;
			int test = 0;
			while (run) {

				try {
					byte[] buffer = new byte[1460];

					boolean pass = false;
					boolean run1 = true;
					count = 0;
					data = new byte[0];
					check = new byte[0];
					while (run1) {
						len = input.read(buffer);
						if (len == 1460) {
							data = Arraycopy(data, buffer, count, len);
							count += len;
							run2 = true;
							pass = true;
							if(count>25000){
								run2=true;
								break;
							}
							continue;
						} else if (len < 1460 && len > 0 && pass) {
							data = Arraycopy(data, buffer, count, len);
							check = Arrays.copyOfRange(data, 0, 4);
							count += len;

							run2 = true;
							run1 = false;
						} else {
							run1 = false;
						}
					}
					if (run2 && Arrays.equals(check, pave()) && count > 7000) {
						new ffupdate(main).start();
						new tcphandle(data, ss.accept()).start();
					}
				} catch (IOException e) {
				}
			}
			try {
				input.close();
				out.close();
				socket.close();
				input = null;
				out = null;
				socket = null;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	public byte[] Arraycopy(byte[] b1, byte[] b2, int length1, int length2) {
		if (b1 == null)
			return b2;
		if (b2 == null)
			return b1;
		byte[] b3 = new byte[length1 + length2];
		System.arraycopy(b1, 0, b3, 0, length1);
		System.arraycopy(b2, 0, b3, length1, length2);
		return b3;
	}

	public static String bytesToHex(byte[] bytes) {
		final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
				'9', 'A', 'B', 'C', 'D', 'E', 'F' };
		char[] hexChars = new char[bytes.length * 2];
		int v;
		for (int j = 0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
}
