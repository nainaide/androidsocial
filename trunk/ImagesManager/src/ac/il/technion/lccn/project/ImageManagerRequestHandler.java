package ac.il.technion.lccn.project;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class ImageManagerRequestHandler {

	private ExecutorService executor;
	
	final static public int SEND_USER_IMAGE = 0x1;
	final static public int RECEIVE_USER_IMAGE = SEND_USER_IMAGE << 1;
	
	public ImageManagerRequestHandler(ExecutorService executor) {
		this.executor = executor; 
	}

	public void handleRequest( Socket socket) {
		try {
			InputStream inputStream = socket.getInputStream( );
			int messageType = inputStream.read( );
			Runnable handler;
			if ( (messageType & SEND_USER_IMAGE) != 0) {
				handler = new ImageReceiver( socket);
			} else {
				handler = new ImageSender( socket);
			}
			executor.execute(handler);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
}
