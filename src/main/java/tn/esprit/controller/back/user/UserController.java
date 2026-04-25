package tn.esprit.controller.back.user;

import javafx.scene.chart.PieChart;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.models.User;
import tn.esprit.models.enums.Role;
import tn.esprit.navigation.Routes;
import tn.esprit.navigation.SceneManager;
import tn.esprit.services.Auth_User.UserService;
import tn.esprit.utils.PasswordUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.io.FileWriter;
import java.io.IOException;

public class UserController implements Initializable {

    /* ─── Stats ─── */
    @FXML private Label statTotal, statAdmins, statUsers, statVerified;
    // Charts
    @FXML private PieChart chartRoles;
    @FXML private PieChart chartVerified;

    /* ─── Form ─── */
    @FXML private Label    formIcon, formTitle, formSubtitle;
    @FXML private TextField usernameField, emailField, addressField, telephoneField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private CheckBox isVerifiedCheck;
    @FXML private Label    errUsername, errEmail, errPassword;
    @FXML private Button   submitBtn;
    @FXML private Label    passwordHint;

    /* ─── Photo ─── */
    @FXML private ImageView photoPreview;
    @FXML private Label     photoLabel;
    private String selectedPhotoPath = null;

    /* ─── Table ─── */
    @FXML private TextField          searchField;
    @FXML private ComboBox<String>   sortCombo, roleFilter;
    @FXML private TableView<User>    tableView;
    @FXML private TableColumn<User, Integer> colIndex;
    @FXML private TableColumn<User, Void>    colPhoto;
    @FXML private TableColumn<User, String>  colUsername, colEmail, colTelephone;
    @FXML private TableColumn<User, String>  colRole, colVerified;
    @FXML private TableColumn<User, Void>    colActions;
    @FXML private Label badgeCount, pagInfo;
    @FXML private HBox  pagButtons;

    /* ─── State ─── */
    private final UserService service = new UserService();
    private List<User> allData;
    private User userEnEdition = null;
    private static final int PER_PAGE = 8;
    private int currentPage = 1;
    /* ─── New stat labels (row 2) ─── */
    @FXML private Label statTotalSub, statAdminsPct, statUsersPct, statVerifiedPct;
    @FXML private Label statFaceEnrolled, statFaceEnrolledPct;
    @FXML private Label statWithPhoto, statWithPhotoPct;
    @FXML private Label statNoPhone, statNoPhonePct;
    @FXML private Label statNoAddress, statNoAddressPct;
    @FXML private Label citiesNote, noCitiesLabel;
    /* ─── New charts ─── */
    @FXML private javafx.scene.chart.PieChart chartFace;

    /* ─── Cities bar chart container ─── */
    @FXML private VBox citiesContainer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Setup combos
        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier par…", "Nom (A→Z)", "Email (A→Z)"));
        sortCombo.getSelectionModel().selectFirst();

        roleFilter.setItems(FXCollections.observableArrayList(
                "Tous les rôles", "ROLE_USER", "ROLE_ADMIN"));
        roleFilter.getSelectionModel().selectFirst();

        roleCombo.setItems(FXCollections.observableArrayList("ROLE_USER", "ROLE_ADMIN"));
        roleCombo.setValue("ROLE_USER");

        setupColumns();
        loadData();
        refreshAll();
    }

    /* ─── Photo chooser ─── */
    @FXML
    private void onChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp")
        );
        Stage stage = (Stage) usernameField.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedPhotoPath = file.getAbsolutePath();
            photoPreview.setImage(new Image(file.toURI().toString(), 72, 72, true, true));
            photoLabel.setText("✅ " + file.getName());
        }
    }

    /* ─── Validation ─── */
    @FXML private void validateUsername() {
        setFieldError(usernameField, errUsername, usernameField.getText().trim().isEmpty());
    }

    @FXML private void validateEmail() {
        String email = emailField.getText().trim();
        setFieldError(emailField, errEmail,
                email.isEmpty() || !email.contains("@"));
    }

    @FXML private void validatePassword() {
        // Only required on create
        if (userEnEdition != null) return;
        setFieldError(passwordField, errPassword,
                passwordField.getText().length() < 6);
    }

    private boolean validateAll() {
        boolean ok = true;
        if (usernameField.getText().trim().isEmpty()) {
            setFieldError(usernameField, errUsername, true); ok = false;
        } else setFieldError(usernameField, errUsername, false);

        String email = emailField.getText().trim();
        if (email.isEmpty() || !email.contains("@")) {
            setFieldError(emailField, errEmail, true); ok = false;
        } else setFieldError(emailField, errEmail, false);

        String pass = passwordField.getText();
        if (userEnEdition == null && pass.length() < 6) {
            setFieldError(passwordField, errPassword, true); ok = false;
        } else setFieldError(passwordField, errPassword, false);

        return ok;
    }

    /* ─── Submit ─── */
    @FXML
    private void onSubmit() {
        if (!validateAll()) return;

        try {
            if (userEnEdition == null) {
                service.createUser(
                        usernameField.getText().trim(),
                        emailField.getText().trim(),
                        passwordField.getText(),
                        addressField.getText().trim(),
                        telephoneField.getText().trim(),
                        roleCombo.getValue(),
                        isVerifiedCheck.isSelected(),
                        selectedPhotoPath
                );
                showToast("✅ Utilisateur créé !");
            } else {
                service.updateUser(
                        userEnEdition.getId(),
                        usernameField.getText().trim(),
                        emailField.getText().trim(),
                        addressField.getText().trim(),
                        telephoneField.getText().trim(),
                        roleCombo.getValue(),
                        isVerifiedCheck.isSelected(),
                        passwordField.getText(),
                        selectedPhotoPath
                );
                showToast("💾 Utilisateur modifié !");
            }

            onReset();
            refreshAll();

        } catch (RuntimeException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    /* ─── Reset ─── */
    @FXML
    private void onReset() {
        userEnEdition = null;
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        addressField.clear();
        telephoneField.clear();
        roleCombo.setValue("ROLE_USER");
        isVerifiedCheck.setSelected(false);

        // Reset photo
        selectedPhotoPath = null;
        if (photoPreview != null) photoPreview.setImage(null);
        if (photoLabel   != null) photoLabel.setText("Aucune photo sélectionnée");

        setFieldError(usernameField, errUsername, false);
        setFieldError(emailField, errEmail, false);
        setFieldError(passwordField, errPassword, false);

        formIcon.setText("👤");
        formTitle.setText("Nouvel Utilisateur");
        formSubtitle.setText("Remplissez les informations ci-dessous.");
        submitBtn.setText("➕ Ajouter");
        passwordHint.setVisible(false);
        passwordHint.setManaged(false);
    }

    /* ─── Load for edit ─── */
    private void chargerPourEdition(User u) {
        userEnEdition = u;
        usernameField.setText(u.getUsername());
        emailField.setText(u.getEmail());
        passwordField.clear(); // never pre-fill password
        addressField.setText(u.getAddress()   != null ? u.getAddress()   : "");
        telephoneField.setText(u.getTelephone() != null ? u.getTelephone() : "");
        roleCombo.setValue(u.getRoles().name());
        isVerifiedCheck.setSelected(u.isVerified());

        // Load existing photo
        selectedPhotoPath = u.getImage();
        if (selectedPhotoPath != null && !selectedPhotoPath.isEmpty()) {
            try {
                File f = new File(selectedPhotoPath);
                if (f.exists()) {
                    photoPreview.setImage(new Image(f.toURI().toString(), 72, 72, true, true));
                    photoLabel.setText("✅ " + f.getName());
                } else {
                    photoPreview.setImage(null);
                    photoLabel.setText("Photo introuvable sur le disque");
                }
            } catch (Exception ex) {
                photoPreview.setImage(null);
                photoLabel.setText("Aucune photo sélectionnée");
            }
        } else {
            photoPreview.setImage(null);
            photoLabel.setText("Aucune photo sélectionnée");
        }

        formIcon.setText("✏️");
        formTitle.setText("Modifier l'Utilisateur");
        formSubtitle.setText("Mettez à jour les informations.");
        submitBtn.setText("💾 Enregistrer");
        passwordHint.setVisible(true);
        passwordHint.setManaged(true);
        usernameField.requestFocus();
    }

    /* ─── Data ─── */
    private void loadData() {
        try {
            allData = service.getAllUsers();
        } catch (Exception e) {
            allData = List.of();
            showAlert("Erreur", e.getMessage());
        }
    }

    private void refreshAll() {
        loadData();
        updateStats();
        renderTable();
    }

    private void updateStats() {
        int total      = allData.size();
        int admins     = (int) allData.stream()
                .filter(u -> u.getRoles() == Role.ROLE_ADMIN).count();
        int users      = (int) allData.stream()
                .filter(u -> u.getRoles() == Role.ROLE_USER).count();
        int verified   = (int) allData.stream()
                .filter(User::isVerified).count();
        int faceEnrolled = (int) allData.stream()
                .filter(u -> u.getFaceDescriptor() != null
                        && !u.getFaceDescriptor().isBlank()).count();
        int withPhoto  = (int) allData.stream()
                .filter(u -> u.getImage() != null && !u.getImage().isBlank()).count();
        int noPhone    = (int) allData.stream()
                .filter(u -> u.getTelephone() == null || u.getTelephone().isBlank()).count();
        int noAddress  = (int) allData.stream()
                .filter(u -> u.getAddress() == null || u.getAddress().isBlank()).count();

        // ── Row 1 labels ──
        statTotal.setText(String.valueOf(total));
        statTotalSub.setText(total == 0 ? "—"
                : users + " membres · " + admins + " admins");

        statAdmins.setText(String.valueOf(admins));
        statAdminsPct.setText(pct(admins, total) + " du total");

        statUsers.setText(String.valueOf(users));
        statUsersPct.setText(pct(users, total) + " du total");

        statVerified.setText(String.valueOf(verified));
        statVerifiedPct.setText(pct(verified, total) + " vérifiés");

        // ── Row 2 labels ──
        statFaceEnrolled.setText(String.valueOf(faceEnrolled));
        statFaceEnrolledPct.setText(pct(faceEnrolled, total) + " des comptes");

        statWithPhoto.setText(String.valueOf(withPhoto));
        statWithPhotoPct.setText(pct(withPhoto, total) + " ont une photo");

        statNoPhone.setText(String.valueOf(noPhone));
        statNoPhonePct.setText(pct(noPhone, total) + " incomplets");

        statNoAddress.setText(String.valueOf(noAddress));
        statNoAddressPct.setText(pct(noAddress, total) + " incomplets");

        // ── Pie charts ──
        chartRoles.getData().setAll(
                new javafx.scene.chart.PieChart.Data("Utilisateurs (" + users + ")", users),
                new javafx.scene.chart.PieChart.Data("Admins (" + admins + ")", admins)
        );
        chartVerified.getData().setAll(
                new javafx.scene.chart.PieChart.Data("Vérifiés (" + verified + ")", verified),
                new javafx.scene.chart.PieChart.Data("Non vérifiés (" + (total - verified) + ")",
                        total - verified)
        );
        chartFace.getData().setAll(
                new javafx.scene.chart.PieChart.Data("Enregistré (" + faceEnrolled + ")", faceEnrolled),
                new javafx.scene.chart.PieChart.Data("Non enregistré (" + (total - faceEnrolled) + ")",
                        total - faceEnrolled)
        );

        // ── Cities bar chart ──
        buildCitiesChart();
    }

    /** Build horizontal bar chart for top cities extracted from address field */
    private void buildCitiesChart() {
        citiesContainer.getChildren().clear();

        // Extract city from address — take the first meaningful token after splitting by comma or common separators
        java.util.Map<String, Long> cityCount = allData.stream()
                .filter(u -> u.getAddress() != null && !u.getAddress().isBlank())
                .map(u -> extractCity(u.getAddress()))
                .filter(c -> !c.isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        c -> c, java.util.stream.Collectors.counting()));

        if (cityCount.isEmpty()) {
            noCitiesLabel.setVisible(true);  noCitiesLabel.setManaged(true);
            citiesNote.setText("");
            return;
        }
        noCitiesLabel.setVisible(false); noCitiesLabel.setManaged(false);

        // Top 6 cities sorted by count descending
        java.util.List<java.util.Map.Entry<String, Long>> top = cityCount.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .collect(Collectors.toList());

        citiesNote.setText(cityCount.size() + " ville(s) distincte(s) · top 6 affichées");

        long max = top.get(0).getValue();

        for (java.util.Map.Entry<String, Long> entry : top) {
            String city  = entry.getKey();
            long   count = entry.getValue();
            double pct   = max == 0 ? 0 : (double) count / max;

            // City label
            Label cityLabel = new Label(city);
            cityLabel.setMinWidth(140);
            cityLabel.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;-fx-font-weight:600;");

            // Bar background
            StackPane barWrap = new StackPane();
            barWrap.setStyle("-fx-background-color:#f1f5f9;-fx-background-radius:6;");
            barWrap.setPrefHeight(22);
            HBox.setHgrow(barWrap, Priority.ALWAYS);

            // Bar fill
            HBox barFill = new HBox();
            barFill.setPrefHeight(22);
            barFill.setStyle("-fx-background-color:linear-gradient(to right,#1a5f2a,#4ade80);"
                    + "-fx-background-radius:6;");
            barFill.prefWidthProperty().bind(
                    barWrap.widthProperty().multiply(pct));
            barWrap.getChildren().add(barFill);
            StackPane.setAlignment(barFill, javafx.geometry.Pos.CENTER_LEFT);

            // Count label
            Label countLabel = new Label(count + " utilisateur" + (count > 1 ? "s" : ""));
            countLabel.setMinWidth(110);
            countLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#64748b;-fx-alignment:CENTER_RIGHT;");

            HBox row = new HBox(12, cityLabel, barWrap, countLabel);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            citiesContainer.getChildren().add(row);
        }
    }

    /**
     * Extract a city name from a free-text address field.
     * Strategy: split by comma, take the longest token that looks like a place name.
     * Examples:
     *   "12 Rue de la Paix, Tunis, Tunisia"  → "Tunis"
     *   "Sfax"                                → "Sfax"
     *   "Tunis Governorate"                   → "Tunis Governorate"
     */
    private String extractCity(String address) {
        if (address == null || address.isBlank()) return "";

        String[] parts = address.split("[,;|\\-]");
        String city = "";

        for (String part : parts) {
            String trimmed = part.trim();
            // Skip very short parts (street numbers) and very long parts (full sentences)
            if (trimmed.length() >= 3 && trimmed.length() <= 40
                    && !trimmed.matches(".*\\d{4,}.*")  // skip parts with long numbers (zip codes)
                    && trimmed.length() > city.length()) {
                city = trimmed;
            }
        }

        // If nothing found, use the first 30 chars of the address as-is
        if (city.isBlank() && !address.isBlank()) {
            city = address.trim().substring(0, Math.min(address.trim().length(), 30));
        }

        // Capitalize first letter
        return city.isEmpty() ? "" :
                Character.toUpperCase(city.charAt(0)) + city.substring(1);
    }

    /** Format a percentage string like "73%" */
    private String pct(int part, int total) {
        if (total == 0) return "0%";
        return String.format("%.0f%%", (double) part / total * 100);
    }
    private void renderTable() {
        String query  = searchField.getText().toLowerCase().trim();
        String sort   = sortCombo.getValue();
        String filter = roleFilter.getValue();

        List<User> filtered = allData.stream()
                .filter(u -> query.isEmpty()
                        || u.getUsername().toLowerCase().contains(query)
                        || u.getEmail().toLowerCase().contains(query)
                        || (u.getTelephone() != null && u.getTelephone().contains(query)))
                .filter(u -> filter == null
                        || filter.equals("Tous les rôles")
                        || u.getRoles().name().equals(filter))
                .collect(Collectors.toList());

        if ("Nom (A→Z)".equals(sort))
            filtered.sort(Comparator.comparing(User::getUsername));
        else if ("Email (A→Z)".equals(sort))
            filtered.sort(Comparator.comparing(User::getEmail));

        badgeCount.setText(String.valueOf(filtered.size()));

        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PER_PAGE));
        if (currentPage > totalPages) currentPage = 1;
        int from = (currentPage - 1) * PER_PAGE;
        int to   = Math.min(from + PER_PAGE, total);

        tableView.setItems(FXCollections.observableArrayList(
                filtered.subList(from, to)));

        pagInfo.setText(total == 0 ? "" :
                "Affichage " + (from + 1) + "–" + to + " sur " + total);

        pagButtons.getChildren().clear();
        for (int p = 1; p <= totalPages; p++) {
            final int pn = p;
            Button b = new Button(String.valueOf(p));
            b.getStyleClass().add("page-btn");
            if (p == currentPage) b.getStyleClass().add("page-btn-active");
            b.setOnAction(e -> { currentPage = pn; renderTable(); });
            pagButtons.getChildren().add(b);
        }
    }

    /* ─── Columns ─── */
    private void setupColumns() {
        // Index
        colIndex.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(
                        getTableView().getItems().indexOf(getTableRow().getItem())
                                + 1 + (currentPage - 1) * PER_PAGE));
            }
        });

        // Photo thumbnail column
        colPhoto.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(36);
                iv.setFitHeight(36);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                iv.setStyle("-fx-background-radius:50%;");
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());
                String path = u.getImage();
                if (path != null && !path.isEmpty()) {
                    try {
                        File f = new File(path);
                        if (f.exists()) {
                            iv.setImage(new Image(f.toURI().toString(), 36, 36, true, true));
                            setGraphic(iv);
                            return;
                        }
                    } catch (Exception ignored) {}
                }
                // Fallback: initials label
                String name = u.getUsername();
                String initials = name.length() >= 2 ? name.substring(0, 2).toUpperCase()
                        : name.toUpperCase();
                Label lbl = new Label(initials);
                lbl.setStyle(
                        "-fx-background-color:#6366f1; -fx-text-fill:white;"
                                + "-fx-font-size:12px; -fx-font-weight:bold;"
                                + "-fx-background-radius:18; -fx-min-width:36; -fx-min-height:36;"
                                + "-fx-alignment:center;");
                setGraphic(lbl);
            }
        });

        // Username
        colUsername.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getUsername()));

        // Email
        colEmail.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getEmail()));

        // Telephone
        colTelephone.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getTelephone() != null ? c.getValue().getTelephone() : "—"));

        // Role badge
        colRole.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getRoles().name()));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setGraphic(null); return; }
                Label badge = new Label(role.equals("ROLE_ADMIN") ? "Admin" : "User");
                badge.getStyleClass().add(role.equals("ROLE_ADMIN") ? "badge-admin" : "badge-user");
                setGraphic(badge); setText(null);
            }
        });

        // Verified badge
        colVerified.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().isVerified() ? "OUI" : "NON"));
        colVerified.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setGraphic(null); return; }
                Label badge = new Label(val.equals("OUI") ? "✅ Oui" : "❌ Non");
                badge.getStyleClass().add(val.equals("OUI") ? "badge-actif" : "badge-inactif");
                setGraphic(badge); setText(null);
            }
        });

        // Actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button delBtn  = new Button("🗑️ Supprimer");
            private final HBox   box     = new HBox(8, editBtn, delBtn);
            {
                editBtn.getStyleClass().add("btn-edit");
                delBtn.getStyleClass().add("btn-del");
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.setOnAction(e ->
                        chargerPourEdition(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e ->
                        confirmDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    /* ─── Delete ─── */
    private void confirmDelete(User u) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer ?");
        alert.setHeaderText("Supprimer « " + u.getUsername() + " » ?");
        alert.setContentText("Cette action est irréversible.");
        ButtonType cancel  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(cancel, confirm);
        alert.showAndWait().filter(r -> r == confirm).ifPresent(r -> {
            service.deleteUser(u.getId());
            if (userEnEdition != null && userEnEdition.getId() == u.getId()) onReset();
            refreshAll();
        });
    }

    /* ─── Navigation ─── */
    @FXML private void onSearch() { currentPage = 1; renderTable(); }
    @FXML private void onSort()   { currentPage = 1; renderTable(); }
    @FXML private void onFilter() { currentPage = 1; renderTable(); }

    /* ─── Export CSV ─── */
    @FXML
    private void onExportCsv() {
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Exporter les utilisateurs");
        chooser.setInitialFileName("ecotrip_users_" + java.time.LocalDate.now() + ".csv");
        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));

        java.io.File file = chooser.showSaveDialog(tableView.getScene().getWindow());
        if (file == null) return;

        // Apply current filters — export what the admin sees
        String query  = searchField.getText().toLowerCase().trim();
        String sort   = sortCombo.getValue();
        String filter = roleFilter.getValue();

        List<User> toExport = allData.stream()
                .filter(u -> query.isEmpty()
                        || u.getUsername().toLowerCase().contains(query)
                        || u.getEmail().toLowerCase().contains(query)
                        || (u.getTelephone() != null && u.getTelephone().contains(query)))
                .filter(u -> filter == null
                        || filter.equals("Tous les rôles")
                        || u.getRoles().name().equals(filter))
                .collect(Collectors.toList());

        if ("Nom (A→Z)".equals(sort))
            toExport.sort(Comparator.comparing(User::getUsername));
        else if ("Email (A→Z)".equals(sort))
            toExport.sort(Comparator.comparing(User::getEmail));

        try (FileWriter fw = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {

            // ── BOM for Excel UTF-8 compatibility ──
            fw.write('\uFEFF');

            // ── Section 1: Export metadata ──
            fw.write("# Export EcoTrip — Utilisateurs\n");
            fw.write("# Date;" + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n");
            fw.write("# Filtre rôle;" + (filter != null ? filter : "Tous") + "\n");
            fw.write("# Recherche;" + (query.isEmpty() ? "—" : query) + "\n");
            fw.write("# Total exporté;" + toExport.size() + "\n");
            fw.write("\n");

            // ── Section 2: Main user data ──
            fw.write("ID;Username;Email;Rôle;Vérifié;Adresse;Ville;Téléphone;"
                    + "Photo;Visage Enregistré\n");

            for (User u : toExport) {
                boolean hasFace    = u.getFaceDescriptor() != null
                        && !u.getFaceDescriptor().isBlank();
                boolean hasPhoto   = u.getImage() != null && !u.getImage().isBlank();
                String  city       = u.getAddress() != null
                        ? extractCity(u.getAddress()) : "";

                fw.write(String.join(";",
                        String.valueOf(u.getId()),
                        csvSafe(u.getUsername()),
                        csvSafe(u.getEmail()),
                        u.getRoles().name().equals("ROLE_ADMIN") ? "Administrateur" : "Utilisateur",
                        u.isVerified() ? "Oui" : "Non",
                        csvSafe(u.getAddress()   != null ? u.getAddress()   : ""),
                        csvSafe(city),
                        csvSafe(u.getTelephone() != null ? u.getTelephone() : ""),
                        hasPhoto   ? "Oui" : "Non",
                        hasFace    ? "Oui" : "Non"
                ) + "\n");
            }

            // ── Section 3: Summary statistics ──
            fw.write("\n");
            fw.write("# RÉSUMÉ STATISTIQUE\n");
            fw.write("Métrique;Valeur;Pourcentage\n");

            int total     = toExport.size();
            int admins    = (int) toExport.stream()
                    .filter(u -> u.getRoles() == Role.ROLE_ADMIN).count();
            int verified  = (int) toExport.stream().filter(User::isVerified).count();
            int face      = (int) toExport.stream()
                    .filter(u -> u.getFaceDescriptor() != null
                            && !u.getFaceDescriptor().isBlank()).count();
            int photo     = (int) toExport.stream()
                    .filter(u -> u.getImage() != null && !u.getImage().isBlank()).count();
            int noPhone   = (int) toExport.stream()
                    .filter(u -> u.getTelephone() == null || u.getTelephone().isBlank()).count();
            int noAddr    = (int) toExport.stream()
                    .filter(u -> u.getAddress() == null || u.getAddress().isBlank()).count();

            fw.write("Total utilisateurs;"   + total    + ";100%\n");
            fw.write("Administrateurs;"      + admins   + ";" + pct(admins, total) + "\n");
            fw.write("Membres;"              + (total - admins) + ";" + pct(total - admins, total) + "\n");
            fw.write("Emails vérifiés;"      + verified + ";" + pct(verified, total) + "\n");
            fw.write("Visage enregistré;"    + face     + ";" + pct(face, total) + "\n");
            fw.write("Avec photo de profil;" + photo    + ";" + pct(photo, total) + "\n");
            fw.write("Sans téléphone;"       + noPhone  + ";" + pct(noPhone, total) + "\n");
            fw.write("Sans adresse;"         + noAddr   + ";" + pct(noAddr, total) + "\n");

            // ── Section 4: Top cities ──
            fw.write("\n");
            fw.write("# TOP VILLES\n");
            fw.write("Ville;Utilisateurs;Pourcentage\n");

            java.util.Map<String, Long> cityMap = toExport.stream()
                    .filter(u -> u.getAddress() != null && !u.getAddress().isBlank())
                    .map(u -> extractCity(u.getAddress()))
                    .filter(c -> !c.isBlank())
                    .collect(java.util.stream.Collectors.groupingBy(
                            c -> c, java.util.stream.Collectors.counting()));

            cityMap.entrySet().stream()
                    .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(e -> {
                        try {
                            fw.write(csvSafe(e.getKey()) + ";"
                                    + e.getValue() + ";"
                                    + pct(e.getValue().intValue(), total) + "\n");
                        } catch (IOException ex) { /* ignore */ }
                    });

            showToast("📥 Export réussi : " + file.getName()
                    + " — " + toExport.size() + " utilisateurs exportés");

        } catch (IOException e) {
            showAlert("Erreur export", e.getMessage());
        }
    }

    /**
     * Escape a value for CSV: wrap in quotes if it contains semicolons, newlines or quotes.
     */
    private String csvSafe(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    @FXML private void onNavDashboard() { SceneManager.navigateTo(Routes.ADMIN_DASHBOARD); }
    @FXML private void onNavUsers()     { SceneManager.navigateTo(Routes.ADMIN_USERS); }

    /* ─── Helpers ─── */
    private void setFieldError(Control field, Label errLabel, boolean hasError) {
        if (hasError) {
            if (!field.getStyleClass().contains("form-input-error"))
                field.getStyleClass().add("form-input-error");
            errLabel.setVisible(true); errLabel.setManaged(true);
        } else {
            field.getStyleClass().remove("form-input-error");
            errLabel.setVisible(false); errLabel.setManaged(false);
        }
    }

    private void showToast(String msg) {
        pagInfo.setText(msg);
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(this::renderTable);
        }).start();
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}