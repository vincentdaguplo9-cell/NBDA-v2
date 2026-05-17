import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import ui.FxmlView;

// JavaFX application entry point.
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(FxmlView.fxmlUrl("Login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(FxmlView.stylesheet("style.css"));

        primaryStage.setTitle("NBDA - DOH-Compliant Blood Bank Management System");
        primaryStage.setScene(scene);
        primaryStage.setFullScreen(true);
        primaryStage.setFullScreenExitHint("Press ESC to exit fullscreen");
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Ensure database schema is up-to-date before launching UI.
        try {
            database.SchemaInitializer.ensureSchema();
        } catch (Exception e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
        launch(args);
    }
}
