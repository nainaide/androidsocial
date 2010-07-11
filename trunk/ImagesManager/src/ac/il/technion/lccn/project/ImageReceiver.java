package ac.il.technion.lccn.project;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageReceiver implements Runnable {

	private InputStream inputStream;
	private OutputStream outputStream;

	public ImageReceiver(InputStream inputStream, OutputStream outputStream) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		if ( !new File( "userImages").exists()) {
			new File( "userImages").mkdir( );
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
				builder.append( in);
			}
			String userName = builder.toString( );
			File userPictureFile = new File( "userImages/" + userName + ".jpg");
			BufferedOutputStream stream = new BufferedOutputStream( new FileOutputStream(userPictureFile));
			// Read from socket until user streams the data
			while ( ( in = (byte)inputStream.read( )) != -1) {
				stream.write( in);
			}
			stream.flush( );
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				inputStream.close( );
				outputStream.close( );
			} catch (IOException e) {
				//  Never will get here.
			}
		}
	}

}
