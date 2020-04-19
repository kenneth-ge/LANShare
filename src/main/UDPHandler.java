package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

import javafx.application.Platform;

import static main.Main.*;

public class UDPHandler {
	
	private Main main;
	
	public UDPHandler(Main main) {
		this.main = main;
	}

	boolean running = false;
	private DatagramSocket socket;
	private byte[] buf = new byte[256];
	
	public void poll() throws IOException {
		running = true;

		socket = new DatagramSocket(PORT);

		while (running) {
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);

			InetAddress address = packet.getAddress();
			int port = packet.getPort();

			String received = new String(packet.getData(), 0, packet.getLength());

			if (received.equals(HELLO)) {
				if (addresses.add(address)) {
					System.out.println(Arrays.toString(address.getAddress()));
					
					main.createButton(address);
					
					System.out.println("Send multiple times");
					sendMultipleTimes(HELLO, address, PORT);
				}
			} else if (received.equals(STOP)) {
				System.out.println("STOP");
				running = false;
				break;
			}

			packet = new DatagramPacket(buf, buf.length, address, port);

			socket.send(packet);
		}

		socket.close();
	}
	
	public void sendMultipleTimes(String message, InetAddress address, int PORT) throws IOException {
		DatagramSocket socket = new DatagramSocket();

		//send HELLO message back
		byte[] buffer = HELLO.getBytes();
		DatagramPacket datagram = new DatagramPacket(buffer, buffer.length, address, PORT);
		
		System.out.println(address);
		
		for(int i = 0; i < 3; i++) {
			socket.send(datagram);
			
			try {
				Thread.sleep(500);
			}catch(Exception e) {
				//do nothing
			}
		}
		socket.close();
	}
	
	public void stop() throws IOException {
		running = false;
	}
	
	public void cleanup() throws IOException {
		DatagramSocket socket = new DatagramSocket();

		byte[] buffer = STOP.getBytes();

		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"), PORT);
		socket.send(packet);
		socket.close();
	}

}
