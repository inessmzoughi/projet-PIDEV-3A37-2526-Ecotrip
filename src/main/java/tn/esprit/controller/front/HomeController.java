package tn.esprit.controller.front;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import tn.esprit.models.Hebergement;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.Hebergement_service;
import tn.esprit.session.SessionManager;

import java.sql.SQLException;
import java.util.List;

public class HomeController {

    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;
    @FXML private ComboBox<String> guestCombo;
    @FXML private ComboBox<String> roomCombo;
    @FXML private HBox roomsContainer;
    @FXML private HBox testimonialsContainer;
    @FXML private VBox bannerArea;

    private final Hebergement_service service = new Hebergement_service();

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            Platform.runLater(() -> SceneManager.navigateTo(Routes.LOGIN));
            return;
        }

        Platform.runLater(() -> {
            try {
                var stream = getClass().getResourceAsStream("/images/img.png");
                if (stream != null) {
                    Image bg = new Image(stream);
                    BackgroundFill overlayFill = new BackgroundFill(
                            Color.rgb(0, 0, 0, 0.72),
                            null, null
                    );
                    BackgroundImage bgImg = new BackgroundImage(
                            bg,
                            BackgroundRepeat.NO_REPEAT,
                            BackgroundRepeat.NO_REPEAT,
                            BackgroundPosition.CENTER,
                            new BackgroundSize(
                                    BackgroundSize.AUTO, BackgroundSize.AUTO,
                                    false, false, true, true  // cover = plein écran
                            )
                    );
                    bannerArea.setBackground(new Background(
                            new BackgroundFill[]{overlayFill},
                            new BackgroundImage[]{bgImg}
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

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

    // ── Cartes hébergements 5 étoiles depuis DB ───────────
    private void buildRoomCards() {
        try {
            List<Hebergement> hebs = service.getTop5Etoiles();
            for (Hebergement h : hebs) {
                roomsContainer.getChildren().add(buildRoomCard(h));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox buildRoomCard(Hebergement h) {
        StackPane imgStack = buildImageStack(h);

        // Étoiles
        HBox stars = new HBox(2);
        stars.setAlignment(Pos.CENTER_LEFT);
        stars.setStyle("-fx-padding: 10 14 0 14;");
        for (int i = 0; i < h.getNb_etoiles(); i++) {
            Label s = new Label("★");
            s.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 15px;");
            stars.getChildren().add(s);
        }

        Label nameLabel = new Label(h.getNom());
        nameLabel.getStyleClass().add("room-name");
        nameLabel.setWrapText(true);

        Label villeLabel = new Label("📍 " + h.getVille());
        villeLabel.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-padding: 2 14 0 14;"
        );

        Button reserveBtn = new Button("Voir détails →");
        reserveBtn.getStyleClass().add("btn-theme");
        reserveBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(reserveBtn, new Insets(8, 14, 14, 14));
        reserveBtn.setOnAction(e -> SceneManager.navigateTo(Routes.HEBERGEMENTS));

        VBox card = new VBox(0, imgStack, stars, nameLabel, villeLabel, reserveBtn);
        card.getStyleClass().add("room-card");
        card.setAlignment(Pos.TOP_LEFT);
        return card;
    }

    private StackPane buildImageStack(Hebergement h) {
        Pane imgPane = null;

        String src = h.getImage_principale();

        if (src != null && !src.isEmpty()) {
            try {
                Image img;
                if (src.startsWith("http")) {
                    img = new Image(src, 260, 180, false, true, true);
                } else if (src.startsWith("uploads/")) {
                    var stream = getClass().getResourceAsStream("/" + src);
                    if (stream == null) throw new Exception("stream null");
                    img = new Image(stream, 260, 180, false, true);
                } else {
                    var stream = getClass().getResourceAsStream("/images/" + src);
                    if (stream == null) throw new Exception("stream null");
                    img = new Image(stream, 260, 180, false, true);
                }

                if (img.isError()) throw new Exception("image error");

                ImageView iv = new ImageView(img);
                iv.setFitWidth(260);
                iv.setFitHeight(180);
                iv.setPreserveRatio(false);

                Rectangle clip = new Rectangle(260, 180);
                clip.setArcWidth(18);
                clip.setArcHeight(18);
                iv.setClip(clip);

                Pane p = new Pane(iv);
                p.setPrefSize(260, 180);
                imgPane = p;

            } catch (Exception ex) {
                imgPane = null;
            }
        }

        if (imgPane == null) {
            Pane placeholder = new Pane();
            placeholder.setPrefSize(260, 180);
            placeholder.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #1a3a2a, #2d5016);" +
                            "-fx-background-radius: 18 18 0 0;"
            );

            Label icon = new Label("\uD83C\uDFE8");
            icon.setStyle("-fx-font-size: 52px;");
            icon.setLayoutX(95);
            icon.setLayoutY(45);

            String nom = h.getNom() != null ? h.getNom() : "";
            Label nomLabel = new Label(nom);
            nomLabel.setStyle(
                    "-fx-font-size: 12px;" +
                            "-fx-text-fill: rgba(255,255,255,0.75);" +
                            "-fx-font-weight: bold;"
            );
            nomLabel.setLayoutX(20);
            nomLabel.setLayoutY(145);
            nomLabel.setMaxWidth(220);
            nomLabel.setWrapText(true);

            placeholder.getChildren().addAll(icon, nomLabel);
            imgPane = placeholder;
        }

        // Badge ⭐ 5 Étoiles
        Label badge = new Label("\u2B50 5 \u00C9toiles");
        badge.setStyle(
                "-fx-background-color: rgba(30,70,32,0.92);" +
                        "-fx-background-radius: 20;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 5 14 5 14;"
        );

        StackPane stack = new StackPane(imgPane, badge);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        badge.setTranslateX(-10);
        badge.setTranslateY(10);
        return stack;
    }

    // ── Testimonials ──────────────────────────────────────
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

    @FXML private void handleBookNow()   { SceneManager.navigateTo(Routes.HEBERGEMENTS); }
    @FXML private void handleCommencer() { SceneManager.navigateTo(Routes.HEBERGEMENTS); }
    @FXML private void handleDecouvrir() { SceneManager.navigateTo(Routes.ABOUT); }
}