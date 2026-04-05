package org.example.controller.front;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import org.example.navigation.Routes;
import org.example.navigation.SceneManager;
import org.example.session.SessionManager;

public class HomeController {

    // Booking bar
    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private ComboBox<String> guestCombo;
    @FXML private ComboBox<String> roomCombo;

    // Dynamic containers
    @FXML private HBox roomsContainer;
    @FXML private HBox testimonialsContainer;

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            Platform.runLater(() -> SceneManager.navigateTo(Routes.LOGIN));
            return;
        }

        setupBookingForm();
        buildRoomCards();
        buildTestimonials();
    }

    private void setupBookingForm() {
        guestCombo.getItems().addAll("Adulte", "Enfant");
        guestCombo.setValue("Adulte");

        roomCombo.getItems().addAll("1 chambre", "2 chambres", "3 chambres");
        roomCombo.setValue("1 chambre");
    }

    // ── Room cards — mirrors .accomodation_item ───────────
    private void buildRoomCards() {
        String[][] rooms = {
                {"Chambre Double Deluxe",    "250 DT/nuit"},
                {"Chambre Simple Deluxe",    "200 DT/nuit"},
                {"Suite Lune de Miel",       "750 DT/nuit"},
                {"Chambre Double Économique","200 DT/nuit"},
        };

        for (String[] room : rooms) {
            roomsContainer.getChildren().add(buildRoomCard(room[0], room[1]));
        }
    }

    private VBox buildRoomCard(String name, String price) {
        // Image placeholder
        Rectangle img = new Rectangle(200, 140);
        img.getStyleClass().add("room-img-placeholder");
        img.setArcWidth(12);
        img.setArcHeight(12);

        Button reserveBtn = new Button("RÉSERVER");
        reserveBtn.getStyleClass().add("btn-theme");
        reserveBtn.setOnAction(e -> handleReserve(name));

        // Image + button overlay
        StackPane imgStack = new StackPane(img, reserveBtn);
        StackPane.setAlignment(reserveBtn, Pos.BOTTOM_CENTER);
        reserveBtn.setTranslateY(-10);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("room-name");
        nameLabel.setWrapText(true);

        Label priceLabel = new Label(price);
        priceLabel.getStyleClass().add("room-price");

        VBox card = new VBox(10, imgStack, nameLabel, priceLabel);
        card.getStyleClass().add("room-card");
        card.setAlignment(Pos.TOP_CENTER);
        return card;
    }

    // ── Testimonials — mirrors .testimonial_item ──────────
    private void buildTestimonials() {
        Object[][] reviews = {
                {"Fanny Spencer",  "Grâce à ce site, j'ai découvert des plages vierges et des paysages magnifiques en Tunisie.", 5},
                {"Ahmed Ben Ali",  "Une expérience incroyable de camping en montagne avec un excellent confort.", 5},
                {"Lina Trabelsi",  "Le service de réservation et le transport étaient très faciles à utiliser.", 5},
        };

        for (Object[] r : reviews) {
            testimonialsContainer.getChildren().add(
                    buildTestimonialCard((String) r[0], (String) r[1], (int) r[2])
            );
        }
    }

    private VBox buildTestimonialCard(String name, String text, int stars) {
        Label quote = new Label("\"" + text + "\"");
        quote.getStyleClass().add("testimonial-text");
        quote.setWrapText(true);

        Label author = new Label(name);
        author.getStyleClass().add("testimonial-author");

        // Star rating
        HBox starRow = new HBox(4);
        starRow.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < stars; i++) {
            Label star = new Label("★");
            star.getStyleClass().add("star-filled");
            starRow.getChildren().add(star);
        }

        VBox card = new VBox(12, quote, author, starRow);
        card.getStyleClass().add("testimonial-card");
        return card;
    }

    // ── Handlers ──────────────────────────────────────────
    @FXML
    private void handleBookNow() {
        SceneManager.navigateTo(Routes.HEBERGEMENTS);
    }

    @FXML
    private void handleCommencer() {
        SceneManager.navigateTo(Routes.HEBERGEMENTS);
    }

    @FXML
    private void handleDecouvrir() {
        SceneManager.navigateTo(Routes.ABOUT);
    }

    private void handleReserve(String roomName) {
        SceneManager.navigateTo(Routes.HEBERGEMENTS);
    }
}