package tn.esprit.controller.back.activity;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.models.activity.ActivityCategory;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.activity.ActivityCategoryService;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CategoriesController implements Initializable {

    /* ─── Stats ─── */
    @FXML private Label statTotal, statActivities, statAvg;

    /* ─── Form ─── */
    @FXML private Label     formPreviewIcon, formTitle;
    @FXML private TextField nameField;
    @FXML private TextArea  descriptionField;
    @FXML private Label     errName, charCount;
    @FXML private Button    submitBtn;
    @FXML private FlowPane  iconGrid, colorGrid;

    /* ─── Cards list ─── */
    @FXML private TextField searchField;
    @FXML private Label     badgeCount;
    @FXML private FlowPane  catCardsPane;
    @FXML private VBox      emptyState;

    /* ─── Palettes ─── */
    private static final String[] ICONS = {
            "🏕️","🏖️","🏄","🚣","🧗","🏊","🎣","🤿","🛶","⛺",
            "🌊","🏔️","🌋","🗻","🏞️","🌲","🌴","🌿","🍃","🦋",
            "🐠","🐟","🦈","🐬","🐋","🦅","🦜","🐻","🦁","🐘",
            "🎭","🎪","🎨","🎬","🎵","🎸","🎺","🎻","🥁","🎯",
            "⚽","🏀","🏈","⚾","🎾","🏐","🏉","🎱","🏓","🏸",
            "🚵","🚴","🏇","🤸","🧘","🤾","🏋️","🤺","🏹","🥊",
            "🌅","🌄","🌠","🌌","🌈","❄️","🔥","💧","🌪️","⛩️"
    };

    private static final String[] COLORS = {
            "#3b82f6", "#38a169", "#8b5cf6", "#f59e0b",
            "#ef4444", "#0d9488", "#ec4899", "#6366f1",
            "#14b8a6", "#f97316", "#84cc16", "#06b6d4"
    };

    /* ─── State ─── */
    private final ActivityCategoryService service = new ActivityCategoryService();
    private List<ActivityCategory> allData = new ArrayList<>();
    private ActivityCategory categoryEnEdition = null;
    private String selectedIcon  = ICONS[0];
    private String selectedColor = COLORS[0];

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        buildIconPicker();
        buildColorPicker();
        refreshAll();
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
       DATA
       ══════════════════════════════════════════ */

    private void refreshAll() {
        try { allData = service.afficherAll(); }
        catch (SQLException e) { allData = new ArrayList<>(); showAlert("Erreur", e.getMessage()); }
        updateStats();
        renderCards();
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allData.size()));
        statActivities.setText("—");
        statAvg.setText("—");
    }

    /* ══════════════════════════════════════════
       CARDS RENDERING
       ══════════════════════════════════════════ */

    @FXML private void onSearch() { renderCards(); }

    private void renderCards() {
        String q = searchField.getText().toLowerCase().trim();

        List<ActivityCategory> filtered = allData.stream()
                .filter(c -> q.isEmpty()
                        || c.getName().toLowerCase().contains(q)
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

    private VBox buildCatCard(ActivityCategory c, int index) {
        String color = COLORS[index % COLORS.length];
        String icon  = (c.getIcon() != null && !c.getIcon().isBlank())
                ? c.getIcon() : ICONS[index % ICONS.length];

        VBox card = new VBox(10);
        card.getStyleClass().add("cat-card");
        card.setPrefWidth(250);
        card.setPadding(new Insets(18, 20, 16, 20));
        card.setStyle("-fx-border-color:" + color + " transparent transparent transparent;"
                + "-fx-border-width:0 0 0 4;");

        // ── Icon + Name ──
        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("cat-card-icon");
        iconLbl.setStyle("-fx-background-color:" + color + "55;"
                + "-fx-border-color:" + color + "bb;");

        Label nameLbl = new Label(c.getName());
        nameLbl.getStyleClass().add("cat-card-name");

        top.getChildren().addAll(iconLbl, nameLbl);

        // ── Description ──
        String descText = (c.getDescription() == null || c.getDescription().isEmpty())
                ? "Aucune description." : c.getDescription();
        Label desc = new Label(descText);
        desc.getStyleClass().add("cat-card-desc");
        desc.setWrapText(true);

        // ── Buttons ──
        HBox meta = new HBox(6);
        meta.setAlignment(Pos.CENTER_RIGHT);
        Button editBtn = new Button("✏️ Modifier");
        editBtn.getStyleClass().add("btn-edit");
        Button delBtn = new Button("🗑️");
        delBtn.getStyleClass().add("btn-del");
        editBtn.setOnAction(e -> loadForEdit(c));
        delBtn.setOnAction(e  -> confirmDelete(c));
        meta.getChildren().addAll(editBtn, delBtn);

        card.getChildren().addAll(top, desc, meta);
        return card;
    }

    /* ══════════════════════════════════════════
       FORM — VALIDATION
       ══════════════════════════════════════════ */

    @FXML private void validateName() {
        setFieldError(nameField, errName, nameField.getText().trim().length() < 3);
    }

    @FXML private void updateCounter() {
        charCount.setText(descriptionField.getText().length() + " / 1000 caractères");
    }

    private boolean validateAll() {
        boolean ok = true;
        if (nameField.getText().trim().length() < 3) {
            setFieldError(nameField, errName, true); ok = false;
        } else setFieldError(nameField, errName, false);
        return ok;
    }

    /* ══════════════════════════════════════════
       SUBMIT / RESET / EDIT / DELETE
       ══════════════════════════════════════════ */

    @FXML private void onSubmit() {
        if (!validateAll()) return;
        ActivityCategory c = categoryEnEdition != null ? categoryEnEdition : new ActivityCategory();
        c.setName(nameField.getText().trim());
        c.setDescription(descriptionField.getText().trim());
        c.setIcon(selectedIcon);
        try {
            if (categoryEnEdition == null) service.ajouter(c);
            else                           service.modifier(c);
            onReset();
            refreshAll();
        } catch (Exception e) { showAlert("Erreur", e.getMessage()); }
    }

    @FXML private void onReset() {
        categoryEnEdition = null;
        selectedIcon  = ICONS[0];
        selectedColor = COLORS[0];
        nameField.clear();
        descriptionField.clear();
        charCount.setText("0 / 1000 caractères");
        setFieldError(nameField, errName, false);
        formPreviewIcon.setText(selectedIcon);
        formTitle.setText("Nouvelle Catégorie");
        submitBtn.setText("💾 Enregistrer");
        buildIconPicker();
        buildColorPicker();
    }

    private void loadForEdit(ActivityCategory c) {
        categoryEnEdition = c;
        nameField.setText(c.getName());
        descriptionField.setText(c.getDescription() != null ? c.getDescription() : "");
        selectedIcon = (c.getIcon() != null && !c.getIcon().isBlank()) ? c.getIcon() : ICONS[0];
        formPreviewIcon.setText(selectedIcon);
        formTitle.setText("Modifier la Catégorie");
        submitBtn.setText("💾 Enregistrer");
        updateCounter();
        setFieldError(nameField, errName, false);
        buildIconPicker();
        iconGrid.getChildren().stream()
                .filter(n -> n instanceof Button)
                .map(n -> (Button) n)
                .filter(b -> b.getText().equals(selectedIcon))
                .findFirst()
                .ifPresent(this::highlightIconBtn);
        nameField.requestFocus();
    }

    private void confirmDelete(ActivityCategory c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer ?");
        alert.setHeaderText("Supprimer « " + c.getName() + " » ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(r -> r == confirm).ifPresent(r -> {
            try {
                service.supprimer(c.getId());
                if (categoryEnEdition != null && categoryEnEdition.getId() == c.getId()) onReset();
                refreshAll();
            } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
        });
    }

    /* ══════════════════════════════════════════
       NAVIGATION + HELPERS
       ══════════════════════════════════════════ */

    @FXML private void onNavDashboard()  { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }
    @FXML private void onNavActivities() { SceneManager.navigateTo(Routes.ADMIN_ACTIVITIES); }

    private void setFieldError(TextField field, Label errLabel, boolean hasError) {
        if (hasError) {
            if (!field.getStyleClass().contains("form-input-error"))
                field.getStyleClass().add("form-input-error");
            errLabel.setVisible(true); errLabel.setManaged(true);
        } else {
            field.getStyleClass().remove("form-input-error");
            errLabel.setVisible(false); errLabel.setManaged(false);
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }
}