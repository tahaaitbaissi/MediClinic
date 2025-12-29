package com.mediclinic.controller;

import com.mediclinic.dao.ConsultationDAO;
import com.mediclinic.model.Consultation;
import com.mediclinic.model.Role;
import com.mediclinic.util.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ConsultationsController implements Initializable {

    @FXML
    private Label contextLabel;
    @FXML
    private TextField searchField;
    @FXML
    private TableView<Consultation> consultationTable;
    @FXML
    private TableColumn<Consultation, Long> colId;
    @FXML
    private TableColumn<Consultation, String> colDate;
    @FXML
    private TableColumn<Consultation, String> colPatient;
    @FXML
    private TableColumn<Consultation, String> colMotif;
    @FXML
    private TableColumn<Consultation, Void> colActions;

    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private javafx.collections.ObservableList<Consultation> allConsultations;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupColumns();
        loadConsultations();
        setupSearch();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        colDate.setCellValueFactory(cellData -> {
            var rdv = cellData.getValue().getRendezVous();
            String date = rdv != null && rdv.getDateHeureDebut() != null
                    ? rdv.getDateHeureDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "";
            return new javafx.beans.property.SimpleStringProperty(date);
        });
        colPatient.setCellValueFactory(cellData -> {
            var rdv = cellData.getValue().getRendezVous();
            String name = rdv != null && rdv.getPatient() != null ? rdv.getPatient().getNomComplet() : "";
            return new javafx.beans.property.SimpleStringProperty(name);
        });
        colMotif.setCellValueFactory(cellData -> {
            var rdv = cellData.getValue().getRendezVous();
            String motif = rdv != null && rdv.getMotif() != null ? rdv.getMotif() : "";
            return new javafx.beans.property.SimpleStringProperty(motif);
        });
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Éditer");
            {
                editBtn.getStyleClass().addAll("btn-warning", "btn-actions");
                editBtn.setOnAction(e -> openEditor());
            }
            private void openEditor() {
                Consultation c = getTableView().getItems().get(getIndex());
                try {
                    ConsultationEditorController.openEditor(c);
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR, "Erreur: " + ex.getMessage()).showAndWait();
                }
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(5, editBtn));
                }
            }
        });
    }

    private void loadConsultations() {
        try {
            var user = UserSession.getInstance().getUser();
            Role role = user.getRole();
            if (role == Role.MEDECIN && user.getMedecin() != null) {
                contextLabel.setText("Pour: Dr. " + user.getMedecin().getNomComplet());
                List<Consultation> list = consultationDAO.findByMedecinId(user.getMedecin().getId());
                allConsultations = FXCollections.observableArrayList(list);
                consultationTable.setItems(allConsultations);
            } else if (role == Role.ADMIN) {
                contextLabel.setText("Toutes les consultations");
                allConsultations = FXCollections.observableArrayList();
                consultationTable.setItems(allConsultations);
            } else {
                contextLabel.setText("Accès limité");
                allConsultations = FXCollections.observableArrayList();
                consultationTable.setItems(allConsultations);
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur de chargement: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleRefresh() {
        loadConsultations();
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterConsultations(newValue);
            });
        }
    }

    private void filterConsultations(String searchText) {
        if (allConsultations == null) return;
        
        if (searchText == null || searchText.trim().isEmpty()) {
            consultationTable.setItems(allConsultations);
        } else {
            String search = searchText.toLowerCase();
            javafx.collections.ObservableList<Consultation> filtered = allConsultations.stream()
                .filter(c -> {
                    var rdv = c.getRendezVous();
                    if (rdv == null) return false;
                    
                    // Recherche par nom patient
                    if (rdv.getPatient() != null && 
                        rdv.getPatient().getNomComplet().toLowerCase().contains(search)) {
                        return true;
                    }
                    
                    // Recherche par motif
                    if (rdv.getMotif() != null && 
                        rdv.getMotif().toLowerCase().contains(search)) {
                        return true;
                    }
                    
                    // Recherche par observations
                    if (c.getObservations() != null && 
                        c.getObservations().toLowerCase().contains(search)) {
                        return true;
                    }
                    
                    // Recherche par diagnostic
                    if (c.getDiagnostic() != null && 
                        c.getDiagnostic().toLowerCase().contains(search)) {
                        return true;
                    }
                    
                    return false;
                })
                .collect(java.util.stream.Collectors.toCollection(FXCollections::observableArrayList));
            
            consultationTable.setItems(filtered);
        }
    }
}
