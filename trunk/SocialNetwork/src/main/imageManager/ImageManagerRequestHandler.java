package main.imageManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import android.util.Log;

public class ImageManagerRequestHandler {

	private static final String LOG_TAG = "ImageManagerRequestHandler";
	
	public static final int SEND_USER_IMAGE = 0x1;
	public static final int RECEIVE_USER_IMAGE = SEND_USER_IMAGE << 1;
	
	private ExecutorService executor;

	private IImageNotifiable notifiable;

	private String fileName;

	
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
			Log.e(LOG_TAG, "handleRequest() : IOException occurred. e.getMessage() = " + e.getMessage());
		} catch (Exception e) {
			Log.e(LOG_TAG, "handleRequest() : Exception occurred. e.getMessage() = " + e.getMessage());
		}
	}
}
