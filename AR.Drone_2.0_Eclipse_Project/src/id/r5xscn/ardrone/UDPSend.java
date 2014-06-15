package id.r5xscn.ardrone;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Handler;
import android.view.View;

public class UDPSend extends Thread {

	DatagramPacket sendPacket, receivePacket;

	byte data[];

	static final int NAV_PORT = 5554;
	static final int NAV_STATE_OFFSET = 4;
	static final int NAV_BATTERY_OFFSET = 24;
	static final int NAV_PITCH_OFFSET = 28;
	static final int NAV_ROLL_OFFSET = 32;
	static final int NAV_YAW_OFFSET = 36;
	static final int NAV_ALTITUDE_OFFSET = 40;
	static final int NAV_VX_OFFSET = 44;
	static final int NAV_VY_OFFSET = 48;
	static final int NAV_VZ_OFFSET = 52;

	// 00 FLY MASK : (0) ardrone is landed, (1) ardrone is flying
	// 01 VIDEO MASK : (0) video disable, (1) video enable
	// 02 VISION MASK : (0) vision disable, (1) vision enable
	// 03 CONTROL ALGO : (0) euler angles control, (1) angular speed control

	// 04 ALTITUDE CONTROL ALGO : (0) altitude control inactive (1) altitude
	// control active
	// 05 USER feedback : Start button state
	// 06 Control command ACK : (0) None, (1) one received
	// 07 Trim command ACK : (0) None, (1) one received

	// 08 Trim running : (0) none, (1) running
	// 09 Trim result : (0) failed, (1) succeeded
	// 10 Navdata demo : (0) All navdata, (1) only navdata demo
	// 11 Navdata bootstrap : (0) options sent in all or demo mode, (1) no
	// navdata options sent

	// 12 Motors status : (0) Ok, (1) Motors Com is down
	// 13
	// 14 Bit means that there's an hardware problem with gyrometers 8
	// 15 VBat low : (1) too low, (0) Ok

	// 16 VBat high (US mad) : (1) too high, (0) Ok 4
	// 17 Timer elapsed : (1) elapsed, (0) not elapsed
	// 18 Power : (0) Ok, (1) not enough to fly
	// 19 Angles : (0) Ok, (1) out of range

	// 20 Wind : (0) Ok, (1) too much to fly 8
	// 21 Ultrasonic sensor : (0) Ok, (1) deaf
	// 22 Cutout system detection : (0) Not detected, (1) detected
	// 23 PIC Version number OK : (0) a bad version number, (1) version number
	// is OK

	// 24 ATCodec thread ON : (0) thread OFF (1) thread ON 4
	// 25 Navdata thread ON : (0) thread OFF (1) thread ON
	// 26 Video thread ON : (0) thread OFF (1) thread ON
	// 27 Acquisition thread ON : (0) thread OFF (1) thread ON

	// 28 CTRL watchdog : (1) delay in control execution (> 5ms), (0) control is
	// well scheduled // Check frequency of control loop 9
	// 29 ADC Watchdog : (1) delay in uart2 dsr (> 5ms), (0) uart2 is good //
	// Check frequency of uart2 dsr (com with adc)
	// 30 Communication Watchdog : (1) com problem, (0) Com is ok // Check if we
	// have an active connection with a client
	// 31 Emergency landing : (0) no emergency, (1) emergency
	static final int fly_mask = 1 << 0;
	static final int video_mask = 1 << 1;
	static final int vision_mask = 1 << 2;
	static final int control_algo = 1 << 3;
	static final int alt_ctrl_algo = 1 << 4;
	static final int usr_fdbck = 1 << 5;
	static final int ctrl_cmd_ack = 1 << 6;
	static final int trim_cmd_ack = 1 << 7;
	static final int trim_run = 1 << 8;
	static final int trim_rslt = 1 << 9;
	static final int navdata_demo = 1 << 10;
	static final int nav_data_bootstrap = 1 << 11;
	static final int mtr_stat = 1 << 12;
	static final int unk = 1 << 13;
	static final int gyro = 1 << 14;
	static final int vbat = 1 << 15;
	static final int vbat_high = 1 << 16;
	static final int timer = 1 << 17;
	static final int power = 1 << 18;
	static final int angles = 1 << 19;
	static final int wind = 1 << 20;
	static final int ultrasonic = 1 << 21;
	static final int current_system_detection = 1 << 22;
	static final int PIC = 1 << 23;
	static final int atcodec_thread = 1 << 24;
	static final int navdata_thread = 1 << 25;
	static final int video_thread = 1 << 26;
	static final int acquisition_thread = 1 << 27;
	static final int ctrl_watchdog = 1 << 28;
	static final int ADC_watchdog = 1 << 29;
	static final int com_watchdog = 1 << 30;
	static final int emergency_landing = 1 << 31;

	String local_ip = "", ardrone_ip_cur = "";
	float pitch, roll, yaw, altitude, vx, vy,vz,
			heading, alt_us, alt_baro, alt_baro_raw;
	int len, battery, status;
	byte[] navdata;
	InetAddress ard;
	String st1, st2, st3;
	int port = 5556;
	int past, now;
	DatagramSocket socket, rocket, rocket2;
	InetAddress ARD;
	ARDrone armain;
	boolean sendctrl, sendctrl1, sendctrl2, run, fly, atcsend;
	byte[] trigger_bytes = { 0x01, 0x00, 0x00, 0x00 };
	int i = 1, i1 = 0, state;
	Handler handler;
	WifiManager wifi;
	MulticastLock lock;
	DhcpInfo dhcp;
	String atcommand = "";
	int print_cnt = 0, timeout_cnt = 0, boot = 0, h1=0;
	String height="";
	String eol = System.getProperty("line.separator");

	public UDPSend(Object... params) {
		height = (String) params[0];
		armain = (ARDrone) params[1];
		i = (Integer) params[2];
		i1 = (Integer) params[3];
		h1=(Integer) params[4];
		state = 0;
		st1 = "";
		st2 = "";
		st3 = "";
		sendctrl = false;
		sendctrl1 = false;
		sendctrl2 = false;
		fly = false;
		atcsend = false;
		// Open connection
		try {
			ARD = InetAddress.getByName("192.168.1.1");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		try {
			socket = new DatagramSocket();
			rocket = new DatagramSocket(5554);
			rocket.setSoTimeout(25);
			rocket.setReuseAddress(true);
			data = new byte[10240];
		} catch (SocketException e) {
			e.printStackTrace();
		}
		run = true;
	}

	void UDP(String AT) {
		DatagramPacket p;
		try {
			// p = new DatagramPacket(At.getBytes(),
			// At.length(),getBroadcastAddress(),5556);
			p = new DatagramPacket(AT.getBytes(), AT.length(), ARD, 5556);
			// socket.setBroadcast(false);
			socket.send(p);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void delay(int delay1) {
		try {
			Thread.sleep(delay1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void reconnect_nav() {
		try {
			// rocket2=null;
			//
			sendPacket = new DatagramPacket(trigger_bytes,
					trigger_bytes.length, ARD, 5554);
			rocket.send(sendPacket);
			// rocket2.close();
			// rocket2=null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void reconnect_nav2() {
		atcommand = atcommand + "AT*CONFIG_IDS=" + (i++)
				+ ",\"5aef8689\",\"eb1daa72\",\"9b92d2e0\"" + "\r";

		atcommand = atcommand + "AT*CONFIG=" + i++
				+ ",\"general:navdata_demo\",\"TRUE\"" + "\r";
	}

	public int get_int(byte[] data, int offset) {
		int tmp = 0, n = 0;

		for (int i = 3; i >= 0; i--) {
			n <<= 8;
			tmp = data[offset + i] & 0xFF;
			n |= tmp;
		}

		return n;
	}

	public float get_float(byte[] data, int offset) {
		return Float.intBitsToFloat(get_int(data, offset));
	}

	public void atconfudp(String aconf) {
		atcommand = atcommand + "AT*CONFIG_IDS=" + (i++)
				+ ",\"5aef8689\",\"eb1daa72\",\"9b92d2e0\"" + "\r"
				+ "AT*CONFIG=" + (i++) + aconf + "\r";
	}

	public void atconf() {
		if (i1 == 1) {
			atconfudp(",\"custom:session_id\",\"5aef8689\"");
			atcsend = false;
			return;
		}
		if (i1 == 2) {
			atconfudp(",\"custom:application_id\",\"9b92d2e0\"");
			atcsend = false;
			return;
		}
		if (i1 == 3) {
			atconfudp(",\"custom:profile_id\",\"eb1daa72\"");
			atcsend = false;
			return;
		}
		if (i1 == 4) {
			atconfudp(",\"custom:session_desc\",\"Session 5aef8689\"");
			atcsend = false;
			return;
		}
		if (i1 == 5) {
			atconfudp(",\"general:navdata_demo\",\"TRUE\"");
			atcsend = false;
			return;
		}
		if (i1 == 6) {
			atconfudp("AT*CONFIG=" + (i++) + ",\"video:codec_fps\",\"30\"");
			atcsend = false;
			return;
		}
		if (i1 == 7) {
			atconfudp("AT*CONFIG=" + (i++) + ",\"video:video_codec\",\"129\"");
			atcsend = false;
			return;
		}
		if (i1 == 8) {
			atconfudp("AT*CONFIG=" + (i++) + ",\"video:max_bitrate\",\"4000\"");
			atcsend = false;
			return;
		}
		if (i1 == 9) {
			atconfudp(",\"control:altitude_max\",\"" + (armain.isb) * 1000
					+ "\"");
			atcsend = false;
			return;
		}
		if (i1 == 10) {
			atconfudp(",\"control:altitude_min\",\"500\"");
			atcsend = false;
			return;
		}
	}

	public void run() {
		String status1 = "";
		reconnect_nav();
		if (i == 1) {
			atcommand = atcommand + "AT*PMODE=" + (i++) + ",2" + "\r"
					+ "AT*MISC=" + (i++) + ",2,20,2000,3000" + "\r"
					+ "AT*CONFIG_IDS=" + (i++)
					+ ",\"5aef8689\",\"eb1daa72\",\"9b92d2e0\"" + "\r"
					+ "AT*CONFIG=" + (i++) + ",\"custom:session_id\",\"-all\""
					+ "\r";
		}
		while (run) {
			try {
				receivePacket = new DatagramPacket(data, data.length);
				rocket.receive(receivePacket);
				len = receivePacket.getLength();
				print_cnt = 0;
				if (len == trigger_bytes.length) {
					status1 = status1 + "Interrupt bytes received, ignore it"
							+ "\n";
					if (++timeout_cnt <= 2)
						continue;
					timeout_cnt = 0;
					reconnect_nav();
				}
				if (len <= 24) {
					status1 = status1
							+ "In BOOTSTRAP mode, reconnect to switch to DEMO mode"
							+ "\n";
				}

				navdata = receivePacket.getData();
				state = get_int(navdata, NAV_STATE_OFFSET);

				if ((state & com_watchdog) > 0) {
					atcommand = atcommand + "AT*COMWDG=" + (armain.task.i++)
							+ "\r";
				}
				if ((state & ctrl_cmd_ack) > 0) {
					atcommand = atcommand + "AT*CTRL=" + (armain.task.i++)
							+ ",5,0" + "\r";
					status1 = status1 + "CTRL Mask" + "\n";
					if (i1 <= 10 && !atcsend) {
						atcsend = true;
						i1++;
					}
				}

				if ((state & trim_cmd_ack) > 0) {
					status1 = status1 + "MYKONOS_TRIM_COMMAND_MASK" + "\n";
					Thread.sleep(100);
				}

				if ((state & trim_rslt) > 0) {
					status1 = status1 + "MYKONOS_TRIM_RESULT_MASK" + "\n";
				}

				if ((state & angles) > 0) {
					status1 = status1 + "MYKONOS_ANGLES_OUT_OF_RANGE" + "\n";
				}

				if ((state & wind) > 0) {
					status1 = status1 + "MYKONOS_WIND_MASK" + "\n";
				}

				if ((state & ultrasonic) > 0) {
					status1 = status1 + "MYKONOS_ULTRASOUND_MASK" + "\n";
				}

				if ((state & emergency_landing) > 0) {
					status1 = status1 + "MYKONOS_EMERGENCY_MASK" + "\n";
				}
				if (len > 24) {
					battery = get_int(navdata, NAV_BATTERY_OFFSET);
					altitude = ((float) get_int(navdata, NAV_ALTITUDE_OFFSET) / 1000);
					height+=(++h1)+"	"+altitude+eol;
					pitch = get_float(navdata, NAV_PITCH_OFFSET) / 1000;
					roll = get_float(navdata, NAV_ROLL_OFFSET) / 1000;
					yaw = get_float(navdata, NAV_YAW_OFFSET) / 1000;

					vx = get_float(navdata, NAV_VX_OFFSET);
					vy = get_float(navdata, NAV_VY_OFFSET);
					vz = get_float(navdata, NAV_VZ_OFFSET);
				}
				if(armain.btn[0].getVisibility()==View.VISIBLE){
					armain.settView(battery,altitude,pitch,roll,yaw,vx,vy,vz);
				}

			} catch (IOException e) {
			} catch (Exception e) {
			}

			if (atcsend) {
				atconf();

			}
			if (sendctrl2) {
				atconfudp(st3);
				sendctrl2 = false;
			}
			if (++print_cnt > 100) {
				reconnect_nav();
				reconnect_nav2();
				print_cnt = 0;
			}
			if (sendctrl) {
				atcommand = atcommand + st1 + (i++) + st2 + "\r";
			} else {
				atcommand = atcommand + "AT*PCMD=" + (i++) + ",0,0,0,0,0\r";
			}
			if (sendctrl1) {
				atcommand = atcommand + "AT*REF=" + (i++) + ",290717952\r";
				fly = false;
				armain.toff1 = false;
				if (++status >= 2) {
					sendctrl1 = false;
					status = 0;
				}
			} else {
				if (fly) {
					atcommand = atcommand + "AT*REF=" + (i++) + ",290718208"
							+ "\r";
				} else {
					atcommand = atcommand + "AT*REF=" + (i++) + ",290717696"
							+ "\r";
				}
			}
			// atcommand=atcommand+"AT*COMWDG="+(i++)+"\r"+"AT*REF="+(i++)+",290717696\rAT*REF="+(i++)+",290717952\rAT*REF="+(i++)+",290717696";

			UDP(atcommand);
			atcommand = "";
			if (!armain.fly) {
				st1 = "AT*REF=";
				st2 = ",290717696";
				for (int i2 = 0; i2 < 30; i2++) {
					UDP(st1 + (i++) + st2);
					delay(30);
				}
			}
		}
		rocket.close();
		socket.close();
		this.interrupt();

	}
}
