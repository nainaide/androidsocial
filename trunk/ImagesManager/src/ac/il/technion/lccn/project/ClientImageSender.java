package ac.il.technion.lccn.project;

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

public class ClientImageSender {

	
	private String host;
	private int port;

	public ClientImageSender( String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public void sendImage( String fileName, String userName) {
		BufferedInputStream inputStream = null;
		OutputStream 		outputStream = null;
		try {
			Socket socket = new Socket(host, port);
			File file = new File( fileName);
			byte[] sendBuffer = new byte[(int)file.length( )];
			inputStream = new BufferedInputStream( new FileInputStream( file));
			inputStream.read(sendBuffer, 0, sendBuffer.length);
			outputStream = socket.getOutputStream( );
			outputStream.write( ImageManagerRequestHandler.SEND_USER_IMAGE);
			outputStream.write( (userName + ";").getBytes( ), 0, (userName + ";").length( ) + 1);
			outputStream.write(sendBuffer);
			outputStream.flush( );
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
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
		OutputStream 		outputStream = null;
		InputStream			inputStream = null;
		try {
			Socket socket = new Socket(host, port);
			fileOut = new BufferedOutputStream( new FileOutputStream( userName + ".jpg"));
			outputStream = socket.getOutputStream( );
			inputStream = socket.getInputStream( );
			byte ch = 0;
			outputStream.write( ImageManagerRequestHandler.SEND_USER_IMAGE);
			outputStream.write( (userName + ";").getBytes( ), 0, (userName + ";").length( ) + 1);
			while ( ( ch = (byte)inputStream.read()) != -1) {
				fileOut.write( ch);
			}
			fileOut.flush( );
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
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
