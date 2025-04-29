package com.example.phase3_cs3431;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.sql.*;

public class YelpController {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String JDBC_URL = dotenv.get("JDBC_URL");
    private static final String JDBC_USER = dotenv.get("JDBC_USER");
    private static final String JDBC_PASSWORD = dotenv.get("JDBC_PASSWORD");
    private Connection connection;

    @FXML private Label searchText;
    @FXML private ComboBox<String> stateComboBox;
    @FXML private Button filterButton;
    @FXML private ListView<String> categoryList;
    @FXML private Button searchButton;
    @FXML private TableView<Business> businessTable;

    @FXML void initialize() {
        updateStates();
        categoryList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        categoryList.setItems(FXCollections.observableArrayList());
        stateComboBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldState, newState) -> {
                    if (newState != null)
                        updateCategories(newState);
                });
        filterButton.setOnAction(event->{updateCategories(stateComboBox.getSelectionModel().getSelectedItem());});
    }


    private void updateCategories(String state) {
        // String state = stateComboBox.getSelectionModel().getSelectedItem();
        if (state == null) {
            return;
        }

        ObservableList<String> categories = FXCollections.observableArrayList();
        String stateQuery = """
            SELECT DISTINCT Category.categoryName
            FROM Category
            JOIN business ON business.business_id = Category.business_id
            WHERE business.state = ?
            ORDER BY Category.categoryName
        """;

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(stateQuery)) {
            ps.setString(1, state);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                categories.add(rs.getString("categoryName"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        categoryList.setItems(categories);
    }


    private void updateStates() {
        ObservableList<String> states = FXCollections.observableArrayList();
        String stateQuery = """
            SELECT DISTINCT state
            FROM business
            ORDER BY state
        """;
        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(stateQuery)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                states.add(rs.getString("state"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        stateComboBox.setItems(states);

        try {connection.close();} catch (SQLException ex) {}
    }

}
