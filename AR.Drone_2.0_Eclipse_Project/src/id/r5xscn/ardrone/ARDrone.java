package id.r5xscn.ardrone;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

@SuppressLint({ "NewApi", "NewApi" })
public class ARDrone extends Activity implements View.OnTouchListener {

	static {
		System.loadLibrary("ffmpeg");
		System.loadLibrary("ffmpeg-test-jni");
	}

	// declare the jni functions
	public static native String naInit(Bitmap bitmap);

	int i1 = 0, isb = 10;
	ImageView IView;
	SeekBar SBar, SBar1, SBar2;
	ToggleButton TButton, AButton;
	TextView tView, tVSpeed, maxh, ta, tp, tr, ty, vx, vy, vz, tm;
	Button btn[] = new Button[7], eButton, tbutton;
	CheckBox cv;
	Handler handler;
	String st1 = "", st2 = "";
	UDPSend task;
	JoyStick JStick;
	int state = 0;
	Bitmap bmp, bmp2;
	boolean sendctrl, sendctrl1, fly, run, hover, emergency, sensor,
			jcontrollrun, toff1, vr;

	AlertDialog.Builder altDialog;
	ConnectivityManager connManager;
	NetworkInfo mWifi;

	// Drone controll
	int seq1 = 1, nav1 = 1;
	float speed = (float) 0.1, speed1;
	float pitch = 0, roll = 0, gaz = 0, yaw = 0;
	FloatBuffer fb;
	IntBuffer ib;

	Video video;

	ARSense arsense1;
	SensorManager sense;

	File directory;
	File file;

	int i12 = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main);

		bmp = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888);
		bmp2 = Bitmap.createBitmap(800, 480, Bitmap.Config.ARGB_8888);

		handler = new Handler();
		sendctrl = false;
		sendctrl1 = false;
		fly = false;
		run = true;
		hover = false;
		emergency = false;
		sensor = false;
		jcontrollrun = false;
		toff1 = false;
		vr = true;

		video = new Video(this);

		ByteBuffer bb = ByteBuffer.allocate(4);
		fb = bb.asFloatBuffer();
		ib = bb.asIntBuffer();
		tView = (TextView) findViewById(R.id.textView1);
		tVSpeed = (TextView) findViewById(R.id.textView3);
		TButton = (ToggleButton) findViewById(R.id.toggleButton1);
		btn[0] = (Button) findViewById(R.id.tRight);

		altDialog = new AlertDialog.Builder(this);

		altDialog
				.setMessage(
						"To switch to control mode, change this device orientation to landscape.")
				.setNeutralButton("OK", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
					}
				});
		sense = (SensorManager) getSystemService(SENSOR_SERVICE);

		arsense1 = new ARSense(this);
		if (btn[0].getVisibility() == View.VISIBLE) {
			// Initialize View for landscape and listener
			AButton = (ToggleButton) findViewById(R.id.toggleButton2);
			arsense1 = new ARSense(this);

			btn[1] = (Button) findViewById(R.id.tLeft);
			btn[2] = (Button) findViewById(R.id.tUp);
			btn[3] = (Button) findViewById(R.id.tDown);
			btn[4] = (Button) findViewById(R.id.tOff);
			eButton = (Button) findViewById(R.id.eButton);
			for (int loop = 0; loop < 4; loop++) {
				btn[loop].setOnTouchListener(this);
			}
			ta = (TextView) findViewById(R.id.a1);
			tp = (TextView) findViewById(R.id.p1);
			tr = (TextView) findViewById(R.id.r1);
			ty = (TextView) findViewById(R.id.y1);
			vx = (TextView) findViewById(R.id.vx1);
			vy = (TextView) findViewById(R.id.vy1);
			vz = (TextView) findViewById(R.id.vz1);
			tm = (TextView) findViewById(R.id.tm1);

		} else {
			connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

			cv = (CheckBox) findViewById(R.id.checkBox1);
			cv.setOnClickListener(new OnClickListener() {

				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					vr = cv.isChecked();
					if (vr) {
						cv.setText("On");
					} else {
						cv.setText("Off");
					}
				}
			});
			SBar = (SeekBar) findViewById(R.id.seekBar1);
			SBar1 = (SeekBar) findViewById(R.id.seekBar2);
			SBar1.setProgress(isb - 1);
			maxh = (TextView) findViewById(R.id.textView8);
			maxh.setText("10");
			SBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				public void onStopTrackingTouch(SeekBar arg0) {
				}

				public void onStartTrackingTouch(SeekBar arg0) {

				}

				public void onProgressChanged(SeekBar arg0, int arg1,
						boolean arg2) {
					speed = ((float) (arg1 + 1) / (float) 10);
					tVSpeed.setText("" + (arg1 + 1));
				}
			});
			SBar1.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				public void onStopTrackingTouch(SeekBar arg0) {
					isb = SBar1.getProgress() + 1;
					if (fly) {
						task.st3 = ",\"control:altitude_max\",\""
								+ (isb * 1000) + "\"";
						task.sendctrl2 = true;
					}
				}

				public void onStartTrackingTouch(SeekBar arg0) {

				}

				public void onProgressChanged(SeekBar arg0, int arg1,
						boolean arg2) {
					maxh.setText("" + (arg1 + 1));
				}
			});
		}

		TButton.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				mWifi = connManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

				if (mWifi.isConnected()) {
					if (TButton.getText().equals("On")) {
						if (!fly) {
							task = new UDPSend(st1, ARDrone.this, 1, 0, 0);
							task.start();
							fly = true;
							altDialog.show();
						}
					} else {
						if (fly) {
							fly = false;
							write();
							task.run = false;
						}
					}
				} else {
					TButton.toggle();
					Toast.makeText(getApplicationContext(),
							"WiFi is not connected!", Toast.LENGTH_SHORT)
							.show();
				}
			}
		});
	}

	public void toffp(View view) {
		if (fly) {
			if (btn[4].getText().equals("Take Off")) {
				btn[4].setText("Land");
				task.fly = true;
				toff1 = true;

			} else {
				toff1 = false;
				task.fly = false;
				btn[4].setText("Take Off");
			}
		}
	}

	public void motions(View view) {
		if (AButton.getText().equals("Motion On")) {
			sensorInit();
		} else {
			sense.unregisterListener(arsense1);
			tm.setText("Motion: Off");
			sensor = false;
		}
	}

	public void Jcontroll() {
		JStick = (JoyStick) findViewById(R.id.joyStick1);
		JStick.Accesscontroll(this);
		JStick.run = true;
		JStick.speedchange(speed);
		jcontrollrun = true;
	}

	public void onSaveInstanceState(Bundle SInstance) {
		if (fly) {
			// Save sequence and configuration sequence
			SInstance.putInt("arsequence", task.i);
			SInstance.putInt("state1", task.i1);
			SInstance.putInt("h1", task.h1);
			SInstance.putString("h2", task.height);
		}
		// Safe configuration sequence
		SInstance.putFloat("speed", speed);
		SInstance.putBoolean("run", fly);
		SInstance.putBoolean("fly1", toff1);
		SInstance.putBoolean("vr1", vr);
		SInstance.putInt("maxh1", isb);
		super.onSaveInstanceState(SInstance);
	}

	public void onRestoreInstanceState(Bundle RInstance) {
		super.onRestoreInstanceState(RInstance);
		state = RInstance.getInt("state1");
		speed = RInstance.getFloat("speed");
		fly = RInstance.getBoolean("run");
		vr = RInstance.getBoolean("vr1");
		toff1 = RInstance.getBoolean("fly1");
		isb = RInstance.getInt("maxh1");
		// If orientation ini configuration layout
		if (btn[0].getVisibility() != View.VISIBLE) {
			SBar1.setProgress(isb - 1);
			maxh.setText(isb + "");
		}
		// If flying start UDPSend thread
		if (fly) {
			i1 = RInstance.getInt("arsequence");
			// Start UDPSend thread with saved data
			task = new UDPSend(RInstance.getString("h2"), this, i1, state,
					RInstance.getInt("h1"));
			task.start();
			// Check if orientation in control layout
			if (btn[0].getVisibility() == View.VISIBLE) {
				Jcontroll();
				if (toff1) {
					btn[4].setText("Land");
				} else {
					btn[4].setText("Take Off");
				}
				// If video enabled start video thread for handling TCP
				if (vr) {
					video = new Video(this);
					video.start();
				}
			}

			if (toff1) {
				task.fly = true;
			}
		}

		if (btn[0].getVisibility() != View.VISIBLE) {
			if (vr) {
				cv.setText("On");
			} else {
				cv.setText("Off");
				cv.toggle();
			}
		}

	}

	public void sensorInit() {
		if (AButton.getText().equals("Motion Off")) {
			AButton.toggle();
		}
		sense.registerListener(arsense1,
				sense.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_GAME);
		arsense1.count = 0;
		arsense1.speed = speed;
		sensor = true;
		if (fly) {
			arsense1.run = true;
		}
	}

	public void ARSpeed() {
		if (pitch != 0) {
			if (pitch > 0)
				pitch = speed;
			else
				pitch = -speed;
		} else if (roll != 0) {
			if (roll > 0)
				roll = speed;
			else
				roll = -speed;
		} else if (gaz != 0) {
			if (gaz > 0)
				gaz = speed;
			else
				gaz = -speed;
		} else if (yaw != 0) {
			if (yaw > 0)
				yaw = speed;
			else
				yaw = -speed;
		}
	}

	void settView(final int txt1, final float txt2, final float txt3,
			final float txt4, final float txt5, final float txt6,
			final float txt7, final float txt8) {
		handler.post(new Runnable() {
			public void run() {
				tView.setText("Battery: " + txt1);
				ta.setText("Altitude: " + txt2);
				tp.setText("Pitch: " + txt3);
				tr.setText("Roll: " + txt4);
				ty.setText("Yaw: " + txt5);
				vx.setText("Velocity X: " + txt6);
				vy.setText("Velocity Y: " + txt7);
				vz.setText("Velocity Z: " + txt8);
			}
		});
	}

	void settView2(final String text1) {
		handler.post(new Runnable() {
			public void run() {
				tm.setText("Motion: " + text1);
			}
		});
	}

	public void textctrl(String tv1) {
		tView.setText(tv1);
	}

	public void ATClear() {
		pitch = 0;
		roll = 0;
		gaz = 0;
		yaw = 0;
	}

	public void ATSend() {
		// if(true){
		task.st1 = "AT*PCMD=";

		task.st2 = ",1," + intOfFloat(pitch) + "," + intOfFloat(roll) + ","
				+ intOfFloat(gaz) + "," + intOfFloat(yaw);
		/*
		 * }else{ task.st1="AT*MOTOR="; task.st2="," + (int)(roll*50) + "," +
		 * (int)(pitch*50) + "," + (int)(gaz*50) + "," + (int)(yaw*50); }
		 */
		task.sendctrl = true;

	}

	public void eButtonapp(View view) {
		if (sensor) {
			arsense1.run = false;
			sense.unregisterListener(arsense1,
					sense.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
			AButton.toggle();
			sensor = false;

		}
		if (fly) {
			task.sendctrl1 = true;
			toff1 = false;
			btn[4].setText("Take Off");
		}
	}

	public int intOfFloat(float f) {
		fb.put(0, f);
		return ib.get(0);
	}

	public boolean onTouch(View v, MotionEvent event) {
		if (fly) {
			switch (v.getId()) {
			case R.id.tRight:
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					sendctrl = true;
					yaw = speed;
					ATSend();
					break;
				case MotionEvent.ACTION_UP:
					sendctrl = false;
					yaw = 0;
					task.sendctrl = false;
					break;
				}
				break;
			case R.id.tLeft:
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					sendctrl = true;
					yaw = -speed;
					ATSend();
					break;
				case MotionEvent.ACTION_UP:
					sendctrl = false;
					yaw = 0;
					task.sendctrl = false;
					break;
				}
				break;
			case R.id.tUp:
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					sendctrl = true;
					tView.setText("asd");
					gaz = speed;
					ATSend();
					break;
				case MotionEvent.ACTION_UP:
					sendctrl = false;
					gaz = 0;
					task.sendctrl = false;
					break;
				}
				break;
			case R.id.tDown:
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					sendctrl = true;
					gaz = -speed;
					ATSend();
					break;
				case MotionEvent.ACTION_UP:
					sendctrl = false;
					gaz = 0;
					task.sendctrl = false;
					break;
				}
				break;
			default:
				break;
			}
		}
		return false;

	}

	public void write() {
		if (!task.height.equals("")) {
			directory = Environment.getExternalStorageDirectory();

			file = new File(directory + "/tinggi.txt");

			int f1 = 0;
			while (file.exists()) {
				file = new File(directory + "/tinggi" + (f1++) + ".txt");
			}
			try {
				file.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			BufferedWriter writer = null;

			try {
				FileWriter fw = new FileWriter(file);
				writer = new BufferedWriter(fw);
				writer.write(task.height);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	protected void onDestroy() {
		super.onDestroy();
		// task.run=false;
		// finish();
	}

	protected void onPause() {
		super.onPause();

		// If AR.Drone 2.0 connected
		if (fly) {
			boolean retry = true;
			task.run = false;
			int idelay = 0;
			// Delay for some time for safety
			while (idelay < 1000) {
				idelay++;
			}
			// Stop thread if video thread is running
			if (video.isAlive()) {
				video.run = false;
			}
			retry = true;
			// Stop UDPSend thread until thread stopped
			while (retry) {
				try {
					task.join();
					retry = false;
				} catch (InterruptedException e) {
				}
			}
			// Stop video thread until thread stopped if video thread is running
			if (video.isAlive()) {
				retry = true;
				video.interrupt();
				while (retry) {
					try {
						video.join();
						retry = false;
					} catch (InterruptedException e) {
					}
				}
			}
		}
		// Unregister accelerometer sensor listener
		if (sensor) {
			sense.unregisterListener(arsense1);
			sensor = false;
		}
	}

}
