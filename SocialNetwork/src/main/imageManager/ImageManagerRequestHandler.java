package main.imageManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import android.util.Log;

public class ImageManagerRequestHandler {

	private static final String LOG_TAG = "ImageManagerRequestHandler";
	
	private ExecutorService executor;

	private IImageNotifiable notifiable;

	private String fileName;
	
	final static public int SEND_USER_IMAGE = 0x1;
	final static public int RECEIVE_USER_IMAGE = SEND_USER_IMAGE << 1;
	
	public ImageManagerRequestHandler(ExecutorService executor, IImageNotifiable notifiable, String fileName) {
		this.executor = executor; 
		this.notifiable = notifiable;
		this.fileName = fileName;
	}

	public void handleRequest( Socket socket) {
		try {
			InputStream inputStream = socket.getInputStream( );
			int messageType = inputStream.read( );
			Runnable handler;
			if ( (messageType & SEND_USER_IMAGE) != 0) {
				handler = new ImageReceiver( socket, notifiable);
			} else {
				handler = new ImageSender( socket, fileName);
			}
			executor.execute(handler);
		} catch (IOException e) {
			Log.e(LOG_TAG, "run() : IOException occurred. e.getMessage() = " + e.getMessage());
//			e.printStackTrace();
		}
		
	}
	
	
}
