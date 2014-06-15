package id.r5xscn.ardrone;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ARSense implements SensorEventListener {

	SensorManager sense;
	ARDrone armain;
	boolean run;
	int count = 0;
	float values[];
	float x, x1, y, y1, z, z1;
	String Adir;
	float speed = (float) 0.1;

	public ARSense(Object... params) {
		armain = (ARDrone) params[0];
		x = x1 = y = y1 = z = z1 = 0;
		Adir = "";
		run = false;
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			values = event.values;
			if (count == 0) {
				x = values[0];
				y = values[1];
				z = values[2];
				count++;
			} else if (count < 99) {
				learn();
				count++;
			} else if (count == 99) {
				x = x / 100;
				y = y / 100;
				z = z / 100;
				count++;
			} else {

				if ((x - values[0]) > 2) {
					Adir = "up";
				} else if ((x - values[0]) < -2) {
					Adir = "down";
				} else if ((y - values[1]) > 2) {
					Adir = "left";
				} else if ((y - values[1]) < -2) {
					Adir = "right";
				} else {
					Adir = "normal";
				}

				if (armain.jcontrollrun) {
					if (!armain.JStick.JSTThread.touched) {
						if ((x - values[0]) > 2) {
							Adir = "up";
							ATSend(true, 0, -speed, 0, 0);
						} else if ((x - values[0]) < -2) {
							Adir = "down";
							ATSend(true, 0, speed, 0, 0);
						} else if ((y - values[1]) > 2) {
							Adir = "left";
							ATSend(true, -speed, 0, 0, 0);
						} else if ((y - values[1]) < -2) {
							Adir = "right";
							ATSend(true, speed, 0, 0, 0);
						} else {
							Adir = "normal";
							ATSend(false, 0, 0, 0, 0);
						}
					}
				}

				armain.settView2(Adir);

			}
		}
	}

	public void learn() {
		x = x + values[0];
		y = y + values[1];
		z = z + values[2];
	}

	public void ATSend(boolean send, float pitch, float roll, float gaz,
			float yaw) {
		if (run && (!armain.sendctrl)) {
			armain.pitch = pitch;
			armain.roll = roll;
			armain.gaz = gaz;
			armain.yaw = yaw;
			if (send) {
				armain.ATSend();
			} else {
				if (armain.task.sendctrl) {
					armain.task.sendctrl = false;
				}
			}
		}
	}

}
