package main.imageManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class ImageSender implements Runnable {

	private OutputStream outputStream;
	private Socket socket;
	private String fileName;

	public ImageSender(Socket socket, String fileName) {
		try {
			this.outputStream = socket.getOutputStream( );
			this.socket = socket;
			this.fileName = fileName;
		} catch (IOException e) {
			e.printStackTrace();
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
			e.printStackTrace();
		} finally {
			try {
				if ( !socket.isOutputShutdown())
					outputStream.close( );
				socket.close( );
			} catch (IOException e) {
				//  Never will get here.
			}
		}
		
	}

}
