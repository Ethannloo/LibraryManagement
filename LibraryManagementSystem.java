import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.*;

public class LibraryManagementSystem extends Application {
    private Connection connection;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        connectToDatabase();
        createTables();

        // GUI Components
        Label welcomeLabel = new Label("Welcome to the Library Management System");

        Button searchButton = new Button("Search Books");
        searchButton.setOnAction(e -> showSearchWindow());

        Button borrowButton = new Button("Borrow Book");
        borrowButton.setOnAction(e -> showBorrowWindow());

        Button returnButton = new Button("Return Book");
        returnButton.setOnAction(e -> showReturnWindow());

        VBox layout = new VBox(10, welcomeLabel, searchButton, borrowButton, returnButton);
        Scene scene = new Scene(layout, 400, 200);

        primaryStage.setTitle("Library Management System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void connectToDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:library.db");
            System.out.println("Connected to the database.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTables() {
        try (Statement statement = connection.createStatement()) {
            String booksTable = "CREATE TABLE IF NOT EXISTS books (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT," +
                    "author TEXT," +
                    "available INTEGER DEFAULT 1)";

            String usersTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT)";

            String transactionsTable = "CREATE TABLE IF NOT EXISTS transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "book_id INTEGER," +
                    "borrow_date TEXT," +
                    "return_date TEXT)";

            statement.execute(booksTable);
            statement.execute(usersTable);
            statement.execute(transactionsTable);
            System.out.println("Tables created or verified.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showSearchWindow() {
        Stage searchStage = new Stage();
        searchStage.setTitle("Search Books");

        TextField searchField = new TextField();
        searchField.setPromptText("Enter book title or author");
        Button searchButton = new Button("Search");

        ListView<String> resultsList = new ListView<>();

        searchButton.setOnAction(e -> {
            String keyword = searchField.getText();
            resultsList.getItems().clear();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM books WHERE title LIKE ? OR author LIKE ?")) {
                statement.setString(1, "%" + keyword + "%");
                statement.setString(2, "%" + keyword + "%");

                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String bookDetails = resultSet.getInt("id") + ": " +
                            resultSet.getString("title") + " by " + resultSet.getString("author");
                    resultsList.getItems().add(bookDetails);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        VBox layout = new VBox(10, searchField, searchButton, resultsList);
        Scene scene = new Scene(layout, 400, 300);
        searchStage.setScene(scene);
        searchStage.show();
    }

    private void showBorrowWindow() {
        Stage borrowStage = new Stage();
        borrowStage.setTitle("Borrow Book");

        TextField userIdField = new TextField();
        userIdField.setPromptText("Enter user ID");

        TextField bookIdField = new TextField();
        bookIdField.setPromptText("Enter book ID");

        Button borrowButton = new Button("Borrow");

        Label statusLabel = new Label();

        borrowButton.setOnAction(e -> {
            int userId = Integer.parseInt(userIdField.getText());
            int bookId = Integer.parseInt(bookIdField.getText());

            try (PreparedStatement checkAvailability = connection.prepareStatement(
                    "SELECT available FROM books WHERE id = ?")) {
                checkAvailability.setInt(1, bookId);
                ResultSet resultSet = checkAvailability.executeQuery();

                if (resultSet.next() && resultSet.getInt("available") == 1) {
                    try (PreparedStatement borrowStatement = connection.prepareStatement(
                            "INSERT INTO transactions (user_id, book_id, borrow_date) VALUES (?, ?, datetime('now'))")) {
                        borrowStatement.setInt(1, userId);
                        borrowStatement.setInt(2, bookId);
                        borrowStatement.executeUpdate();
                    }

                    try (PreparedStatement updateBook = connection.prepareStatement(
                            "UPDATE books SET available = 0 WHERE id = ?")) {
                        updateBook.setInt(1, bookId);
                        updateBook.executeUpdate();
                    }

                    statusLabel.setText("Book borrowed successfully!");
                } else {
                    statusLabel.setText("Book is not available.");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                statusLabel.setText("An error occurred.");
            }
        });

        VBox layout = new VBox(10, userIdField, bookIdField, borrowButton, statusLabel);
        Scene scene = new Scene(layout, 400, 200);
        borrowStage.setScene(scene);
        borrowStage.show();
    }

    private void showReturnWindow() {
        Stage returnStage = new Stage();
        returnStage.setTitle("Return Book");

        TextField transactionIdField = new TextField();
        transactionIdField.setPromptText("Enter transaction ID");

        Button returnButton = new Button("Return");

        Label statusLabel = new Label();

        returnButton.setOnAction(e -> {
            int transactionId = Integer.parseInt(transactionIdField.getText());

            try (PreparedStatement transactionStatement = connection.prepareStatement(
                    "SELECT book_id FROM transactions WHERE id = ? AND return_date IS NULL")) {
                transactionStatement.setInt(1, transactionId);
                ResultSet resultSet = transactionStatement.executeQuery();

                if (resultSet.next()) {
                    int bookId = resultSet.getInt("book_id");

                    try (PreparedStatement returnTransaction = connection.prepareStatement(
                            "UPDATE transactions SET return_date = datetime('now') WHERE id = ?")) {
                        returnTransaction.setInt(1, transactionId);
                        returnTransaction.executeUpdate();
                    }

                    try (PreparedStatement updateBook = connection.prepareStatement(
                            "UPDATE books SET available = 1 WHERE id = ?")) {
                        updateBook.setInt(1, bookId);
                        updateBook.executeUpdate();
                    }

                    statusLabel.setText("Book returned successfully!");
                } else {
                    statusLabel.setText("Transaction not found or already returned.");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                statusLabel.setText("An error occurred.");
            }
        });

        VBox layout = new VBox(10, transactionIdField, returnButton, statusLabel);
        Scene scene = new Scene(layout, 400, 200);
        returnStage.setScene(scene);
        returnStage.show();
    }

    @Override
    public void stop() {
        try {
            if (connection != null) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}