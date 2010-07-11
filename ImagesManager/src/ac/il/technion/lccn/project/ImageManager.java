package ac.il.technion.lccn.project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageManager implements Runnable {

	ExecutorService executor = Executors.newFixedThreadPool( 10);
	ServerSocket socket;
	boolean interrupted = false;
	@Override
	public void run() {
		try {
			socket = new ServerSocket(1703);
			while (!interrupted) {
				Socket $ = socket.accept( );
				ImageManagerRequestHandler handler = new ImageManagerRequestHandler( executor);
				handler.handleRequest( $);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void shutdown( ) {
		interrupted = true;
		executor.shutdown( );
	}

}
