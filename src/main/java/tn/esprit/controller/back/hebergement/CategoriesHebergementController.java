package tn.esprit.controller.back.hebergement;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.esprit.models.Categorie_hebergement;
import tn.esprit.database.Base;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.hebergement.CategorieH_service;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CategoriesHebergementController implements Initializable {

    /* ─── FXML ─── */
    @FXML private Label     statTotal;
    @FXML private Label     statHeb;
    @FXML private Label     statTop;
    @FXML private Label     formTitle;
    @FXML private Label     formPreviewIcon;
    @FXML private TextField nomField;
    @FXML private TextArea  descField;
    @FXML private Label     errNom;
    @FXML private TextField searchField;
    @FXML private Label     badgeCount;
    @FXML private FlowPane  catCardsPane;
    @FXML private VBox      emptyState;
    @FXML private FlowPane  iconGrid;
    @FXML private FlowPane  colorGrid;

    /* ─── Palettes ─── */
    private static final String[] ICONS = {
            "🏨", "🏡", "🏩", "🛖", "⛺", "🏰",
            "🌿", "🌴", "🏔️", "🌊", "🚐", "🏕️"
    };
    private static final String[] COLORS = {
            "#3b82f6", "#38a169", "#8b5cf6", "#f59e0b",
            "#ef4444", "#0d9488", "#ec4899", "#6366f1",
            "#14b8a6", "#f97316", "#84cc16", "#06b6d4"
    };

    /* ─── État ─── */
    private final CategorieH_service service = new CategorieH_service();
    private List<Categorie_hebergement> allData;
    private Integer editingId     = null;
    private String  selectedIcon  = ICONS[0];
    private String  selectedColor = COLORS[0];

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        buildIconPicker();
        buildColorPicker();
        loadData();
    }

    /* ══════════════════════════════════════════
       PICKERS
       ══════════════════════════════════════════ */
    private void buildIconPicker() {
        iconGrid.getChildren().clear();
        for (String icon : ICONS) {
            Button btn = new Button(icon);
            btn.setStyle("-fx-font-size:20; -fx-background-radius:8; -fx-cursor:hand;"
                    + "-fx-background-color:transparent; -fx-border-radius:8;"
                    + "-fx-border-color:transparent; -fx-border-width:2;");
            if (icon.equals(selectedIcon)) highlightIconBtn(btn);
            btn.setOnAction(e -> {
                selectedIcon = icon;
                formPreviewIcon.setText(icon);
                iconGrid.getChildren().forEach(n -> {
                    if (n instanceof Button b)
                        b.setStyle("-fx-font-size:20; -fx-background-radius:8;"
                                + "-fx-cursor:hand; -fx-background-color:transparent;"
                                + "-fx-border-radius:8; -fx-border-color:transparent;"
                                + "-fx-border-width:2;");
                });
                highlightIconBtn(btn);
            });
            iconGrid.getChildren().add(btn);
        }
    }

    private void highlightIconBtn(Button btn) {
        btn.setStyle("-fx-font-size:20; -fx-background-radius:8; -fx-cursor:hand;"
                + "-fx-background-color:" + selectedColor + "33;"
                + "-fx-border-color:" + selectedColor + ";"
                + "-fx-border-radius:8; -fx-border-width:2;");
    }

    private void buildColorPicker() {
        colorGrid.getChildren().clear();
        for (String color : COLORS) {
            Button btn = new Button();
            btn.setPrefSize(28, 28);
            btn.setUserData(color);
            btn.setStyle("-fx-background-color:" + color
                    + "; -fx-background-radius:50; -fx-cursor:hand;"
                    + "-fx-border-color:transparent; -fx-border-width:2;"
                    + "-fx-border-radius:50;");
            if (color.equals(selectedColor)) highlightColorBtn(btn, color);
            btn.setOnAction(e -> {
                selectedColor = color;
                colorGrid.getChildren().forEach(n -> {
                    if (n instanceof Button b && b.getUserData() instanceof String c)
                        b.setStyle("-fx-background-color:" + c
                                + "; -fx-background-radius:50; -fx-cursor:hand;"
                                + "-fx-border-color:transparent; -fx-border-width:2;"
                                + "-fx-border-radius:50;");
                });
                highlightColorBtn(btn, color);
                buildIconPicker();
                iconGrid.getChildren().stream()
                        .filter(n -> n instanceof Button)
                        .map(n -> (Button) n)
                        .filter(b -> b.getText().equals(selectedIcon))
                        .findFirst()
                        .ifPresent(this::highlightIconBtn);
            });
            colorGrid.getChildren().add(btn);
        }
    }

    private void highlightColorBtn(Button btn, String color) {
        btn.setStyle("-fx-background-color:" + color
                + "; -fx-background-radius:50; -fx-cursor:hand;"
                + "-fx-border-color:white; -fx-border-width:2;"
                + "-fx-border-radius:50;"
                + "-fx-effect:dropshadow(gaussian," + color + ",6,0.6,0,0);");
    }

    /* ══════════════════════════════════════════
       DONNÉES
       ══════════════════════════════════════════ */
    private void loadData() {
        try {
            allData = service.getAll();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            allData = List.of();
        }
        renderCards();
        updateStats();
    }

    /* ══════════════════════════════════════════
       STATS — CORRIGÉES ✅
       ══════════════════════════════════════════ */
    private void updateStats() {
        // 1. Total catégories
        statTotal.setText(String.valueOf(allData.size()));

        // 2. Hébergements classés = hébergements qui ont un categorie_id valide
        try {
            Connection conn = Base.getInstance().getConnection();
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT COUNT(DISTINCT h.id) AS nb " +
                            "FROM hebergement h " +
                            "INNER JOIN categorie_hebergement ch ON ch.id = h.categorie_id");
            if (rs.next()) {
                statHeb.setText(String.valueOf(rs.getInt("nb")));
            } else {
                statHeb.setText("0");
            }
        } catch (SQLException e) {
            statHeb.setText("—");
        }

        // 3. Catégorie la plus utilisée
        try {
            Connection conn = Base.getInstance().getConnection();
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT ch.nom, COUNT(h.id) AS nb " +
                            "FROM categorie_hebergement ch " +
                            "LEFT JOIN hebergement h ON h.categorie_id = ch.id " +
                            "GROUP BY ch.id, ch.nom " +
                            "ORDER BY nb DESC " +
                            "LIMIT 1");
            if (rs.next()) {
                statTop.setText(rs.getString("nom"));
            } else {
                statTop.setText("—");
            }
        } catch (SQLException e) {
            statTop.setText("—");
        }
    }

    /* ══════════════════════════════════════════
       FORMULAIRE
       ══════════════════════════════════════════ */
    @FXML
    private void onNomChanged() {
        if (!nomField.getText().trim().isEmpty()) {
            errNom.setVisible(false);
            errNom.setManaged(false);
        }
    }

    @FXML
    private void onReset() {
        editingId     = null;
        selectedIcon  = ICONS[0];
        selectedColor = COLORS[0];
        formTitle.setText("Nouvelle Catégorie");
        formPreviewIcon.setText(selectedIcon);
        nomField.clear();
        descField.clear();
        errNom.setVisible(false);
        errNom.setManaged(false);
        buildIconPicker();
        buildColorPicker();
    }

    @FXML
    private void AjouterCategorie() {
        String nom  = nomField.getText().trim();
        String desc = descField.getText().trim();

        if (nom.isEmpty()) {
            errNom.setVisible(true);
            errNom.setManaged(true);
            return;
        }

        try {
            if (editingId != null) {
                service.modifier(new Categorie_hebergement(editingId, nom, desc));
            } else {
                service.ajouter(new Categorie_hebergement(0, nom, desc));
            }
            onReset();
            loadData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        }
    }

    /* ══════════════════════════════════════════
       RENDU CARTES
       ══════════════════════════════════════════ */
    @FXML
    private void onSearch() { renderCards(); }

    private void renderCards() {
        String q = searchField.getText().toLowerCase().trim();

        List<Categorie_hebergement> filtered = allData.stream()
                .filter(c -> q.isEmpty()
                        || c.getNom().toLowerCase().contains(q)
                        || (c.getDescription() != null
                        && c.getDescription().toLowerCase().contains(q)))
                .collect(Collectors.toList());

        badgeCount.setText(String.valueOf(filtered.size()));
        catCardsPane.getChildren().clear();

        if (filtered.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            return;
        }
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        for (int i = 0; i < filtered.size(); i++)
            catCardsPane.getChildren().add(buildCatCard(filtered.get(i), i));
    }

    private VBox buildCatCard(Categorie_hebergement c, int index) {
        String color = COLORS[index % COLORS.length];
        String icon  = ICONS[index % ICONS.length];

        VBox card = new VBox(10);
        card.getStyleClass().add("cat-card");
        card.setPrefWidth(250);
        card.setPadding(new Insets(18, 20, 16, 20));
        card.setStyle("-fx-border-color:" + color
                + " transparent transparent transparent;"
                + "-fx-border-width:0 0 0 4;");

        // Icône + Nom
        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-background-color:" + color + "33;"
                + "-fx-background-radius:12; -fx-padding:6; -fx-font-size:22;");

        Label nameLbl = new Label(c.getNom());
        nameLbl.setStyle("-fx-font-weight:bold; -fx-font-size:14;");
        top.getChildren().addAll(iconLbl, nameLbl);

        // Description
        String descText = (c.getDescription() == null || c.getDescription().isEmpty())
                ? "Aucune description." : c.getDescription();
        Label desc = new Label(descText);
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill:#6b7280; -fx-font-size:12;");

        // ✅ Nb hébergements dans cette catégorie
        int nbHebs = countHebergementsByCategorie(c.getId());
        Label hebCount = new Label("🏨 " + nbHebs + " hébergement(s)");
        hebCount.setStyle("-fx-font-size:11; -fx-text-fill:" + color + "; -fx-font-weight:bold;");

        // Boutons
        HBox meta = new HBox(6);
        meta.setAlignment(Pos.CENTER_RIGHT);
        Button editBtn = new Button("✏️ Modifier");
        editBtn.getStyleClass().add("btn-edit");
        Button delBtn = new Button("🗑️");
        delBtn.getStyleClass().add("btn-del");
        editBtn.setOnAction(e -> loadEdit(c.getId()));
        delBtn.setOnAction(e -> confirmDelete(c));
        meta.getChildren().addAll(editBtn, delBtn);

        card.getChildren().addAll(top, desc, hebCount, meta);
        return card;
    }

    /* ✅ Compte les hébergements d'une catégorie spécifique */
    private int countHebergementsByCategorie(int categorieId) {
        try {
            Connection conn = Base.getInstance().getConnection();
            java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM hebergement WHERE categorie_id = ?");
            ps.setInt(1, categorieId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /* ══════════════════════════════════════════
       ÉDITION / SUPPRESSION
       ══════════════════════════════════════════ */
    private void loadEdit(int id) {
        allData.stream().filter(c -> c.getId() == id).findFirst().ifPresent(c -> {
            editingId = id;
            formTitle.setText("Modifier Catégorie");
            formPreviewIcon.setText(selectedIcon);
            nomField.setText(c.getNom());
            descField.setText(c.getDescription() != null ? c.getDescription() : "");
        });
    }

    private void confirmDelete(Categorie_hebergement c) {
        // ✅ Vérifie si des hébergements utilisent cette catégorie
        int nb = countHebergementsByCategorie(c.getId());
        if (nb > 0) {
            showAlert(Alert.AlertType.WARNING,
                    "Suppression impossible",
                    "Cette catégorie est utilisée par " + nb
                            + " hébergement(s).\nVeuillez d'abord réassigner ces hébergements.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer ?");
        alert.setHeaderText("🗑️  Supprimer « " + c.getNom() + " » ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(r -> r == confirm).ifPresent(r -> {
            try {
                service.supprimer(c.getId());
                if (editingId != null && editingId == c.getId()) onReset();
                loadData();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
            }
        });
    }

    /* ══════════════════════════════════════════
       NAVIGATION
       ══════════════════════════════════════════ */
    @FXML private void onNavHebergements() { SceneManager.navigateTo(Routes.ADMIN_HEBERGEMENTS); }
    @FXML private void onNavChambres()     { navigateTo("Chambres.fxml",         "Chambres"); }
    @FXML private void onNavEquipements()  { navigateTo("Equipements.fxml",      "Équipements"); }
    @FXML private void onLogout()          { System.exit(0); }
    @FXML private void onNavDashboard() { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }

    private void navigateTo(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/" + fxml));
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
            stage.setTitle("EcoTrip Admin — " + title);
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg);
        a.setTitle(title);
        a.showAndWait();
    }
}