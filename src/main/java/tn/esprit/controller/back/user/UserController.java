package tn.esprit.controller.back.user;

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

    /* ─── Form ─── */
    @FXML private Label    formIcon, formTitle, formSubtitle;
    @FXML private TextField usernameField, emailField, addressField, telephoneField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private CheckBox isVerifiedCheck;
    @FXML private Label    errUsername, errEmail, errPassword;
    @FXML private Button   submitBtn;
    @FXML private Label    passwordHint;

    /* ─── Table ─── */
    @FXML private TextField          searchField;
    @FXML private ComboBox<String>   sortCombo, roleFilter;
    @FXML private TableView<User>    tableView;
    @FXML private TableColumn<User, Integer> colIndex;
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
                        isVerifiedCheck.isSelected()
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
                        passwordField.getText()
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
        statTotal.setText(String.valueOf(service.countUsers()));
        statAdmins.setText(String.valueOf(service.countByRole(Role.ROLE_ADMIN)));
        statUsers.setText(String.valueOf(service.countByRole(Role.ROLE_USER)));
        statVerified.setText(String.valueOf(service.countVerifiedUsers()));
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
        chooser.setInitialFileName("users_" + java.time.LocalDate.now() + ".csv");
        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));

        java.io.File file = chooser.showSaveDialog(tableView.getScene().getWindow());
        if (file == null) return;

        // Build the same filtered+sorted list that's currently displayed
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

        try (FileWriter fw = new FileWriter(file)) {
            fw.write("ID;Username;Email;Adresse;Téléphone;Rôle;Vérifié\n");
            for (User u : toExport) {
                fw.write(String.join(";",
                        String.valueOf(u.getId()),
                        u.getUsername(),
                        u.getEmail(),
                        u.getAddress()    != null ? u.getAddress()    : "",
                        u.getTelephone()  != null ? u.getTelephone()  : "",
                        u.getRoles().name(),
                        u.isVerified() ? "Oui" : "Non"
                ) + "\n");
            }
            showToast("📥 Export CSV : " + file.getName()
                    + " (" + toExport.size() + " lignes)");
        } catch (IOException e) {
            showAlert("Erreur export", e.getMessage());
        }
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