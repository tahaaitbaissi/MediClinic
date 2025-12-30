package com.mediclinic.controller;

import com.mediclinic.model.RendezVous;
import com.mediclinic.service.RendezVousService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class WaitListController {
    @FXML
    private Label currentReservationLabel;
    @FXML
    private TableView<RendezVous> reservationTable;
    @FXML
    private TableColumn<RendezVous, String> colPatient;
    @FXML
    private TableColumn<RendezVous, String> colMedecin;
    @FXML
    private TableColumn<RendezVous, String> colHeure;

    private final RendezVousService rendezVousService = new RendezVousService();

    @FXML
    public void initialize() {
        colPatient.setCellValueFactory(cellData -> {
            var p = cellData.getValue().getPatient();
            return new javafx.beans.property.SimpleStringProperty(p != null ? p.getNomComplet() : "");
        });
        colMedecin.setCellValueFactory(cellData -> {
            var m = cellData.getValue().getMedecin();
            return new javafx.beans.property.SimpleStringProperty(m != null ? (m.getNom() + " " + (m.getPrenom() != null ? m.getPrenom() : "")) : "");
        });
        colHeure.setCellValueFactory(cellData -> {
            var dt = cellData.getValue().getDateHeureDebut();
            return new javafx.beans.property.SimpleStringProperty(dt != null ? dt.format(DateTimeFormatter.ofPattern("HH:mm")) : "");
        });
        refreshList();

        // Rafraîchissement automatique toutes les 10 secondes
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> refreshList()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void refreshList() {
        List<RendezVous> waitingList = rendezVousService.getWaitingRoomReservations();
        reservationTable.setItems(FXCollections.observableArrayList(waitingList));
        if (!waitingList.isEmpty()) {
            RendezVous current = waitingList.get(0);
            String info = (current.getPatient() != null ? current.getPatient().getNomComplet() : "") +
                    "\n" + (current.getDateHeureDebut() != null ? current.getDateHeureDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
            currentReservationLabel.setText(info);
        } else {
            currentReservationLabel.setText("Aucune réservation en cours");
        }
    }
}
