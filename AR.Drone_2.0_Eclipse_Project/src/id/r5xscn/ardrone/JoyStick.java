package id.r5xscn.ardrone;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class JoyStick extends SurfaceView implements SurfaceHolder.Callback {
	JoyStickThread JSThread;
	JoyStickTouchThread JSTThread;
	ARDrone armain2;
	int x, y;
	Bitmap Joystick, JSBGround;
	String status = "", statustv = "";
	boolean run, status2 = true, draw = false;
	public JoyStick(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(this);
		WindowManager wm = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		x = 20;
		
		//Put the JoyStick at Bottom Left of the screen
		if (Build.VERSION.SDK_INT > 12) {
			Point size = new Point();
			display.getSize(size);
			y = size.y - (170);
		} else {
			y = display.getHeight() - 170; // deprecated
		}

		Joystick = BitmapFactory.decodeResource(getResources(),
				R.drawable.joystick);
		JSBGround = BitmapFactory.decodeResource(getResources(),
				R.drawable.joystick_bg);
		JSTThread = new JoyStickTouchThread(x + 45, y + 45, this);
		setOnTouchListener(JSTThread);
		//JSThread used for updating the JoyStick position
		JSThread = new JoyStickThread(getHolder(), this);
		run = false;
	}

	@SuppressLint("DrawAllocation")
	public void onDraw(Canvas canvas) {
		//Draw Joystick
		try{
		canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);}
		catch (Exception e){
			
		}
		if (draw)
			canvas.drawBitmap(armain2.bmp2, 0, 0, null);
		canvas.drawBitmap(JSBGround, x, y, null);
		canvas.drawBitmap(Joystick, JSTThread.TPoint.x - (45 - 27),
				JSTThread.TPoint.y - (45 - 27), null);
	}

	public void ATSend(boolean send, float pitch, float roll, float gaz,
			float yaw) {
		//Send the control through UDPSend thread
		if (run) {
			armain2.pitch = pitch;
			armain2.roll = roll;
			armain2.gaz = gaz;
			armain2.yaw = yaw;
			if (send) {
				armain2.ATSend();
			} else {
				if (armain2.task.sendctrl) {
					armain2.task.sendctrl = false;
				}
			}
		}
	}

	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

	}

	public void surfaceCreated(SurfaceHolder arg0) {
		JSThread.start();
	}

	public void Accesscontroll(ARDrone armain) {
		//For cross thread data exchange
		armain2 = armain;
		draw = true;
	}

	public void speedchange(float changedspeed) {
		JSTThread.speed = changedspeed;
	}

	public void surfaceDestroyed(SurfaceHolder arg0) {
		JSThread.run = false;
		boolean retry = true;
		
		JSThread.interrupt();

		while (retry) {
			try {
				JSThread.join();
				retry = false;
			} catch (InterruptedException e) {
				// we will try it again and again...
			}
		}
	}

}
