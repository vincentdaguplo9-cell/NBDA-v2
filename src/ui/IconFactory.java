package ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.lang.reflect.Constructor;

// Uses FontAwesomeFX when present and falls back to simple vector icons when the jar is absent.
public class IconFactory {
    private static final double BASE_ICON_SIZE = 18.0;

    private IconFactory() {
    }

    public static void install(Button button, String fontAwesomeName, String fallbackText) {
        String badgeStyleClass = "button-icon-badge";
        String iconColor = "#FFFFFF";
        if (button.getStyleClass().contains("nav-button")) {
            badgeStyleClass = "nav-icon-badge";
        } else if (button.getStyleClass().contains("secondary-button")) {
            badgeStyleClass = "secondary-button-icon-badge";
            iconColor = "#1A5276";
        }
        button.setGraphic(createBadge(fontAwesomeName, fallbackText, badgeStyleClass, iconColor, 14));
    }

    public static StackPane createBadge(String fontAwesomeName, String fallbackText, String badgeStyleClass, String iconColor, double iconSize) {
        StackPane badge = new StackPane();
        badge.getStyleClass().add(badgeStyleClass);
        badge.getChildren().add(createIcon(fontAwesomeName, fallbackText, iconColor, iconSize));
        return badge;
    }

    public static Node createIcon(String fontAwesomeName, String fallbackText, String iconColor, double iconSize) {
        Color color = parseColor(iconColor);
        try {
            Class<?> enumClass = Class.forName("de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon");
            @SuppressWarnings("unchecked")
            Object enumValue = Enum.valueOf((Class<Enum>) enumClass.asSubclass(Enum.class), fontAwesomeName);
            Class<?> viewClass = Class.forName("de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView");
            Constructor<?> ctor = viewClass.getConstructor(enumClass);
            Object icon = ctor.newInstance(enumValue);
            if (icon instanceof Node) {
                Node node = (Node) icon;
                node.setStyle("-fx-fill: " + toHex(color) + "; -fx-font-size: " + iconSize + "px;");
                return node;
            }
        } catch (Exception ignored) {
        }

        return buildFallbackIcon(fontAwesomeName, fallbackText, color, iconSize);
    }

    private static Node buildFallbackIcon(String fontAwesomeName, String fallbackText, Color color, double iconSize) {
        Node node;
        switch (fontAwesomeName) {
            case "TINT":
                node = buildTintIcon(color);
                break;
            case "CHECK_CIRCLE":
                node = buildCheckCircleIcon(color);
                break;
            case "EXCLAMATION_TRIANGLE":
                node = buildExclamationTriangleIcon(color);
                break;
            case "USER_PLUS":
                node = buildUserPlusIcon(color);
                break;
            case "SEARCH":
                node = buildSearchIcon(color);
                break;
            case "DASHBOARD":
                node = buildDashboardIcon(color);
                break;
            case "BOOK":
                node = buildBookIcon(color);
                break;
            case "SIGN_OUT":
                node = buildSignOutIcon(color);
                break;
            case "SIGN_IN":
                node = buildSignInIcon(color);
                break;
            case "EDIT":
                node = buildEditIcon(color);
                break;
            case "LIST":
                node = buildListIcon(color);
                break;
            case "USER":
                node = buildUserIcon(color);
                break;
            case "ANCHOR_DROP":
                node = buildAnchorDropIcon(color);
                break;
            default:
                node = buildFallbackLabel(fallbackText, color);
                break;
        }
        node.setScaleX(iconSize / BASE_ICON_SIZE);
        node.setScaleY(iconSize / BASE_ICON_SIZE);
        return node;
    }

    private static Node buildAnchorDropIcon(Color color) {
        Group group = new Group();

        SVGPath shield = new SVGPath();
        shield.setContent("M9 2 L14 4 L14 9 C14 13 9 16 9 16 C9 16 4 13 4 9 L4 4 Z");
        shield.setFill(Color.TRANSPARENT);
        shield.setStroke(color);
        shield.setStrokeWidth(1.8);

        SVGPath drop = new SVGPath();
        drop.setContent("M9 6 C7.5 8 6 9.5 6 11 A3 3 0 0 0 12 11 C12 9.5 10.5 8 9 6 Z");
        drop.setFill(color);

        group.getChildren().addAll(shield, drop);
        return group;
    }

    private static Node buildTintIcon(Color color) {
        SVGPath drop = new SVGPath();
        drop.setContent("M9 1 C7 4 4 7.2 4 11 a5 5 0 0 0 10 0 C14 7.2 11 4 9 1 Z");
        drop.setFill(color);
        return drop;
    }

    private static Node buildCheckCircleIcon(Color color) {
        Circle circle = new Circle(9, 9, 7);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(color);
        circle.setStrokeWidth(1.8);

        Polyline check = new Polyline(5.4, 9.5, 8.0, 12.0, 13.0, 6.7);
        check.setStroke(color);
        check.setStrokeWidth(2.0);
        check.setStrokeLineCap(StrokeLineCap.ROUND);
        check.setStrokeLineJoin(StrokeLineJoin.ROUND);
        check.setFill(Color.TRANSPARENT);
        return new Group(circle, check);
    }

    private static Node buildExclamationTriangleIcon(Color color) {
        Polygon triangle = new Polygon(9.0, 2.0, 16.0, 15.0, 2.0, 15.0);
        triangle.setFill(Color.TRANSPARENT);
        triangle.setStroke(color);
        triangle.setStrokeWidth(1.8);

        Line stem = new Line(9.0, 6.0, 9.0, 10.4);
        stem.setStroke(color);
        stem.setStrokeWidth(2.0);
        stem.setStrokeLineCap(StrokeLineCap.ROUND);

        Circle dot = new Circle(9.0, 13.0, 1.0, color);
        return new Group(triangle, stem, dot);
    }

    private static Node buildUserPlusIcon(Color color) {
        Circle head = new Circle(7.0, 5.0, 3.1);
        head.setFill(Color.TRANSPARENT);
        head.setStroke(color);
        head.setStrokeWidth(1.8);

        SVGPath shoulders = new SVGPath();
        shoulders.setContent("M2.5 15 C3.8 11.8 6.0 10.4 8.5 10.4 C11.0 10.4 13.1 11.8 14.5 15");
        shoulders.setFill(Color.TRANSPARENT);
        shoulders.setStroke(color);
        shoulders.setStrokeWidth(1.8);
        shoulders.setStrokeLineCap(StrokeLineCap.ROUND);

        Line horizontal = new Line(12.5, 6.0, 17.0, 6.0);
        horizontal.setStroke(color);
        horizontal.setStrokeWidth(1.8);
        horizontal.setStrokeLineCap(StrokeLineCap.ROUND);

        Line vertical = new Line(14.8, 3.8, 14.8, 8.2);
        vertical.setStroke(color);
        vertical.setStrokeWidth(1.8);
        vertical.setStrokeLineCap(StrokeLineCap.ROUND);
        return new Group(head, shoulders, horizontal, vertical);
    }

    private static Node buildSearchIcon(Color color) {
        Circle circle = new Circle(7.0, 7.0, 4.6);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(color);
        circle.setStrokeWidth(1.8);

        Line handle = new Line(10.6, 10.6, 15.0, 15.0);
        handle.setStroke(color);
        handle.setStrokeWidth(2.0);
        handle.setStrokeLineCap(StrokeLineCap.ROUND);
        return new Group(circle, handle);
    }

    private static Node buildDashboardIcon(Color color) {
        Rectangle topLeft = buildFilledRect(2.0, 2.0, 5.2, 5.2, color);
        Rectangle topRight = buildFilledRect(10.8, 2.0, 5.2, 5.2, color);
        Rectangle bottomLeft = buildFilledRect(2.0, 10.8, 5.2, 5.2, color);
        Rectangle bottomRight = buildFilledRect(10.8, 10.8, 5.2, 5.2, color);
        return new Group(topLeft, topRight, bottomLeft, bottomRight);
    }

    private static Node buildBookIcon(Color color) {
        Rectangle cover = new Rectangle(3.0, 2.5, 11.5, 13.0);
        cover.setArcWidth(2.0);
        cover.setArcHeight(2.0);
        cover.setFill(Color.TRANSPARENT);
        cover.setStroke(color);
        cover.setStrokeWidth(1.6);

        Line spine = new Line(8.5, 2.8, 8.5, 15.2);
        spine.setStroke(color);
        spine.setStrokeWidth(1.5);
        return new Group(cover, spine);
    }

    private static Node buildSignOutIcon(Color color) {
        Line bar = new Line(3.0, 9.0, 9.0, 9.0);
        bar.setStroke(color);
        bar.setStrokeWidth(1.8);
        bar.setStrokeLineCap(StrokeLineCap.ROUND);

        Line shaft = new Line(9.0, 9.0, 14.5, 9.0);
        shaft.setStroke(color);
        shaft.setStrokeWidth(1.8);
        shaft.setStrokeLineCap(StrokeLineCap.ROUND);

        Polyline arrow = new Polyline(11.7, 6.3, 14.7, 9.0, 11.7, 11.7);
        arrow.setStroke(color);
        arrow.setStrokeWidth(1.8);
        arrow.setStrokeLineCap(StrokeLineCap.ROUND);
        arrow.setStrokeLineJoin(StrokeLineJoin.ROUND);
        arrow.setFill(Color.TRANSPARENT);
        return new Group(bar, shaft, arrow);
    }

    private static Node buildSignInIcon(Color color) {
        Line shaft = new Line(3.5, 9.0, 13.5, 9.0);
        shaft.setStroke(color);
        shaft.setStrokeWidth(1.8);
        shaft.setStrokeLineCap(StrokeLineCap.ROUND);

        Polyline arrow = new Polyline(10.6, 6.0, 13.8, 9.0, 10.6, 12.0);
        arrow.setStroke(color);
        arrow.setStrokeWidth(1.8);
        arrow.setStrokeLineCap(StrokeLineCap.ROUND);
        arrow.setStrokeLineJoin(StrokeLineJoin.ROUND);
        arrow.setFill(Color.TRANSPARENT);
        return new Group(shaft, arrow);
    }

    private static Node buildEditIcon(Color color) {
        Polygon pencil = new Polygon(4.0, 13.0, 5.6, 14.6, 13.8, 6.4, 12.2, 4.8);
        pencil.setFill(Color.TRANSPARENT);
        pencil.setStroke(color);
        pencil.setStrokeWidth(1.6);

        Line tip = new Line(13.2, 5.4, 15.3, 3.3);
        tip.setStroke(color);
        tip.setStrokeWidth(1.8);
        tip.setStrokeLineCap(StrokeLineCap.ROUND);

        Line base = new Line(3.2, 15.3, 6.2, 15.3);
        base.setStroke(color);
        base.setStrokeWidth(1.8);
        base.setStrokeLineCap(StrokeLineCap.ROUND);
        return new Group(pencil, tip, base);
    }

    private static Node buildListIcon(Color color) {
        Circle bulletOne = new Circle(4.0, 4.5, 1.0, color);
        Circle bulletTwo = new Circle(4.0, 9.0, 1.0, color);
        Circle bulletThree = new Circle(4.0, 13.5, 1.0, color);

        Line lineOne = buildLine(7.0, 4.5, 15.0, 4.5, color);
        Line lineTwo = buildLine(7.0, 9.0, 15.0, 9.0, color);
        Line lineThree = buildLine(7.0, 13.5, 15.0, 13.5, color);
        return new Group(bulletOne, bulletTwo, bulletThree, lineOne, lineTwo, lineThree);
    }

    private static Node buildUserIcon(Color color) {
        Circle head = new Circle(9.0, 6.0, 3.0);
        head.setFill(Color.TRANSPARENT);
        head.setStroke(color);
        head.setStrokeWidth(1.8);

        SVGPath shoulders = new SVGPath();
        shoulders.setContent("M3.3 15 C4.7 11.7 6.8 10.4 9.0 10.4 C11.2 10.4 13.3 11.7 14.7 15");
        shoulders.setFill(Color.TRANSPARENT);
        shoulders.setStroke(color);
        shoulders.setStrokeWidth(1.8);
        shoulders.setStrokeLineCap(StrokeLineCap.ROUND);
        return new Group(head, shoulders);
    }

    private static Rectangle buildFilledRect(double x, double y, double width, double height, Color color) {
        Rectangle rectangle = new Rectangle(x, y, width, height);
        rectangle.setArcWidth(2.0);
        rectangle.setArcHeight(2.0);
        rectangle.setFill(color);
        return rectangle;
    }

    private static Line buildLine(double startX, double startY, double endX, double endY, Color color) {
        Line line = new Line(startX, startY, endX, endY);
        line.setStroke(color);
        line.setStrokeWidth(1.8);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        return line;
    }

    private static Node buildFallbackLabel(String fallbackText, Color color) {
        Label fallback = new Label(fallbackText);
        fallback.setTextFill(color);
        fallback.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        return fallback;
    }

    private static Color parseColor(String iconColor) {
        try {
            return Color.web(iconColor);
        } catch (Exception ex) {
            return Color.WHITE;
        }
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }
}
