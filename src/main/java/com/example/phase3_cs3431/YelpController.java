package com.example.phase3_cs3431;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
    @FXML private TableColumn<Business, String> nameColumn;
    @FXML private TableColumn<Business, String> addressColumn;
    @FXML private TableColumn<Business, String> cityColumn;

    @FXML void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));


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
        searchButton.setOnAction(event->{searchBusinesses();});
    }

    private void searchBusinesses(){
        String state = stateComboBox.getSelectionModel().getSelectedItem();
        List<String> cats = new ArrayList<>(categoryList.getSelectionModel().getSelectedItems());
        List<Business> results = queryBusinesses (state, cats);
        businessTable.setItems (FXCollections.observableArrayList (results));
    }

    private List<Business> queryBusinesses(String state, List<String> categories) {
        List<Business> res = new ArrayList<>();

        String businessQuery = """
        SELECT business_id, name, street_address, city, latitude, longitude, starRating, num_tip
        FROM business
        WHERE business.state = ?
    """;

        // You can iterate over all selected categories as follows. You should add more conditions to your query dynamically.
        businessQuery = businessQuery.concat(" AND business_id IN (SELECT C1.business_id FROM Category C1 WHERE ");
        for (String cat : categories) {
            businessQuery = businessQuery.concat("C1.categoryName = '");
            businessQuery = businessQuery.concat(cat);
            businessQuery = businessQuery.concat("' OR ");
        }
        businessQuery = businessQuery.concat(" FALSE)");
        // Final results looks like the following:
        /*
            SELECT business_id, name, street_address, city, latitude, longitude, starRating, num_tip
            FROM business
            WHERE business.state = {State}
            AND business_id IN (SELECT C1.business_id FROM Category C1 WHERE
                C1.categoryName = {Category1} OR
                C1.categoryName = {Category2} OR
                ...
                FALSE) -- A OR FALSE = A by definition
         */

        businessQuery = businessQuery.concat(" ORDER BY name");
        System.out.println(businessQuery);

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(businessQuery)) {
            int count = 1;
            ps.setString(count, state);
            count++;
        /*
        for (String cat: categories){
            ps.setString(count, cat);
            count++;
        }
        */
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                res.add(new Business(
                        rs.getString("business_id"),
                        rs.getString("name"),
                        rs.getString("street_address"),
                        rs.getString("city")
                ));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try { connection.close(); } catch (SQLException e) { e.printStackTrace(); }

        return res;
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
