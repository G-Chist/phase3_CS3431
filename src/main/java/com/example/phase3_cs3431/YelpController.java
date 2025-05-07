package com.example.phase3_cs3431;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
    @FXML private ComboBox<String> cityComboBox;
    @FXML private Button filterButton;
    @FXML private ListView<String> categoryList;
    @FXML private ListView<String> attributeList;
    @FXML private Button searchButton;
    @FXML private TableView<Business> businessTable;
    @FXML private Label resultsFoundLabel;
    @FXML private TableColumn<Business, String> nameColumn;
    @FXML private TableColumn<Business, String> addressColumn;
    @FXML private TableColumn<Business, String> cityColumn;
    @FXML private TableColumn<Business, String> starsColumn;
    @FXML private TableColumn<Business, String> tipsColumn;
    @FXML private TableColumn<Business, String> latitudeColumn;
    @FXML private TableColumn<Business, String> longitudeColumn;


    @FXML void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        starsColumn.setCellValueFactory(new PropertyValueFactory<>("starRating"));
        tipsColumn.setCellValueFactory(new PropertyValueFactory<>("numTip"));
        latitudeColumn.setCellValueFactory(new PropertyValueFactory<>("latitude"));
        longitudeColumn.setCellValueFactory(new PropertyValueFactory<>("longitude"));

        updateStates();
        categoryList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        categoryList.setItems(FXCollections.observableArrayList());
        attributeList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        attributeList.setItems(FXCollections.observableArrayList());
        stateComboBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldState, newState) -> {
                    if (newState != null)
                        updateCities(newState); // Update cities list
                });
        cityComboBox.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldCity, newCity) -> {
                    if (newCity != null) {
                        updateAttributes(stateComboBox.getSelectionModel().getSelectedItem(), newCity);
                        updateCategories(stateComboBox.getSelectionModel().getSelectedItem(), newCity);
                    }
                });
        filterButton.setOnAction(event->{updateCategories(stateComboBox.getSelectionModel().getSelectedItem(), cityComboBox.getSelectionModel().getSelectedItem());});
        searchButton.setOnAction(event->{searchBusinesses();});
        businessTable.setOnMouseClicked(event -> {
           if (event.getClickCount() == 2) {
               Business selected = businessTable.getSelectionModel().getSelectedItem();
               if (selected != null) {
                   loadBusinessPage(selected);
               }
           }
        });
    }

    private void loadBusinessPage(Business selected) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(YelpApplication.class.getResource("businessDetails.fxml"));
            Parent root = fxmlLoader.load();
            BusinessDetailsController controller = fxmlLoader.getController();
            //attributesBusinessList.setItems(FXCollections.observableArrayList());

            ObservableList<Business> businesses = FXCollections.observableArrayList(
                    getSimilarBusinesses(selected)
            );
            ObservableList<String> categories = FXCollections.observableArrayList(
                    getDetailsCategories(selected)
            );
            ObservableList<String> attributes = FXCollections.observableArrayList(
                    getDetailsAttributes(selected)
            );
            controller.initData(selected.getName(), businesses, categories, attributes);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(businessTable.getScene().getWindow());
            dialog.setTitle("Business Details");

            Scene scene = new Scene(root, 700, 600);
            scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private List<Business> getSimilarBusinesses(Business selected) {

        List<Business> res = new ArrayList<>();

        String count_categories_function = "CREATE OR REPLACE FUNCTION\n" +
                "    count_categories(b1_id VARCHAR(22),b2_id VARCHAR(22)) RETURNS INT AS '\n" +
                "    DECLARE\n" +
                "        commonCategories INT;\n" +
                "    BEGIN\n" +
                "        WITH categories AS (SELECT categoryname\n" +
                "                            FROM category\n" +
                "                            WHERE business_id = b1_id\n" +
                "                            INTERSECT\n" +
                "                            SELECT categoryname\n" +
                "                            FROM category\n" +
                "                            WHERE business_id = b2_id)\n" +
                "        SELECT count(categories.categoryname)\n" +
                "        INTO commonCategories\n" +
                "        FROM categories;\n" +
                "        RETURN commonCategories;\n" +
                "    END;\n" +
                "' LANGUAGE plpgsql;";

        String geodistance_function = "CREATE OR REPLACE FUNCTION geodistance(\n" +
                "    lat_a DOUBLE PRECISION,\n" +
                "    lng_a DOUBLE PRECISION,\n" +
                "    lat_b DOUBLE PRECISION,\n" +
                "    lng_b DOUBLE PRECISION\n" +
                ") RETURNS DOUBLE PRECISION AS '\n" +
                "DECLARE\n" +
                "    radius_earth_km CONSTANT DOUBLE PRECISION := 6371.0;\n" +
                "    delta_lat_rad DOUBLE PRECISION;\n" +
                "    delta_lng_rad DOUBLE PRECISION;\n" +
                "    a DOUBLE PRECISION;\n" +
                "    c DOUBLE PRECISION;\n" +
                "BEGIN\n" +
                "    -- Convert delta values to radians\n" +
                "    delta_lat_rad := radians(lat_b - lat_a);\n" +
                "    delta_lng_rad := radians(lng_b - lng_a);\n" +
                "\n" +
                "    -- Apply the haversine formula\n" +
                "    a := sin(delta_lat_rad / 2)^2\n" +
                "        + cos(radians(lat_a)) * cos(radians(lat_b))\n" +
                "             * sin(delta_lng_rad / 2)^2;\n" +
                "\n" +
                "    c := 2 * atan2(sqrt(a), sqrt(1 - a));\n" +
                "\n" +
                "    RETURN radius_earth_km * c / 1.609344; -- convert from km to miles\n" +
                "END;\n" +
                "' LANGUAGE plpgsql;";

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {

            // First create the functions separately
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(geodistance_function); // does not return a ResultSet
                stmt.execute(count_categories_function);
            }

            // Now run the SELECT that returns data
            String stateQuery = """
                WITH myBusiness AS (SELECT '""";

            stateQuery = stateQuery.concat(selected.getId());
            stateQuery = stateQuery.concat("'as business_id)");
            stateQuery = stateQuery.concat("""
        SELECT b.business_id, b.name, b.city, b.zip_code,
               b2.business_id as b2business_id, b2.name as b2name,
               b2.street_address as b2street_address, b2.city as b2city, b2.state as b2state,
               b2.zip_code as b2zip_code, b2.latitude as b2latitude, b2.longitude as b2longitude,
               b2.starRating as b2starRating, b2.num_tip as b2num_tip, b2.is_open as b2is_open,
               count_categories(b.business_id, b2.business_id) AS rank
        FROM business b
        JOIN myBusiness ON b.business_id = myBusiness.business_id
        INNER JOIN business b2 ON b.zip_code = b2.zip_code
        WHERE 20 > geodistance(b2.latitude, b2.longitude, b.latitude, b.longitude)
          AND 0 < count_categories(myBusiness.business_id, b2.business_id)
          AND b2.business_id <> b.business_id
        ORDER BY count_categories(b.business_id, b2.business_id) DESC
        LIMIT 20;
    """);

            System.out.println(stateQuery);


            try (PreparedStatement ps = conn.prepareStatement(stateQuery)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    res.add(new Business(
                            rs.getString("b2business_id"),
                            rs.getString("b2name"),
                            rs.getString("b2street_address"),
                            rs.getString("b2city"),
                            rs.getString("b2state"),
                            rs.getInt("b2zip_code"),
                            rs.getDouble("b2latitude"),
                            rs.getDouble("b2longitude"),
                            rs.getInt("b2starRating"),
                            rs.getInt("b2num_tip"),
                            rs.getInt("b2is_open")
                    ));
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }


        return res;
    }

    private List<String> getDetailsCategories(Business selected){
        List<String> categories = new ArrayList<String>();

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {

            // Now run the SELECT that returns data
            String stateQuery = """
            SELECT categoryname
            FROM category
            NATURAL JOIN business
            WHERE business_id = '""";

            stateQuery = stateQuery.concat(selected.getId());
            stateQuery = stateQuery.concat("';");

            System.out.println(stateQuery);


            try (PreparedStatement ps = conn.prepareStatement(stateQuery)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    categories.add(rs.getString("categoryname"));
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return categories;
    }

    private List<String> getDetailsAttributes(Business selected){
        List<String> attributes = new ArrayList<String>();

        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {

            // Now run the SELECT that returns data
            String stateQuery = """
            SELECT attribute_name
            FROM attribute
            NATURAL JOIN business
            WHERE business_id = '""";
            stateQuery = stateQuery.concat(selected.getId());
            stateQuery = stateQuery.concat("';");

            System.out.println(stateQuery);


            try (PreparedStatement ps = conn.prepareStatement(stateQuery)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    attributes.add(rs.getString("attribute_name"));
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return attributes;
    }

    private void searchBusinesses(){
        String state = stateComboBox.getSelectionModel().getSelectedItem();
        String city = cityComboBox.getSelectionModel().getSelectedItem();
        List<String> cats = new ArrayList<>(categoryList.getSelectionModel().getSelectedItems());
        List<String> attrs = new ArrayList<>(attributeList.getSelectionModel().getSelectedItems());
        List<Business> results = queryBusinesses (state, city, cats, attrs);
        businessTable.setItems (FXCollections.observableArrayList (results));
    }

    private List<Business> queryBusinesses(String state, String city, List<String> categories, List<String> attrs) {
        List<Business> res = new ArrayList<>();

        // Build subquery to get businesses that match ALL attributes
        String attributeSubquery = """
        SELECT A.business_id
        FROM Attribute A
        WHERE 
    """;

        for (int i = 0; i < attrs.size(); i++) {
            attributeSubquery += "(A.attribute_name = '" + attrs.get(i) + "' AND A.attValue <> 'False') OR ";
        }
        if (!attrs.isEmpty()) {
            attributeSubquery = attributeSubquery.substring(0, attributeSubquery.length() - 4); // remove last ' OR '
        }

        if (!attrs.isEmpty()) {
            attributeSubquery += " GROUP BY A.business_id HAVING COUNT(*) = " + attrs.size();
        }

        // Build subquery to get businesses that match ALL categories
        String categorySubquery = """
            SELECT C.business_id
            FROM Category C
            WHERE 
        """;

        for (int i = 0; i < categories.size(); i++) {
            categorySubquery += "C.categoryName = '" + categories.get(i) + "' OR ";
        }
        if (!categories.isEmpty()) {
            categorySubquery = categorySubquery.substring(0, categorySubquery.length() - 4); // remove last ' OR '
            categorySubquery += " GROUP BY C.business_id HAVING COUNT(*) = " + categories.size();
        }

        // Query assembling
        String businessQuery = """
        SELECT DISTINCT
            B.business_id,
            B.name,
            B.street_address,
            B.city,
            B.state,
            B.zip_code,
            B.latitude,
            B.longitude,
            B.starRating,
            B.num_tip,
            B.is_open
        FROM business B
        WHERE B.state = ? AND B.city = ?
    """;

        if (!attrs.isEmpty()) {
            businessQuery += " AND B.business_id IN (" + attributeSubquery + ")";
        }
        if (!categories.isEmpty()) {
            businessQuery += " AND B.business_id IN (" + categorySubquery + ")";
        }

        businessQuery += " ORDER BY B.name";

        System.out.println("Final Query:\n" + businessQuery);

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(businessQuery)) {
            ps.setString(1, state);
            ps.setString(2, city);
            ResultSet rs = ps.executeQuery();
            int businessCounter = 0;
            while (rs.next()) {
                res.add(new Business(
                        rs.getString("business_id"),
                        rs.getString("name"),
                        rs.getString("street_address"),
                        rs.getString("city"),
                        rs.getString("state"),
                        rs.getInt("zip_code"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude"),
                        rs.getInt("starRating"),
                        rs.getInt("num_tip"),
                        rs.getInt("is_open")
                ));
                businessCounter++;
            }
            resultsFoundLabel.setText(businessCounter + " results found");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return res;
    }




    private void updateCategories(String state, String city) {
        if (state == null || city == null) {
            return;
        }

        ObservableList<String> categories = FXCollections.observableArrayList();
        String query = """
        SELECT DISTINCT Category.categoryName
        FROM Category
        JOIN business ON business.business_id = Category.business_id
        WHERE business.state = ? AND business.city = ?
        ORDER BY Category.categoryName
    """;

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
        } catch (SQLException ex) {
            ex.printStackTrace();
            return;
        }

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, state);
            ps.setString(2, city);
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

    private void updateCities(String selectedState) {
        ObservableList<String> cities = FXCollections.observableArrayList();
        String cityQuery = """
            SELECT DISTINCT city
            FROM business
            WHERE state = ?
            ORDER BY city
        """;

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(cityQuery)) {
            ps.setString(1, selectedState); // set the state parameter
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cities.add(rs.getString("city"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        cityComboBox.setItems(cities);

        try { connection.close(); } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void updateAttributes(String selectedState, String selectedCity) {
        ObservableList<String> allAttributes = FXCollections.observableArrayList();
        ObservableList<String> selectedAttributes = FXCollections.observableArrayList();

        String attrQuery = """
            SELECT DISTINCT A.attribute_name, A.attValue
            FROM Attribute A
            JOIN business B ON A.business_id = B.business_id
            WHERE B.state = ? AND B.city = ? AND A.attValue <> 'False'
        """;

        try {
            connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        try (PreparedStatement ps = connection.prepareStatement(attrQuery)) {
            ps.setString(1, selectedState);
            ps.setString(2, selectedCity);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String attrName = rs.getString("attribute_name");
                String attValue = rs.getString("attValue");

                if (!allAttributes.contains(attrName)) {
                    allAttributes.add(attrName);
                }

                if ("True".equalsIgnoreCase(attValue) && !selectedAttributes.contains(attrName)) {
                    selectedAttributes.add(attrName);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        attributeList.setItems(allAttributes);
        attributeList.getSelectionModel().clearSelection();

        for (String attr : selectedAttributes) {
            attributeList.getSelectionModel().select(attr);
        }

        try {
            connection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }



}
