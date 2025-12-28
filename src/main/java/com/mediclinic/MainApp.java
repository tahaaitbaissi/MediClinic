package com.mediclinic;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.mediclinic.util.HibernateUtil;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialisation de Hibernate
        HibernateUtil.getSessionFactory();

        // Chargement de la page de connexion
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login_view.fxml"));

        primaryStage.setTitle("MediClinic - Connexion");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        // Fermeture propre de Hibernate a la fermeture de l'application
        HibernateUtil.shutdown();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
