package main.imageManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Looper;
import android.util.Log;

public class ImageManager implements Runnable {

	private static final String LOG_TAG = "ImageManager";
	
	ExecutorService executor = Executors.newFixedThreadPool( 10);
	ServerSocket serverSocket;
	boolean interrupted = false;
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
			Looper.prepare();
			while (!interrupted) {
//			while (Thread.currentThread().isInterrupted() == false) {
				Socket socket = serverSocket.accept( );
				ImageManagerRequestHandler handler = new ImageManagerRequestHandler( executor, notifiable, fileName);
				handler.handleRequest( socket);
			}
		} catch (IOException e) {
//			e.printStackTrace();
			Log.e(LOG_TAG, "run() : IOException occurred. e.getMessage() = " + e.getMessage());
		}
		finally {
			try {
				Log.d(LOG_TAG, "run() : About to close the serverSocket and finish");
				
				serverSocket.close();
			} catch (IOException e) {
			}
		}
	}
	
	public void shutdown( ) {
		interrupted = true;
		executor.shutdown( );
	}
	
//	public static void main(String[] args) {
//		final ImageManager manager = new ImageManager( new IImageNotifiable( ) {
//			public void imageReady( String imageName) {
//			};
//		});
//		new Thread( manager).start( );
//		Runtime.getRuntime( ).addShutdownHook( new Thread( ) {
//			public void run( ) {
//				manager.shutdown( );
//			}
//		});
//		while( true) {
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
//	}

}
