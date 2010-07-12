package ac.il.technion.lccn.project;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageSender implements Runnable {

	private InputStream inputStream;
	private OutputStream outputStream;

	public ImageSender(InputStream inputStream, OutputStream outputStream) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
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
			byte[] buffer = new byte[(int)userPictureFile.length( )];
			BufferedInputStream stream = new BufferedInputStream( new FileInputStream( userPictureFile));
			stream.read(buffer, 0, (int)userPictureFile.length( ));
			outputStream.write(buffer, 0, (int)userPictureFile.length( ));
			outputStream.flush( );
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
