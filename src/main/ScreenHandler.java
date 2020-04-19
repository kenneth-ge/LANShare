package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class ScreenHandler {

	public VBox setupInfo(AnchorPane rootContent) {
		VBox root = new VBox(10);
		root.setAlignment(Pos.CENTER);
		root.setBackground(Background.EMPTY);
		
		Text title = new Text("Help and Info");
		title.setTextAlignment(TextAlignment.CENTER);
		title.setFont(Font.font("Verdana", 24));
		title.setWrappingWidth(450);
		
		Text body = new Text(readFile("info.txt"));
		body.setLineSpacing(1.7);
		body.setWrappingWidth(450);
		
		VBox g = new VBox(title, body);
		
		ScrollPane pane = new ScrollPane(g);
		pane.setHbarPolicy(ScrollBarPolicy.NEVER);
		
		pane.setMaxWidth(477);
		pane.prefHeightProperty().bind(rootContent.heightProperty());
		
		root.getChildren().add(pane);
		root.setMaxSize(477, Double.POSITIVE_INFINITY);
		root.prefWidthProperty().bind(rootContent.widthProperty());
		root.prefHeightProperty().bind(rootContent.heightProperty());
		
		return root;
	}
	
	public VBox setupAbout(AnchorPane rootContent) {
		VBox root = new VBox(10);
		root.setAlignment(Pos.CENTER);
		root.setBackground(Background.EMPTY);
		
		Text title = new Text("About");
		title.setTextAlignment(TextAlignment.CENTER);
		title.setFont(Font.font("Verdana", 24));
		title.setWrappingWidth(450);
		
		Text body = new Text(readFile("about.txt"));
		body.setFont(Font.font("Calibri", 15));
		body.setTextAlignment(TextAlignment.CENTER);
		body.setLineSpacing(2);
		body.setWrappingWidth(450);
		
		VBox g = new VBox(title, body);
		
		ScrollPane pane = new ScrollPane(g);
		pane.setHbarPolicy(ScrollBarPolicy.NEVER);
		
		pane.setMaxWidth(477);
		pane.prefHeightProperty().bind(rootContent.heightProperty());
		
		root.getChildren().add(pane);
		root.setMaxSize(477, Double.POSITIVE_INFINITY);
		root.prefWidthProperty().bind(rootContent.widthProperty());
		root.prefHeightProperty().bind(rootContent.heightProperty());
		
		return root;
	}
	
	public String readFile(String location) {
		StringBuffer s = new StringBuffer();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(location)));
			
			String line;
			
			while((line = br.readLine()) != null) {
				s.append(line);
				s.append('\n');
			}
			
			br.close();
		}catch(IOException e) {
			e.printStackTrace();
			return "Hmm... something went wrong";
		}
		
		return s.toString();
	}
	
}
