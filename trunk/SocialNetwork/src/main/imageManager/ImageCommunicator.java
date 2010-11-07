package main.imageManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.util.Log;

public class ImageCommunicator {

	private static final String LOG_TAG = "ImageCommunicator";
	
	public static final int IMAGE_SERVER_PORT = 1703;
	
	private String host;
	private int port;

	public ImageCommunicator( String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public void sendImage( String fileName, String userName) {
		BufferedInputStream inputStream = null;
		OutputStream 		outputStream = null;
		Socket				socket = null;
		
		try {
			socket = new Socket(host, port);
			
			File file = new File( fileName);
			byte[] sendBuffer = new byte[(int)file.length( )];
			
			inputStream = new BufferedInputStream( new FileInputStream( file));
			inputStream.read(sendBuffer, 0, sendBuffer.length);
			
			outputStream = socket.getOutputStream( );
			outputStream.write( ImageManagerRequestHandler.SEND_USER_IMAGE);
			outputStream.write( (userName + ";").getBytes( ), 0, (userName + ";").length( ));
			outputStream.write(sendBuffer, 0, sendBuffer.length);
			
			if ( !socket.isOutputShutdown( ))
				outputStream.flush( );
		} catch (UnknownHostException e) {
			Log.e(LOG_TAG, "sendImage - UnknownHostException. e.getMessage() = " + e.getMessage());
		} catch (IOException e) {
			Log.e(LOG_TAG, "sendImage - IOException. e.getMessage() = " + e.getMessage());
		} finally {
			try {
				if (socket != null) {
					socket.close();
				}
				if ( inputStream != null) {
					inputStream.close( );
				}
				if ( outputStream != null) {
					outputStream.close( );
				}
			} catch (IOException e) {
			}
		}
	}
	
	public void requestImage( String userName) {
		BufferedOutputStream fileOut = null;
		OutputStream 		 outputStream = null;
		InputStream			 inputStream = null;
		Socket				 socket = null;
		
		try {
			socket = new Socket(host, port);
			
			outputStream = socket.getOutputStream( );
			inputStream = socket.getInputStream( );
			
			outputStream.write( ImageManagerRequestHandler.RECEIVE_USER_IMAGE);
			
			int ch = 0;
			fileOut = new BufferedOutputStream( new FileOutputStream( "/sdcard/" + userName + ".jpg"));
			while ( ( ch = inputStream.read()) != -1) {
				fileOut.write( ch);
			}
			fileOut.flush( );
		} catch (UnknownHostException e) {
			Log.e(LOG_TAG, "requestImage - UnknownHostException. e.getMessage() = " + e.getMessage());
		} catch (IOException e) {
			Log.e(LOG_TAG, "requestImage - IOException. e.getMessage() = " + e.getMessage());
		} finally {
			try {
				if (socket != null) {
					socket.close();
				}					
				if ( fileOut != null)
					fileOut.close( );
				if ( outputStream != null)
					outputStream.close( );
				if ( inputStream != null) 
					inputStream.close( );
			} catch (IOException e) {
			}
		}
	}
}
