package main.imageManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import android.util.Log;

public class ImageSender implements Runnable {

	private static final String LOG_TAG = "ImageSender";

	private OutputStream outputStream;
	private Socket socket;
	private String fileName;

	public ImageSender(Socket socket, String fileName) {
		try {
			this.outputStream = socket.getOutputStream( );
			this.socket = socket;
			this.fileName = fileName;
		} catch (IOException e) {
			Log.e(LOG_TAG, "ImageSender CTor - IOException. e.getMessage() = " + e.getMessage());
		} catch (Exception e) {
			Log.e(LOG_TAG, "ImageSender CTor - Exception. e.getMessage() = " + e.getMessage());
		} 
	}

//	@Override
	public void run() {
		if ( fileName == null || fileName.length() == 0 ) 
			return;
		try {
			File userPictureFile = new File( fileName);
			byte[] buffer = new byte[(int)userPictureFile.length( )];
			BufferedInputStream stream = new BufferedInputStream( new FileInputStream( userPictureFile));
			
			stream.read(buffer, 0, (int)userPictureFile.length( ));
			
			outputStream.write(buffer, 0, (int)userPictureFile.length( ));
			outputStream.flush( );
		} catch (IOException e) {
			Log.e(LOG_TAG, "run() - IOException. e.getMessage() = " + e.getMessage());
		} finally {
			try {
				if ( !socket.isOutputShutdown())
					outputStream.close( );
				socket.close( );
			} catch (Exception e) {
				//  Never will get here.
			}
		}
	}
}
