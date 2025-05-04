package com.example.phase3_cs3431;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;

public class BusinessDetailsController {
    private static final Dotenv dotenv = Dotenv.load();
    private static final String JDBC_URL = dotenv.get("JDBC_URL");
    private static final String JDBC_USER = dotenv.get("JDBC_USER");
    private static final String JDBC_PASSWORD = dotenv.get("JDBC_PASSWORD");
    private Connection connection;

    @FXML private Label titleLabel;
    @FXML private ListView<String> categoryBusinessList;
    @FXML private TableView<Business> similarBusinesses;
    @FXML private TableColumn<Business, String> rankColumn;
    @FXML private TableColumn<Business, String> nameColumn;
    @FXML private TableColumn<Business, String> addressColumn;
    @FXML private TableColumn<Business, String> idColumn;
    @FXML private TableColumn<Business, String> starsColumn;
    @FXML private TableColumn<Business, String> latColumn;
    @FXML private TableColumn<Business, String> longColumn;

    @FXML void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        starsColumn.setCellValueFactory(new PropertyValueFactory<>("starRating"));
        latColumn.setCellValueFactory(new PropertyValueFactory<>("latitude"));
        longColumn.setCellValueFactory(new PropertyValueFactory<>("longitude"));
        categoryBusinessList.setItems(FXCollections.observableArrayList());
    }

    public void initData (String businessName, ObservableList<Business> similars, ObservableList<String> categories) {
        System.out.println("initData called!");
        titleLabel.setText("Similar to: " + businessName);
        System.out.println("Label text set: " + businessName);
        similarBusinesses.setItems(similars);
        categoryBusinessList.setItems(categories);
    }
}