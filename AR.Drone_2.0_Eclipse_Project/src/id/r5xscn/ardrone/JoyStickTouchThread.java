package id.r5xscn.ardrone;

import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class JoyStickTouchThread implements OnTouchListener {
	int x, y, xb, yb, JSDir;
	Point TPoint;
	JoyStick jsmain;
	boolean touched;

	float speed = (float) 0.1;

	public JoyStickTouchThread(Object... params) {
		xb = (Integer) params[0];
		yb = (Integer) params[1];
		jsmain = (JoyStick) params[2];
		TPoint = new Point(xb, yb);
		JSDir = 0;
		touched = false;
	}

	public boolean onTouch(View v, MotionEvent event) {
		if (event != null) {
			touched = true;
			if ((int) event.getX() < (xb + 105)
					&& (int) event.getY() > (yb - 105)) {
				//Translate touch coordinate to JoyStick movement
				TPoint.x = ((int) event.getX());
				TPoint.y = ((int) event.getY());

				if (TPoint.x < (xb - 45)) {
					TPoint.x = xb - 45;
				}
				if (TPoint.y <= (yb - 45)) {
					TPoint.y = yb - 45;
				}
				if (TPoint.x > (xb + 45)) {
					TPoint.x = xb + 45;
				}
				if (TPoint.y > (yb + 45)) {
					TPoint.y = yb + 45;
				}
				toDir();
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {
				TPoint.x = xb;
				TPoint.y = yb;
				JSDir = 0;
				touched = false;
				jsmain.ATSend(false, 0, 0, 0, 0);
			}
		}

		return true;
	}

	public void toDir() {
		/*
		 * if(TPoint.x<=(xb-45)&&TPoint.y<=(yb-45)){ JSDir=8;
		 * jsmain.ATSend(true,-speed,-speed,0,0); return; }
		 * if(TPoint.x<=(xb-45)&&TPoint.y>=(yb+45)){ JSDir=6;
		 * jsmain.ATSend(true, -speed, speed, 0, 0); return; }
		 * if(TPoint.x>=(xb+45)&&TPoint.y>=(yb+45)){ JSDir=4;
		 * jsmain.ATSend(true, speed, speed, 0, 0); return; }
		 * if(TPoint.x>=(xb+45)&&TPoint.y<=(yb-45)){ JSDir=2;
		 * jsmain.ATSend(true, speed, -speed, 0, 0); return; }
		 */
		//Translate JoyStick coordinate to direction
		if (TPoint.x <= (xb - 45)) {
			jsmain.ATSend(true, -speed, 0, 0, 0);
			return;
		}
		if (TPoint.y <= (yb - 45)) {
			jsmain.ATSend(true, 0, -speed, 0, 0);
			return;
		}
		if (TPoint.x >= (xb + 45)) {
			jsmain.ATSend(true, speed, 0, 0, 0);
			return;
		}
		if (TPoint.y >= (yb + 45)) {
			jsmain.ATSend(true, 0, speed, 0, 0);
			return;
		}
		jsmain.ATSend(false, 0, 0, 0, 0);
	}

	public void armove(float pitch1, float roll1, float gaz1, float yaw1) {
	}
}

