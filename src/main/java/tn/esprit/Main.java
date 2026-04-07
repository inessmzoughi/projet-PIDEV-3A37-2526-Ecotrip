package tn.esprit;

import javafx.application.Application;
import javafx.stage.Stage;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        SceneManager.initialize(primaryStage);
        primaryStage.setTitle("EcoTrip");
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(700);

        SceneManager.navigateTo(Routes.LOGIN); // ← just this

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}