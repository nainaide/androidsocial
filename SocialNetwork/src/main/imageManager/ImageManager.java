package main.imageManager;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import main.main.ApplicationSocialNetwork;
import android.os.Looper;
import android.util.Log;

public class ImageManager implements Runnable {

	private static final String LOG_TAG = "ImageManager";
	
	private ExecutorService executor = Executors.newFixedThreadPool( 10);
	private ServerSocket serverSocket;
	private boolean interrupted = false;
	private IImageNotifiable notifiable;
	private String fileName;
	
	public String getFileName() {
		return fileName;
	}

	public synchronized void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public ImageManager( IImageNotifiable notifiable) {
		this.notifiable = notifiable;
	}
	
//	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket( ImageCommunicator.IMAGE_SERVER_PORT);
			serverSocket.setSoTimeout(ApplicationSocialNetwork.TIMEOUT_SOCKET_RECEIVE);
			Looper.prepare();
			while (!interrupted) {
				try {
					Socket socket = serverSocket.accept( );
					ImageManagerRequestHandler handler = new ImageManagerRequestHandler( executor, notifiable, fileName);
					handler.handleRequest( socket);
				} catch (InterruptedIOException e) {
				}
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, "run() : IOException occurred. e.getMessage() = " + e.getMessage());
		}
		finally {
			try {
				Log.d(LOG_TAG, "run() : About to close the serverSocket and finish");
				
				serverSocket.close();
			} catch (Exception e) {
			}
		}
	}
	
	public void shutdown( ) {
		interrupted = true;
		executor.shutdown( );
	}
}
