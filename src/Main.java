import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import database.SchemaInitializer;
import ui.FxmlView;

// JavaFX application entry point.
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        SchemaInitializer.ensureSchema();

        FXMLLoader loader = new FXMLLoader(FxmlView.fxmlUrl("Login.fxml"));
        Parent root = loader.load();

        Screen screen = Screen.getPrimary();
        double width = screen.getVisualBounds().getWidth();
        double height = screen.getVisualBounds().getHeight();

        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(FxmlView.stylesheet("nbda-modern.css"));

        primaryStage.setTitle("NBDA - DOH-Compliant Blood Bank Management System");
        primaryStage.setScene(scene);
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreen(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
