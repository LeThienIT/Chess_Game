package ui.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import core.CoreConstants;
import core.Main;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;

public class StartMenuController {
	private MainController controller;

	// Tải giao diện trò chơi cờ vua chính
	private void setupController() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/root.fxml"));
			Parent root = loader.load();
			controller = loader.<MainController>getController();
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
			Main.primaryStage.setScene(scene);
			Main.primaryStage.setResizable(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Được thực thi nếu người dùng nhấn 'Play Mortimer'
	public void playMortimer() {
		setupController();
		chooseColour();
		controller.setupGame();
		controller.playGame();
	}

	private void chooseColour() {
		String choice = "";
		List<String> choices = new ArrayList<>();
		choices.add("White");
		choices.add("Black");
		choices.add("Random");

		ChoiceDialog<String> dialog = new ChoiceDialog<>("White", choices);
		dialog.setTitle("Colour Choice");
		dialog.setContentText("Choose the colour you wish to play with");
		Optional<String> result = dialog.showAndWait();

		if (result.isPresent()) {
			choice = result.get();
		} else {
			chooseColour();
		}
		switch (choice) {
		case "White":
			UIConstants.PLAYER_COLOUR = CoreConstants.WHITE;
			UIConstants.AI_COLOUR = CoreConstants.BLACK;
			break;
		case "Black":
			UIConstants.PLAYER_COLOUR = CoreConstants.BLACK;
			UIConstants.AI_COLOUR = CoreConstants.WHITE;
			break;
		case "Random":
			Random random = new Random();
			int side = random.nextInt(2);
			UIConstants.PLAYER_COLOUR = side;
			UIConstants.AI_COLOUR = (side == 0) ? 1 : 0;
			break;
		}
	}

	public void playHuman() {
		setupController();
		controller.setupGame();
		UIConstants.PLAYING_AI = false;
		controller.playGame();
	}

}
