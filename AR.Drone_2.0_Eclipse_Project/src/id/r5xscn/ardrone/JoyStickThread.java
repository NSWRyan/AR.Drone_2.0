package id.r5xscn.ardrone;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.SurfaceHolder;

public class JoyStickThread extends Thread{
	SurfaceHolder SHolder;
	JoyStick JStick;
	boolean run;
	private long sleepTime;
	private long beforeTime;

	private long delay=35;
	
	public JoyStickThread(Object... params){
		SHolder=(SurfaceHolder) params[0];
		JStick=(id.r5xscn.ardrone.JoyStick) params[1];
		run=true;
	}
	public void run(){
		Canvas cvs;
		while(run){
			cvs=null;
			try{
				//Update Joystick Picture
				cvs=SHolder.lockCanvas(null);
				synchronized(SHolder){
					if(run){
						JStick.onDraw(cvs);
					}else{
						break;
					}
				}
			}finally{
				if(cvs!=null){
					SHolder.unlockCanvasAndPost(cvs);
				}
			}

			beforeTime = System.nanoTime();
			this.sleepTime = delay-((System.nanoTime()-beforeTime)/1000000L);

			try {
				//actual sleep code
				if(sleepTime>0){
					this.sleep(sleepTime);
				}
			} catch (InterruptedException ex) {
			}
		}
	}
}
