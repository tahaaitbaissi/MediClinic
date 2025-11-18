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

        // Chargement de l'interface principale
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/main_view.fxml"));

        primaryStage.setTitle("MediClinic - Gestion Medicale");
        primaryStage.setScene(new Scene(root, 1200, 700));
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(600);
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