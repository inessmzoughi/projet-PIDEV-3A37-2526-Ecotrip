package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.database.Base;
import org.example.navigation.Routes;
import org.example.navigation.SceneManager;

import java.sql.Connection;

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