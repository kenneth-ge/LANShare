package main;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import com.jfoenix.controls.JFXButton;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

	public static int counter = 0;

	public static void main(String[] args) throws Exception {
		launch(args);
	}

	public static int width, height;
	public Scene start, receive, send;

	public Stage primaryStage;

	public Thread pollThread, tcpReceiveThread;

	static Set<InetAddress> addresses = new HashSet<>();

	public static final String HELLO = "Hello", STOP = "Stop", SEND_FILE_OK = "OK SEND FILE", SEND_FILE  = "SEND FILE";
	public static final int PORT = 4445, TCP_PORT = PORT + 1;

	private UDPHandler udpHandler;
	private TCPHandler tcpHandler;
	
	private VBox deviceListReceive;
	
	private AnchorPane rootContent;
	private ScreenHandler screenHandler;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		
        Scene fxml = null;
        
        try {
        	FXMLLoader loader = new FXMLLoader(getClass().getResource("drawermaterial.fxml"));
        	loader.setController(this);
	        AnchorPane parent = loader.load();
	        fxml = new Scene(parent);
	        fxml.getStylesheets().add("main/stylesheet.css");
	        rootContent = (AnchorPane) fxml.lookup("#content");
        }catch(Exception e) {
        	e.printStackTrace();
        }
		
		System.out.println(primaryStage);
		
		primaryStage.setTitle("LAN File Sharing");

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		width = screenSize.width / 3;
		height = screenSize.width / 3;

		otherDevices = new VBox(10);
		otherDevices.setAlignment(Pos.CENTER);
		
		deviceListReceive = new VBox(10);
		deviceListReceive.setAlignment(Pos.CENTER);

		screenHandler = new ScreenHandler();
		
		start = setupStartScene();
		receive = setupReceive();
		send = setupSend();
		info = screenHandler.setupInfo(rootContent);
		about = screenHandler.setupAbout(rootContent);
		
		start.getStylesheets().add("main/stylesheet.css");
		receive.getStylesheets().add("main/stylesheet.css");
		send.getStylesheets().add("main/stylesheet.css");
		
		udpHandler = new UDPHandler(this);
		tcpHandler = new TCPHandler(this);
		
        Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
        for (; n.hasMoreElements();)
        {
                NetworkInterface e = n.nextElement();
                System.out.println("Interface: " + e.getName());
                Enumeration<InetAddress> a = e.getInetAddresses();
                for (; a.hasMoreElements();)
                {
                        InetAddress addr = a.nextElement();
                        System.out.println("  " + addr.getHostAddress());
                        addresses.add(addr);
                }
        }
        
        primaryStage.setScene(fxml/* start */);
        primaryStage.setResizable(false);
        primaryStage.show();
	}

	public Scene setupStartScene() {
		JFXButton receive = new JFXButton();
		receive.setText("Receive Files");
		receive.setOnAction((e) -> receive(e));

		JFXButton send = new JFXButton();
		send.setText("Send Files");
		send.setOnAction((event) -> send(event));

		VBox root = new VBox(10);
		root.setAlignment(Pos.CENTER);
		root.getChildren().addAll(receive, send);

		return new Scene(root, width, height);
	}

	public Scene setupReceive() {
		JFXButton receive = new JFXButton();
		
		receive.setText("Check for other devices");
		receive.setOnAction((e) -> sendBroadcast());
		
		receiveRoot = new VBox(10);
		receiveRoot.setAlignment(Pos.CENTER);
		receiveRoot.getChildren().addAll(receive);
		receiveRoot.setMaxSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
		receiveRoot.prefWidthProperty().bind(rootContent.widthProperty());
		receiveRoot.prefHeightProperty().bind(rootContent.heightProperty());
		
		receiveRoot.getChildren().addAll(new Label("Available Devices:"), deviceListReceive);
		
		return new Scene(receiveRoot, width, height);
	}

	public void sendBroadcast() {
		try {
			broadcast(HELLO, InetAddress.getByName("255.255.255.255"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void broadcast(String broadcastMessage, InetAddress address) throws IOException {
		DatagramSocket socket = new DatagramSocket();
		socket.setBroadcast(true);

		byte[] buffer = broadcastMessage.getBytes();

		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, PORT);
		socket.send(packet);
		socket.close();
	}

	File selectedFile;
	private VBox sendRoot, receiveRoot, otherDevices, info, about;

	public Scene setupSend() {
		HBox hbox = new HBox();
		hbox.setAlignment(Pos.CENTER);
		Label label = new Label("No file selected");
		JFXButton chooseFile = new JFXButton();
		chooseFile.setText("Choose File");
		chooseFile.setOnAction((e) -> {
			FileChooser fileChooser = new FileChooser();
			selectedFile = fileChooser.showOpenDialog(primaryStage);
			if(selectedFile == null)
				return;
			label.setText(selectedFile.getName());
		});
		hbox.getChildren().addAll(label, chooseFile);

		sendRoot = new VBox(10);
		sendRoot.setAlignment(Pos.CENTER);
		sendRoot.prefWidthProperty().bind(rootContent.widthProperty());
		sendRoot.prefHeightProperty().bind(rootContent.heightProperty());
		sendRoot.getChildren().addAll(hbox, new Label("Send to: "), otherDevices);

		return new Scene(sendRoot, width, height);
	}

	@Override
	public void stop() throws Exception {
		udpHandler.stop();
		
		if (pollThread != null) {
			udpHandler.cleanup();
		}
		
		tcpHandler.stop();
		tcpHandler.cleanup();
				
		super.stop();
		
		System.exit(0);
	}
	
	public void startPollThread() {
		pollThread = new Thread(() -> {
			try {
				udpHandler.poll();
			} catch (IOException e1) {
				Platform.runLater(() -> {
					Alert alert = new Alert(AlertType.WARNING);
					alert.setTitle("Warning");
					alert.setHeaderText("Unable to find other devices");
					alert.setContentText("The program is unable to find other devices. Please see if you have any other instances open. ");
					alert.setResizable(true);
					alert.showAndWait();
				});
				e1.printStackTrace();
			}
		});

		pollThread.start();
	}
	
	public void createButton(InetAddress address) {
		Platform.runLater(() -> {
			long start = System.currentTimeMillis();
			otherDevices.getChildren().add(createJFXButton(address, address.getHostName()));
			System.out.println("Add button " + address.getHostName());
			System.out.println("Total time: " + (System.currentTimeMillis() - start));
			
			deviceListReceive.getChildren().add(new Label(address.getHostName()));
			
			System.out.println(deviceListReceive.getChildren());
		});
	}
	
	private JFXButton createJFXButton(InetAddress address, String name) {
		JFXButton btn = new JFXButton(name);
		btn.setOnAction((e) -> {
			try {
				tcpHandler.establishConnection(address);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});

		return btn;
	}

	@FXML
	public void send(ActionEvent event) {
		for(Node n: rootContent.getChildren()) {
			TranslateTransition tt = new TranslateTransition(Duration.seconds(1), n);
			tt.setByX(500);
			
			tt.play();
		}
		
		//this.rootContent.getChildren().clear();
		
		sendRoot.setBackground(Background.EMPTY);
		
		TranslateTransition tt = new TranslateTransition(Duration.seconds(1), sendRoot);
		tt.setFromY(-500);
		tt.setToY(0);
		
		tt.play();
		
		this.rootContent.getChildren().add(sendRoot);
		
		startPollThread();
	}
	
	@FXML
	public void receive(ActionEvent event) {
		startPollThread();

		tcpReceiveThread = new Thread(() -> {
			try {
				tcpHandler.receivePoll();
			} catch (IOException e1) {
				e1.printStackTrace();
				Platform.runLater(() -> {
					Alert alert = new Alert(AlertType.WARNING);
					alert.setTitle("Warning");
					alert.setHeaderText("Unable to receive files");
					alert.setContentText("Something went wrong :'(. Please close any other open instances of this program. ");
					alert.setResizable(true);
					alert.showAndWait();
				});
			}
		});

		tcpReceiveThread.start();

		for(Node n: rootContent.getChildren()) {
			TranslateTransition tt = new TranslateTransition(Duration.seconds(1), n);
			tt.setByX(500);
			
			tt.play();
		}
		
		//this.rootContent.getChildren().clear();
		
		receiveRoot.setBackground(Background.EMPTY);
		
		TranslateTransition tt = new TranslateTransition(Duration.seconds(1), receiveRoot);
		tt.setFromY(-500);
		tt.setToY(0);
		
		tt.play();
		
		this.rootContent.getChildren().add(receiveRoot);
	}
	
	@FXML
	public void info() {
		this.rootContent.getChildren().remove(info);
		
		for(Node n: rootContent.getChildren()) {
			TranslateTransition tt = new TranslateTransition(Duration.seconds(1), n);
			tt.setToX(500);
			
			tt.play();
		}
		
		//this.rootContent.getChildren().clear();
		
		TranslateTransition tt = new TranslateTransition(Duration.seconds(1), info);
		tt.setFromY(-500);
		tt.setToY(0);
		tt.setToX(0);
		
		tt.play();
		
		this.rootContent.getChildren().add(this.info);
	}
	
	@FXML
	public void about() {
		this.rootContent.getChildren().remove(about);
		
		for(Node n: rootContent.getChildren()) {
			TranslateTransition tt = new TranslateTransition(Duration.seconds(1), n);
			//tt.setFromX(0);
			tt.setToX(500);
			
			tt.play();
		}
		
		//this.rootContent.getChildren().clear();
		
		System.out.println("Hi");
		
		TranslateTransition tt = new TranslateTransition(Duration.seconds(1), about);
		tt.setFromY(-500);
		tt.setToY(0);
		tt.setToX(0);
		
		tt.play();
		
		this.rootContent.getChildren().add(this.about);
	}

}
