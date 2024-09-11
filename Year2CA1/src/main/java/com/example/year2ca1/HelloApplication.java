package com.example.year2ca1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;

public class HelloApplication extends Application {

    private static Stage stage1;

    //Start begins the program, and boots the main menu controller before any other scene
    @Override
    public void start(Stage stage1) throws IOException {
        HelloApplication.stage1 = stage1;
        changeScene("MainMenuController.fxml");
    }

    //changeScene swaps and reloads the scene with another to cycle them.
    public static void changeScene(String sceneSwap) throws IOException {
        Parent screen = FXMLLoader.load(
                Objects.requireNonNull(HelloApplication.class.getResource(sceneSwap)));

        Scene scene = new Scene(screen);
        stage1.setScene(scene);
        stage1.show();
    }
}