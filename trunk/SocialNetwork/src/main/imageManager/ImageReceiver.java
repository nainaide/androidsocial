package main.imageManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import android.util.Log;

public class ImageReceiver implements Runnable {

	private static final String LOG_TAG = "ImageReceiver";
	
	private InputStream inputStream;
	private OutputStream outputStream;
	private Socket socket;
	private IImageNotifiable notifiable;

	public ImageReceiver(Socket socket, IImageNotifiable notifiable) {
		try {
			this.inputStream = socket.getInputStream( );
			this.outputStream = socket.getOutputStream( );
			this.socket = socket;
			this.notifiable = notifiable;
			if ( !new File( "userImages").exists()) {
				new File( "userImages").mkdir( );
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, "ImageReceiver CTor - IOException. e.getMessage() = " + e.getMessage());
		}
	}

//	@Override
	public void run() {
		int in;
		try {
			StringBuilder builder = new StringBuilder( );
			while( (in = inputStream.read( )) != -1) {
				if ( in == ';')
					break;
				builder.append( (char)in);
			}
			String userName = builder.toString( );
			File userPictureFile = new File( "/sdcard/" + userName + ".jpg");
			BufferedOutputStream stream = new BufferedOutputStream( new FileOutputStream(userPictureFile));
			// Read from socket until user streams the data
			while ( ( in = inputStream.read( )) != -1) {
				stream.write( in);
			}
			stream.flush( );
			notifiable.imageReady( userName);
		} catch (IOException e) {
			Log.e(LOG_TAG, "run() - IOException. e.getMessage() = " + e.getMessage());
		} finally {
			try {
				if ( !socket.isInputShutdown())
					inputStream.close( );
				if ( !socket.isOutputShutdown())
					outputStream.close( );
				socket.close( );
			} catch (Exception e) {
				//  Never will get here.
			}
		}
	}
}
