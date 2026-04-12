//package tn.esprit.controller.back.user;
//
//import tn.esprit.models.User;
//import tn.esprit.models.enums.Role;
//import tn.esprit.navigation.Routes;
//import tn.esprit.navigation.SceneManager;
//import tn.esprit.repository.UserRepository;
//import tn.esprit.session.SessionManager;
//import javafx.application.Platform;
//import javafx.beans.property.SimpleStringProperty;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.collections.transformation.FilteredList;
//import javafx.fxml.FXML;
//import javafx.scene.chart.PieChart;
//import javafx.scene.control.*;
//import javafx.scene.layout.HBox;
//
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.List;
//
//public class UserListController {
//
//    // Stats
//    @FXML private Label statTotal;
//    @FXML private Label statAdmins;
//    @FXML private Label statUsers;
//    @FXML private Label statVerified;
//
//    // Charts
//    @FXML private PieChart chartRoles;
//    @FXML private PieChart chartVerified;
//
//    // Toolbar
//    @FXML private TextField  searchField;
//    @FXML private ComboBox<String> roleFilter;
//    @FXML private Label successLabel;
//
//    // Table
//    @FXML private TableView<User>          userTable;
//    @FXML private TableColumn<User, String> colId;
//    @FXML private TableColumn<User, String> colUsername;
//    @FXML private TableColumn<User, String> colEmail;
//    @FXML private TableColumn<User, String> colTelephone;
//    @FXML private TableColumn<User, String> colRole;
//    @FXML private TableColumn<User, String> colVerified;
//    @FXML private TableColumn<User, Void>   colActions;
//
//    private final UserRepository userRepository = new UserRepository();
//    private ObservableList<User> allUsers;
//    private FilteredList<User>   filteredUsers;
//
//    @FXML
//    public void initialize() {
//        if (!SessionManager.getInstance().isAdmin()) {
//            Platform.runLater(() -> SceneManager.navigateTo(Routes.LOGIN));
//            return;
//        }
//
//        setupTable();
//        setupFilters();
//        loadData();
//
//        // Show success message if coming back from create/edit/delete
//        String msg = SessionManager.getInstance().getFlashMessage();
//        if (msg != null) {
//            showSuccess(msg);
//            SessionManager.getInstance().clearFlashMessage();
//        }
//    }
//
//    // ── Table setup ───────────────────────────────────────
//
//    private void setupTable() {
//        colId.setCellValueFactory(c ->
//                new SimpleStringProperty(String.valueOf(c.getValue().getId())));
//
//        colUsername.setCellValueFactory(c ->
//                new SimpleStringProperty(c.getValue().getUsername()));
//
//        colEmail.setCellValueFactory(c ->
//                new SimpleStringProperty(c.getValue().getEmail()));
//
//        colTelephone.setCellValueFactory(c ->
//                new SimpleStringProperty(
//                        c.getValue().getTelephone() != null ? c.getValue().getTelephone() : "—"));
//
//        // Role badge column
//        colRole.setCellValueFactory(c ->
//                new SimpleStringProperty(c.getValue().getRoles().name()));
//        colRole.setCellFactory(col -> new TableCell<>() {
//            @Override
//            protected void updateItem(String role, boolean empty) {
//                super.updateItem(role, empty);
//                if (empty || role == null) { setGraphic(null); return; }
//                Label badge = new Label(role.equals("ROLE_ADMIN") ? "Admin" : "User");
//                badge.getStyleClass().add(
//                        role.equals("ROLE_ADMIN") ? "badge-admin" : "badge-user");
//                setGraphic(badge);
//                setText(null);
//            }
//        });
//
//        // Verified badge column
//        colVerified.setCellValueFactory(c ->
//                new SimpleStringProperty(c.getValue().isVerified() ? "OUI" : "NON"));
//        colVerified.setCellFactory(col -> new TableCell<>() {
//            @Override
//            protected void updateItem(String val, boolean empty) {
//                super.updateItem(val, empty);
//                if (empty || val == null) { setGraphic(null); return; }
//                Label badge = new Label(val);
//                badge.getStyleClass().add(
//                        val.equals("OUI") ? "badge-verified-yes" : "badge-verified-no");
//                setGraphic(badge);
//                setText(null);
//            }
//        });
//
//        // Actions column: edit / view / delete buttons
//        colActions.setCellFactory(col -> new TableCell<>() {
//            private final Button btnEdit   = new Button("✏");
//            private final Button btnView   = new Button("👁");
//            private final Button btnDelete = new Button("🗑");
//            private final HBox   box       = new HBox(6, btnEdit, btnView, btnDelete);
//
//            {
//                btnEdit.getStyleClass().add("btn-action-edit");
//                btnView.getStyleClass().add("btn-action-view");
//                btnDelete.getStyleClass().add("btn-action-delete");
//
//                btnEdit.setOnAction(e -> {
//                    User user = getTableView().getItems().get(getIndex());
//                    handleEdit(user);
//                });
//                btnView.setOnAction(e -> {
//                    User user = getTableView().getItems().get(getIndex());
//                    handleView(user);
//                });
//                btnDelete.setOnAction(e -> {
//                    User user = getTableView().getItems().get(getIndex());
//                    handleDelete(user);
//                });
//            }
//
//            @Override
//            protected void updateItem(Void item, boolean empty) {
//                super.updateItem(item, empty);
//                setGraphic(empty ? null : box);
//            }
//        });
//    }
//
//    // ── Filter setup ──────────────────────────────────────
//
//    private void setupFilters() {
//        roleFilter.getItems().addAll("Tous les rôles", "ROLE_USER", "ROLE_ADMIN");
//        roleFilter.setValue("Tous les rôles");
//
//        // Live search — filters the list as user types
//        searchField.textProperty().addListener((obs, old, val) -> applyFilter());
//        roleFilter.valueProperty().addListener((obs, old, val) -> applyFilter());
//    }
//
//    private void applyFilter() {
//        String query = searchField.getText().toLowerCase().trim();
//        String role  = roleFilter.getValue();
//
//        filteredUsers.setPredicate(user -> {
//            boolean matchesQuery = query.isEmpty()
//                    || user.getUsername().toLowerCase().contains(query)
//                    || user.getEmail().toLowerCase().contains(query)
//                    || (user.getTelephone() != null && user.getTelephone().contains(query));
//
//            boolean matchesRole = role == null
//                    || role.equals("Tous les rôles")
//                    || user.getRoles().name().equals(role);
//
//            return matchesQuery && matchesRole;
//        });
//    }
//
//    // ── Data loading ──────────────────────────────────────
//
//    private void loadData() {
//        List<User> users = userRepository.findAll();
//        allUsers     = FXCollections.observableArrayList(users);
//        filteredUsers = new FilteredList<>(allUsers, u -> true);
//        userTable.setItems(filteredUsers);
//
//        // Stats
//        int total    = userRepository.count();
//        int admins   = userRepository.countByRole(Role.ROLE_ADMIN);
//        int regular  = userRepository.countByRole(Role.ROLE_USER);
//        int verified = userRepository.countVerified();
//
//        statTotal.setText(String.valueOf(total));
//        statAdmins.setText(String.valueOf(admins));
//        statUsers.setText(String.valueOf(regular));
//        statVerified.setText(String.valueOf(verified));
//
//        // Charts
//        chartRoles.getData().setAll(
//                new PieChart.Data("Utilisateurs", regular),
//                new PieChart.Data("Administrateurs", admins)
//        );
//        chartVerified.getData().setAll(
//                new PieChart.Data("Vérifiés", verified),
//                new PieChart.Data("Non vérifiés", total - verified)
//        );
//    }
//
//    // ── Handlers ──────────────────────────────────────────
//
//    @FXML
//    private void handleNewUser() {
//        SessionManager.getInstance().setPendingUser(null); // null = create mode
//        SceneManager.navigateTo(Routes.ADMIN_USER_NEW);
//    }
//
//    private void handleEdit(User user) {
//        SessionManager.getInstance().setPendingUser(user);
//        SceneManager.navigateTo(Routes.ADMIN_USER_EDIT);
//    }
//
//    private void handleView(User user) {
//        SessionManager.getInstance().setPendingUser(user);
//        SceneManager.navigateTo(Routes.ADMIN_USER_SHOW);
//    }
//
//    private void handleDelete(User user) {
//        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
//        confirm.setTitle("Supprimer l'utilisateur");
//        confirm.setHeaderText("Supprimer " + user.getUsername() + " ?");
//        confirm.setContentText("Cette action est irréversible.");
//        confirm.showAndWait().ifPresent(btn -> {
//            if (btn == ButtonType.OK) {
//                userRepository.delete(user.getId());
//                allUsers.remove(user);
//                // Refresh stats
//                loadData();
//                showSuccess("Utilisateur supprimé avec succès.");
//            }
//        });
//    }
//
//    @FXML
//    private void handleExportCsv() {
//        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
//        chooser.setTitle("Exporter CSV");
//        chooser.setInitialFileName("users_export.csv");
//        chooser.getExtensionFilters().add(
//                new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv"));
//
//        java.io.File file = chooser.showSaveDialog(
//                userTable.getScene().getWindow());
//        if (file == null) return;
//
//        try (FileWriter fw = new FileWriter(file)) {
//            fw.write("ID;Email;Username;Address;Telephone;Role;Verified\n");
//            for (User u : filteredUsers) {
//                fw.write(String.join(";",
//                        String.valueOf(u.getId()),
//                        u.getEmail(),
//                        u.getUsername(),
//                        u.getAddress()   != null ? u.getAddress()   : "",
//                        u.getTelephone() != null ? u.getTelephone() : "",
//                        u.getRoles().name(),
//                        u.isVerified() ? "Oui" : "Non"
//                ) + "\n");
//            }
//            showSuccess("Export CSV réussi : " + file.getName());
//        } catch (IOException e) {
//            showSuccess("Erreur export : " + e.getMessage());
//        }
//    }
//
//    private void showSuccess(String msg) {
//        successLabel.setText("✅ " + msg);
//        successLabel.setVisible(true);
//        successLabel.setManaged(true);
//    }
//}