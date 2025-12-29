package com.mediclinic.controller;

import com.mediclinic.model.Medecin;
import com.mediclinic.service.MedecinService;
import com.mediclinic.service.SignatureService;
import com.mediclinic.util.UserSession;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Controller for doctor profile and signature management
 */
public class DoctorProfileController {

    @FXML
    private Label doctorNameLabel;

    @FXML
    private Label doctorSpecialtyLabel;

    @FXML
    private Label doctorEmailLabel;

    @FXML
    private Label doctorPhoneLabel;

    @FXML
    private Canvas signatureCanvas;

    @FXML
    private Button clearButton;

    @FXML
    private Button saveSignatureButton;

    @FXML
    private Button deleteSignatureButton;

    @FXML
    private ImageView currentSignatureView;

    @FXML
    private VBox currentSignatureBox;

    @FXML
    private Label signatureStatusLabel;

    private SignatureService signatureService;
    private MedecinService medecinService;
    private Medecin currentMedecin;
    private GraphicsContext gc;
    private boolean isDrawing = false;
    private boolean hasDrawn = false;

    public DoctorProfileController() {
        this.signatureService = new SignatureService();
        this.medecinService = new MedecinService();
    }

    @FXML
    public void initialize() {
        System.out.println(
            "DoctorProfileController initialized - setting up signature canvas"
        );

        // Load current doctor from session
        loadCurrentDoctor();

        // Setup canvas for drawing
        setupSignatureCanvas();

        // Load existing signature if available
        loadExistingSignature();
    }

    /**
     * Loads the current doctor from the session
     */
    private void loadCurrentDoctor() {
        if (!UserSession.isLoggedIn()) {
            showAlert(
                "Erreur",
                "Aucun utilisateur connecté",
                Alert.AlertType.ERROR
            );
            return;
        }

        Long medecinId = UserSession.getMedecinId();
        if (medecinId == null) {
            showAlert(
                "Erreur",
                "Vous n'êtes pas associé à un médecin",
                Alert.AlertType.ERROR
            );
            return;
        }

        try {
            currentMedecin = medecinService.getMedecinById(medecinId);
            if (currentMedecin != null) {
                displayDoctorInfo();
            } else {
                showAlert(
                    "Erreur",
                    "Impossible de charger les informations du médecin",
                    Alert.AlertType.ERROR
                );
            }
        } catch (Exception e) {
            System.err.println("Error loading doctor: " + e.getMessage());
            e.printStackTrace();
            showAlert(
                "Erreur",
                "Erreur lors du chargement des informations: " +
                    e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    /**
     * Displays doctor information in the UI
     */
    private void displayDoctorInfo() {
        if (currentMedecin == null) return;

        doctorNameLabel.setText(
            "Dr. " +
                currentMedecin.getPrenom() +
                " " +
                currentMedecin.getNom().toUpperCase()
        );
        doctorSpecialtyLabel.setText(
            "Spécialité: " + currentMedecin.getSpecialite().toString()
        );
        doctorEmailLabel.setText(
            "Email: " +
                (currentMedecin.getEmail() != null
                    ? currentMedecin.getEmail()
                    : "Non renseigné")
        );
        doctorPhoneLabel.setText(
            "Téléphone: " +
                (currentMedecin.getTelephone() != null
                    ? currentMedecin.getTelephone()
                    : "Non renseigné")
        );
    }

    /**
     * Sets up the signature canvas for drawing
     */
    private void setupSignatureCanvas() {
        gc = signatureCanvas.getGraphicsContext2D();

        // Set drawing style
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);

        // Fill with white background
        gc.setFill(Color.WHITE);
        gc.fillRect(
            0,
            0,
            signatureCanvas.getWidth(),
            signatureCanvas.getHeight()
        );

        // Mouse press: start drawing
        signatureCanvas.setOnMousePressed(this::handleMousePressed);

        // Mouse drag: continue drawing
        signatureCanvas.setOnMouseDragged(this::handleMouseDragged);

        // Mouse release: stop drawing
        signatureCanvas.setOnMouseReleased(this::handleMouseReleased);

        System.out.println("Signature canvas setup complete");
    }

    /**
     * Handles mouse press event on canvas
     */
    private void handleMousePressed(MouseEvent event) {
        gc.beginPath();
        gc.moveTo(event.getX(), event.getY());
        gc.stroke();
        isDrawing = true;
        hasDrawn = true;
    }

    /**
     * Handles mouse drag event on canvas
     */
    private void handleMouseDragged(MouseEvent event) {
        if (isDrawing) {
            gc.lineTo(event.getX(), event.getY());
            gc.stroke();
        }
    }

    /**
     * Handles mouse release event on canvas
     */
    private void handleMouseReleased(MouseEvent event) {
        isDrawing = false;
    }

    /**
     * Clears the signature canvas
     */
    @FXML
    private void handleClearCanvas() {
        gc.setFill(Color.WHITE);
        gc.fillRect(
            0,
            0,
            signatureCanvas.getWidth(),
            signatureCanvas.getHeight()
        );
        hasDrawn = false;
        System.out.println("Canvas cleared");
    }

    /**
     * Saves the signature
     */
    @FXML
    private void handleSaveSignature() {
        if (currentMedecin == null) {
            showAlert(
                "Erreur",
                "Impossible de sauvegarder: médecin non identifié",
                Alert.AlertType.ERROR
            );
            return;
        }

        if (!hasDrawn) {
            showAlert(
                "Attention",
                "Veuillez dessiner votre signature avant de sauvegarder",
                Alert.AlertType.WARNING
            );
            return;
        }

        try {
            // Capture canvas as image
            WritableImage signature = new WritableImage(
                (int) signatureCanvas.getWidth(),
                (int) signatureCanvas.getHeight()
            );
            signatureCanvas.snapshot(null, signature);

            // Validate signature
            if (!signatureService.validateSignature(signature)) {
                showAlert(
                    "Erreur",
                    "La signature est invalide (trop petite ou trop grande)",
                    Alert.AlertType.ERROR
                );
                return;
            }

            // Save signature
            boolean saved = signatureService.saveSignature(
                currentMedecin.getId(),
                signature
            );

            if (saved) {
                showAlert(
                    "Succès",
                    "Signature enregistrée avec succès!",
                    Alert.AlertType.INFORMATION
                );

                // Reload existing signature to show in preview
                loadExistingSignature();

                // Clear canvas after successful save
                handleClearCanvas();
            } else {
                showAlert(
                    "Erreur",
                    "Échec de l'enregistrement de la signature",
                    Alert.AlertType.ERROR
                );
            }
        } catch (Exception e) {
            System.err.println("Error saving signature: " + e.getMessage());
            e.printStackTrace();
            showAlert(
                "Erreur",
                "Erreur lors de l'enregistrement: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    /**
     * Deletes the current signature
     */
    @FXML
    private void handleDeleteSignature() {
        if (currentMedecin == null) {
            return;
        }

        if (!signatureService.hasSignature(currentMedecin.getId())) {
            showAlert(
                "Information",
                "Aucune signature à supprimer",
                Alert.AlertType.INFORMATION
            );
            return;
        }

        // Confirm deletion
        Alert confirmAlert = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Êtes-vous sûr de vouloir supprimer votre signature électronique?",
            ButtonType.YES,
            ButtonType.NO
        );
        confirmAlert.setTitle("Confirmation");
        confirmAlert.setHeaderText("Supprimer la signature");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                boolean deleted = signatureService.deleteSignature(
                    currentMedecin.getId()
                );

                if (deleted) {
                    showAlert(
                        "Succès",
                        "Signature supprimée avec succès",
                        Alert.AlertType.INFORMATION
                    );

                    // Hide current signature preview
                    currentSignatureBox.setVisible(false);
                    currentSignatureBox.setManaged(false);
                    updateSignatureStatus(false);
                } else {
                    showAlert(
                        "Erreur",
                        "Échec de la suppression de la signature",
                        Alert.AlertType.ERROR
                    );
                }
            }
        });
    }

    /**
     * Loads and displays existing signature if available
     */
    private void loadExistingSignature() {
        if (currentMedecin == null) {
            return;
        }

        boolean hasSignature = signatureService.hasSignature(
            currentMedecin.getId()
        );

        if (hasSignature) {
            Image signature = signatureService.loadSignature(
                currentMedecin.getId()
            );

            if (signature != null) {
                currentSignatureView.setImage(signature);
                currentSignatureBox.setVisible(true);
                currentSignatureBox.setManaged(true);
                deleteSignatureButton.setDisable(false);
                updateSignatureStatus(true);

                System.out.println(
                    "Loaded existing signature for medecin " +
                        currentMedecin.getId()
                );
            }
        } else {
            currentSignatureBox.setVisible(false);
            currentSignatureBox.setManaged(false);
            deleteSignatureButton.setDisable(true);
            updateSignatureStatus(false);

            System.out.println(
                "No existing signature found for medecin " +
                    currentMedecin.getId()
            );
        }
    }

    /**
     * Updates the signature status label
     */
    private void updateSignatureStatus(boolean hasSignature) {
        if (hasSignature) {
            signatureStatusLabel.setText(
                "✅ Signature électronique enregistrée"
            );
            signatureStatusLabel.setStyle("-fx-text-fill: #27ae60;");
        } else {
            signatureStatusLabel.setText(
                "❌ Aucune signature enregistrée"
            );
            signatureStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    /**
     * Closes the profile window
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) signatureCanvas.getScene().getWindow();
        stage.close();
    }

    /**
     * Shows an alert dialog
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
