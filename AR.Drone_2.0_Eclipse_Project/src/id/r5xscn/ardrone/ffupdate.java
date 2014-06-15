package id.r5xscn.ardrone;

import android.graphics.Bitmap;
import android.graphics.Matrix;


public class ffupdate extends Thread{
	ARDrone main;
	Matrix matrix;
	float scaleWidth,scaleHeight;
	public ffupdate(ARDrone main2) {

		scaleWidth = ((float) 800) / 640;
		scaleHeight = ((float) 480) / 360;

		matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		main=main2;
	}
	@Override
	public void run() {
		try {
			Thread.sleep(70);
			ARDrone.naInit(main.bmp);
			main.bmp2=Bitmap.createBitmap(main.bmp, 0, 0, 640, 360, matrix, false);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
