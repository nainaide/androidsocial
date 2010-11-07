package main.imageManager;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

public class ImageManager implements Runnable {

	private static final String LOG_TAG = "ImageManager";
	
	private static final int TIMEOUT_SERVER_SOCKET_ACCEPT = 500;
	
	private static final long TIMEOUT_CREATE_SERVER = 1000;
	
	private ExecutorService executor = null;
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
			boolean isServerCreated = false;
			long timeStart = SystemClock.uptimeMillis();
			long timePassed = 0;
			
			// The thread from the previous run might still be running so the serverSocket creation will fail
			// because the port is already taken, so we wait a bit
			while (isServerCreated == false && timePassed < TIMEOUT_CREATE_SERVER)
			{
				try
				{
					serverSocket = new ServerSocket( ImageCommunicator.IMAGE_SERVER_PORT);
					
					isServerCreated = true;
				}
				catch (IOException e)
				{
					timePassed = SystemClock.uptimeMillis() - timeStart;
				}
			}
			
			serverSocket.setSoTimeout(TIMEOUT_SERVER_SOCKET_ACCEPT);
			
			interrupted = false;
			executor = Executors.newFixedThreadPool( 10);
			
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
			Log.e(LOG_TAG, "run() : Thread - " + Thread.currentThread().getId() + ", IOException occurred. e.getMessage() = " + e.getMessage());
		} catch (Exception e) {
			Log.e(LOG_TAG, "run() : Thread - " + Thread.currentThread().getId() + ", Exception occurred. e.getMessage() = " + e.getMessage());
		}
		finally {
			try {
				Log.d(LOG_TAG, "run() : Thread - " + Thread.currentThread().getId() + ", About to close the serverSocket and finish");
				
				serverSocket.close();
			} catch (Exception e) {
			}
		}

		Log.d(LOG_TAG, "run() : Thread - " + Thread.currentThread().getId() + ", Ending run()");
	}
	
	public void shutdown( ) {
		interrupted = true;
		executor.shutdown( );
	}
}
