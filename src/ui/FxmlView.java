package ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

// File-based FXML/CSS lookup for this non-Maven JavaFX project.
public class FxmlView {
    private FxmlView() {
    }

    public static URL fxmlUrl(String fileName) {
        return toUrl(Paths.get("assets", "fxml", fileName));
    }

    public static String stylesheet(String fileName) {
        return toUrl(Paths.get("assets", "css", fileName)).toExternalForm();
    }

    private static URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid UI asset path: " + path, e);
        }
    }
}
