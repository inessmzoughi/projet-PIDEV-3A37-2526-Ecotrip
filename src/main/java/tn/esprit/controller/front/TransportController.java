package tn.esprit.controller.front;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.models.transport.Transport;
import tn.esprit.services.transport.TransportService;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class TransportController implements Initializable {

    private static final String UPLOADS_DIR = "uploads/transports/";

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> availabilityCombo;
    @FXML private FlowPane cardsPane;
    @FXML private Label resultCount;
    @FXML private VBox emptyState;

    private final TransportService service = new TransportService();
    private List<Transport> allData = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sortCombo.getItems().addAll("Type (A-Z)", "Prix croissant", "Capacite croissante");
        sortCombo.getSelectionModel().selectFirst();
        availabilityCombo.getItems().addAll("Tous", "Disponibles", "Indisponibles");
        availabilityCombo.getSelectionModel().selectFirst();
        loadData();
        searchField.setOnKeyReleased(e -> applyFilters());
        sortCombo.setOnAction(e -> applyFilters());
        availabilityCombo.setOnAction(e -> applyFilters());
    }

    private void loadData() {
        try {
            allData = service.afficherAll();
        } catch (SQLException e) {
            allData = new ArrayList<>();
        }
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String availability = availabilityCombo.getValue();

        List<Transport> filtered = allData.stream()
                .filter(t -> query.isEmpty()
                        || t.getType().toLowerCase().contains(query)
                        || (t.getCategory() != null && t.getCategory().getName().toLowerCase().contains(query))
                        || (t.getChauffeur() != null && t.getChauffeur().getFullName().toLowerCase().contains(query)))
                .filter(t -> "Tous".equals(availability)
                        || ("Disponibles".equals(availability) && t.isDisponible())
                        || ("Indisponibles".equals(availability) && !t.isDisponible()))
                .collect(Collectors.toList());

        String sort = sortCombo.getValue();
        if ("Type (A-Z)".equals(sort)) filtered.sort(Comparator.comparing(Transport::getType, String.CASE_INSENSITIVE_ORDER));
        else if ("Prix croissant".equals(sort)) filtered.sort(Comparator.comparingDouble(Transport::getPrixParPersonne));
        else if ("Capacite croissante".equals(sort)) filtered.sort(Comparator.comparingInt(Transport::getCapacite));

        renderCards(filtered);
    }

    private void renderCards(List<Transport> transports) {
        cardsPane.getChildren().clear();
        resultCount.setText(transports.size() + " transport(s) trouve(s)");
        boolean empty = transports.isEmpty();
        emptyState.setManaged(empty);
        emptyState.setVisible(empty);
        cardsPane.setManaged(!empty);
        cardsPane.setVisible(!empty);

        for (Transport transport : transports) {
            cardsPane.getChildren().add(buildCard(transport));
        }
    }

    private VBox buildCard(Transport transport) {
        VBox card = new VBox(0);
        card.getStyleClass().add("heb-card");
        card.setPrefWidth(320);

        card.getChildren().add(buildImageZone(transport));

        VBox body = new VBox(12);
        body.setPadding(new Insets(18));

        Label badge = new Label(transport.isDisponible() ? "Disponible" : "Indisponible");
        badge.getStyleClass().add(transport.isDisponible() ? "badge-actif" : "badge-inactif");

        Label title = new Label(transport.getType());
        title.getStyleClass().add("heb-card-nom");

        Label category = new Label(transport.getCategory() == null ? "Sans categorie" : transport.getCategory().getName());
        category.getStyleClass().add("heb-card-category");

        Label chauffeur = new Label("Chauffeur: " + (transport.getChauffeur() == null ? "Non assigne" : transport.getChauffeur().getFullName()));
        chauffeur.getStyleClass().add("heb-card-ville");

        HBox meta = new HBox(12);
        Label capacite = new Label("Capacite: " + transport.getCapacite());
        capacite.getStyleClass().add("heb-card-equipement");
        Label emission = new Label("CO2: " + transport.getEmissionCo2());
        emission.getStyleClass().add("heb-card-equipement");
        meta.getChildren().addAll(capacite, emission);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox priceRow = new HBox(10);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label(String.format("%.2f DT / personne", transport.getPrixParPersonne()));
        price.getStyleClass().add("heb-card-price-amount");
        Button action = new Button(transport.isDisponible() ? "Reserver" : "Indisponible");
        action.getStyleClass().add("heb-card-btn");
        action.setDisable(!transport.isDisponible());
        priceRow.getChildren().addAll(price, spacer, action);

        body.getChildren().addAll(badge, title, category, chauffeur, meta, priceRow);
        card.getChildren().add(body);
        return card;
    }

    private StackPane buildImageZone(Transport transport) {
        VBox imgBox = new VBox();
        imgBox.getStyleClass().add("heb-card-img");
        imgBox.setPrefHeight(200);
        imgBox.setMinHeight(200);
        imgBox.setMaxHeight(200);
        imgBox.setAlignment(Pos.CENTER);

        Image image = loadImage(transport.getImage());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(320);
            imageView.setFitHeight(200);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            imgBox.getChildren().add(imageView);
        } else {
            Label fallback = new Label("🚌");
            fallback.setStyle("-fx-font-size: 52;");
            imgBox.getChildren().add(fallback);
        }

        return new StackPane(imgBox);
    }

    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        try {
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                Image image = new Image(imagePath, 320, 200, false, true, true);
                return image.isError() ? null : image;
            }

            File absoluteFile = new File(imagePath);
            if (absoluteFile.exists()) {
                return new Image(absoluteFile.toURI().toString(), 320, 200, false, true);
            }

            String fileName = new File(imagePath).getName();
            File uploadFile = new File(UPLOADS_DIR + fileName);
            if (uploadFile.exists()) {
                return new Image(uploadFile.toURI().toString(), 320, 200, false, true);
            }

            URL resource = getClass().getResource("/images/" + fileName);
            if (resource != null) {
                return new Image(resource.toExternalForm(), 320, 200, false, true);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @FXML
    private void onReset() {
        searchField.clear();
        sortCombo.getSelectionModel().selectFirst();
        availabilityCombo.getSelectionModel().selectFirst();
        applyFilters();
    }
}
