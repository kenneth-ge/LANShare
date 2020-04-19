package main;

import static main.Main.SEND_FILE;
import static main.Main.SEND_FILE_OK;
import static main.Main.STOP;
import static main.Main.TCP_PORT;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class TCPHandler implements Runnable {

	private Main main;
	boolean running = false;
	
	private ServerSocket servsock;
	private Alert alert;
	private String fileName;
	
	public TCPHandler(Main main) {
		this.main = main;
		
		pbAlert = new Alert(AlertType.NONE);
		pbAlert.setTitle("Transfering file...");
		pbAlert.setHeaderText("File transfer in progress...");
		pbAlert.setContentText("Progress: ");

        pb = new ProgressBar();

		// layout manager
		VBox vbox = new VBox();
		vbox.setFillWidth(true);
		vbox.setMaxWidth(Double.MAX_VALUE);
		vbox.setAlignment(Pos.CENTER_LEFT);
		
		pb.prefWidthProperty().bind(vbox.widthProperty().subtract(10));
		vbox.getChildren().add(pb);
		pbAlert.getDialogPane().setContent(vbox);
		
		//pbAlert.setResizable(true);
	}
	
	public void receivePoll() throws IOException {
		servsock = new ServerSocket(TCP_PORT);

		running = true;
		while (running) {
			System.out.println("Waiting...");

			Socket socket = servsock.accept();
			System.out.println("Accepted connection " + socket);

			System.out.println(socket.getInetAddress());

			// write data out
			PrintStream ps = new PrintStream(socket.getOutputStream());

			// to read data coming from the client
			DataInputStream br = new DataInputStream(socket.getInputStream());
			BufferedReader br2 = new BufferedReader(new InputStreamReader(br));
			String action = br2.readLine();
			
			if(action.equals(STOP)) {
				br2.close();
				br.close();
				ps.close();
				break;
			}
			
			fileName = br2.readLine();//.replace("\0", "");
			final double size = Long.parseLong(br2.readLine());
			
			Platform.runLater(() -> {
				alert = new Alert(AlertType.CONFIRMATION);
				alert.setTitle("Accept File");
				alert.setHeaderText("Accept file?");
				alert.setContentText(
						"Are you sure you want to receive file " + fileName + " from " + socket.getInetAddress() + "?");
				alert.setResizable(true);
				
				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == ButtonType.OK) {
					ps.println(SEND_FILE_OK);
					ps.flush();
 
					resetProgressBar();
					
					new Thread(() -> {
						DataOutputStream bw;
						try {
							String home = System.getProperty("user.home");
							File f = new File(home + "/Desktop/" + fileName);
							
							String baseName = FilenameUtils.getBaseName(f.getName());

							int count = 1;

							while (f.exists()) {
								f = new File(home + "/Desktop/" + baseName + " (" + count + ")." + FilenameUtils.getExtension(f.getName()));
								count++;
							}

							bw = new DataOutputStream(new FileOutputStream(f));

							byte[] buffer = new byte[8192];
							
							double bytesWritten = 0;
							long lastCounter = 0;
							
							while ((count = br.read(buffer)) > 0) {
								bytesWritten += count;
								bw.write(buffer, 0, count);
								
								long counter = (long)(200 * bytesWritten / size);
								if(counter > lastCounter) {
									lastCounter = counter;
									
									final double d = bytesWritten;
									Platform.runLater(() -> {
										pb.setProgress(d / size);
									});
								}
							}
							bw.close();
							
							final File f2 = f;

							Platform.runLater(() -> {
								closeProgressDialog();
								
								alert = new Alert(AlertType.INFORMATION);
								alert.setTitle("File transfer complete");
								alert.setHeaderText("File transfer complete");
								alert.setContentText(
										"Successfully received " + fileName + ". It is stored at " + f2.getAbsolutePath());
								alert.setResizable(true);
								alert.showAndWait();
							});
						} catch (Exception e) {
							e.printStackTrace();
						}
					}).start();
				} else {
					System.out.println("Refused file");
				}
			});

		}

		servsock.close();
	}
	
	private Socket tcpSocket;
	private DataOutputStream out;
	
	public void establishConnection(InetAddress address) throws IOException {
		if (main.selectedFile == null) {
			System.err.println("No file selected");
			return;
		}

		System.out.println("Attempting to send file...");
		System.out.println("Doing handshake...");
		tcpSocket = new Socket(address, TCP_PORT);

		var in = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
		out = new DataOutputStream(new BufferedOutputStream(tcpSocket.getOutputStream()));

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
		bw.write(SEND_FILE + "\n");
		bw.write(main.selectedFile.getName() + "\n");
		bw.write(main.selectedFile.length() + "\n");
		bw.flush();

		String response = in.readLine();
		System.out.println(response);
		System.out.println(SEND_FILE_OK);
		if (!response.equals(SEND_FILE_OK)) {
			out.flush();
			tcpSocket.close();
			out.close();

			Platform.runLater(() -> {
				alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Other device refused");
				alert.setHeaderText("Connection refused");
				alert.setContentText("The other device refused to accept the file transfer. Please try again. ");
				alert.setResizable(true);
				alert.showAndWait();
			});
			return;
		}
		
		resetProgressBar();
		
		System.out.println("Start thread");
		new Thread(this).start();
	}
	
	private ProgressBar pb;
	private Alert pbAlert;
	
	private double bytesRead = 0;
	
	@Override
	public void run() {
		try {
			double totalBytes = main.selectedFile.length();
			
			BufferedInputStream	fis = new BufferedInputStream(new FileInputStream(main.selectedFile));
			
			byte[] buffer = new byte[8192];
	
			int count;
			double lastCounter = 0;
			bytesRead = 0;
			
			while ((count = fis.read(buffer)) > 0) {
				out.write(buffer, 0, count);
				
				bytesRead += count;
				
				double numOutOf200 = (200.0 * bytesRead) / (totalBytes);
				
				if((int)numOutOf200 > lastCounter) {
					System.out.println("Progress: " + (bytesRead / totalBytes));
					
					lastCounter = (int)numOutOf200;
					
					Platform.runLater(() -> {
						pb.setProgress(bytesRead / totalBytes);
					});
				}
			}
	
			System.out.println("File sent -- flushing");
			out.flush();
			fis.close();
			tcpSocket.close();
			out.close();
	
			Platform.runLater(() -> {
				closeProgressDialog();
				
				alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("File sent successfully");
				alert.setHeaderText("File transfered successfully");
				alert.setContentText("The file was sent successfully!");
				alert.setResizable(true);
				alert.showAndWait();
			});
		} catch (IOException e) {
			Platform.runLater(() -> {
				closeProgressDialog();
				
				alert = new Alert(AlertType.WARNING);
				alert.setTitle("Failed to send file");
				alert.setHeaderText("File transfered unsuccessfully");
				alert.setContentText("File failed to transfer");
				alert.setResizable(true);
				alert.showAndWait();
			});
			e.printStackTrace();
		}
	}
	
	public void stop() {
		running = false;
	}
	
	public void closeProgressDialog() {
		pbAlert.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
		pbAlert.close();
	}
	
	public void cleanup() throws UnknownHostException, IOException {
		if(servsock != null) {
			Socket socket = new Socket("127.0.0.1", TCP_PORT);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			out.println(STOP);
			out.close();
			socket.close();
		}
	}
	
	public void resetProgressBar() {
		Platform.runLater(() -> {
			pb.setProgress(0);
			pbAlert.getDialogPane().getButtonTypes().clear();
			pbAlert.show();
			
		});
	}
	
}
