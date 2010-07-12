package ac.il.technion.lccn.project;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ImageSender implements Runnable {

	private InputStream inputStream;
	private OutputStream outputStream;
	private Socket socket;

	public ImageSender(Socket socket) {
		try {
			this.inputStream = socket.getInputStream( );
			this.outputStream = socket.getOutputStream( );
			this.socket = socket;
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	@Override
	public void run() {
		byte in;
		try {
			StringBuilder builder = new StringBuilder( );
			while( (in = (byte) inputStream.read( )) != -1) {
				if ( in == ';')
					break;
				builder.append( (char)in);
			}
			String userName = builder.toString( );
			File userPictureFile = new File( "userImages/" + userName + ".jpg");
			byte[] buffer = new byte[(int)userPictureFile.length( )];
			BufferedInputStream stream = new BufferedInputStream( new FileInputStream( userPictureFile));
			stream.read(buffer, 0, (int)userPictureFile.length( ));
			outputStream.write(buffer, 0, (int)userPictureFile.length( ));
			outputStream.flush( );
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if ( !socket.isInputShutdown())
					inputStream.close( );
				if ( !socket.isOutputShutdown())
					outputStream.close( );
				socket.close( );
			} catch (IOException e) {
				//  Never will get here.
			}
		}
		
	}

}
