package main.java.example.wireplan;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.ArcType;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class HelloApplication extends Application {
    private boolean isDrawing = false;
    private double startX, startY;
    private String currentTool = "rectangle";
    private double totalWireLength = 0;
    private GraphicsContext gc;
    private boolean wireMode = false;
    private TextField lengthInput;
    private Label lengthLabel;
    private Button addLengthButton;
    int w =1250;
    int h = 800;
    private static Stack<Drawable> drawings = new Stack<>();
    private static Stack<List<Drawable>> drawingsHistory = new Stack<>();

    private boolean isPlacingElectricMeter = false;
    private boolean isPlacingBulbMeter = false;
    private boolean isPlacingRosetteMeter = false;

    Button electricMeterButton;
    Button bulbMeterButton;
    Button rosetteMeterButton;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Building Plan Drawer");

        BorderPane root = new BorderPane();

        Canvas canvas = new Canvas(w,h);
        gc = canvas.getGraphicsContext2D();
        drawCanvasBorder();

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setFullScreen(false);
        primaryStage.show();

        VBox toolBox = createToolBox(canvas); // Інструменти
        root.setLeft(toolBox);
        root.setCenter(canvas);

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (isPlacingElectricMeter) { //електрощитка
                double meterX = e.getX();
                double meterY = e.getY();
                ElectricMeterDrawable newMeter = new ElectricMeterDrawable(meterX, meterY, 40); // Розміри електрощитка
                drawings.add(newMeter); // Додавання до списку
                redrawAllShapes(); // Перемалювання всіх фігур на канвасі
                isPlacingElectricMeter = false; // Вимкнення режиму розміщення
            } else if (isPlacingBulbMeter) { // Логіка для лампочки
                double bulbX = e.getX();
                double bulbY = e.getY();
                LightBulbDrawable newBulb = new LightBulbDrawable(bulbX, bulbY, 20); // Розміри лампочки
                drawings.add(newBulb); // Додавання до списку
                redrawAllShapes(); // Перемалювання всіх фігур на канвасі
                isPlacingBulbMeter = false; // Вимкнення режиму розміщення
            } else if (isPlacingRosetteMeter) { // Логіка для розетки
                double socketX = e.getX();
                double socketY = e.getY();
                SocketDrawable newSocket = new SocketDrawable(socketX, socketY, 40, 30); // Розміри розетки
                drawings.add(newSocket); // Додавання до списку
                redrawAllShapes(); // Перемалювання всіх фігур на канвасі
                isPlacingRosetteMeter = false; // Вимкнення режиму розміщення
            } else {
                startX = e.getX();
                startY = e.getY();
                isDrawing = true;
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (isDrawing) {
                if (wireMode) {
                    drawPreviewWire(e.getX(), e.getY()); // Попередній перегляд дроту
                } else if (currentTool.equals("line")) {
                    drawPreviewLine(e.getX(), e.getY()); // Попередній перегляд лінії
                } else if (currentTool.equals("rectangle")) {
                    drawPreviewRectangle(e.getX(), e.getY()); // Попередній перегляд прямокутника
                } else {
                    drawPreviewShape(e.getX(), e.getY()); // Інші фігури
                }
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (isDrawing) {
                if (wireMode) {
                    drawWire(e.getX(), e.getY()); // Малювання дроту
                } else {
                    drawShape(e.getX(), e.getY()); // Малювання інших фігур
                }
                isDrawing = false;
                clearPreview();
                redrawAllShapes(); // Оновлення всіх фігур на канвасі
            }
        });

        scene.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode().toString().equals("Z")) {
                undoLastDrawing();
            }
        });

    }

    private void undoLastDrawing() {
        if (!drawingsHistory.isEmpty()) {
            // Отримання останнього елементу, який є списком, і призначення його новому стеку drawings
            List<Drawable> lastDrawings = drawingsHistory.pop();
            drawings = new Stack<>();
            drawings.addAll(lastDrawings);
            redrawAllShapes(); // Оновлюємо канвас
        }
    }


    private void drawCanvasBorder() {
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(3);
        gc.strokeRect(0, 0, w, h);
    }

    private VBox createToolBox(Canvas canvas) {
        VBox toolBox = new VBox();
        Button lineButton = new Button("Лінія");
        Button rectangleButton = new Button("Прямокутник");
        Button eraserButton = new Button("Гумка");
        Button switchModeButton = new Button("Перейти в режим дротів");

        lengthLabel = new Label("Довжина дроту: " + String.format("%.2f", totalWireLength) + " м");

        lineButton.setOnAction(e -> {
            currentTool = "line";
            wireMode = false;
        });

        rectangleButton.setOnAction(e -> {
            currentTool = "rectangle";
            wireMode = false;
        });

        eraserButton.setOnAction(e -> {
            currentTool = "eraser";
            wireMode = false;
        });

        switchModeButton.setOnAction(e -> {
            switchToWireMode(toolBox);
        });

        toolBox.getChildren().addAll(lineButton, rectangleButton, eraserButton, switchModeButton, lengthLabel);

        return toolBox;
    }

    private void createLengthInputAndButton(VBox toolBox) {
        lengthInput = new TextField();
        lengthInput.setPromptText("Введіть довжину для додавання");

        addLengthButton = new Button("Додати довжину");
        addLengthButton.setOnAction(e -> addLengthToWire());

        Button addZeroButton = new Button("Обнулити довжину");
        addZeroButton.setOnAction(e -> addZeroToWire());

        // Додаємо поле вводу та кнопку на панель інструментів
        toolBox.getChildren().addAll(lengthInput, addLengthButton, addZeroButton);
    }

    private void addLengthToWire() {
        try {
            double additionalLength = Double.parseDouble(lengthInput.getText());
            totalWireLength += additionalLength;
            lengthLabel.setText("Довжина дроту: " + String.format("%.2f", totalWireLength) + " м");
            lengthInput.clear();
        } catch (NumberFormatException e) {
            lengthInput.clear();
            lengthInput.setPromptText("Invalid input");
        }
    }

    private void addZeroToWire() {
        try {
            totalWireLength = 0;
            lengthLabel.setText("Довжина дроту: " + String.format("%.2f", totalWireLength) + " м");
            lengthInput.clear();
        } catch (NumberFormatException e) {
            lengthInput.clear();
            lengthInput.setPromptText("Invalid input");
        }
    }

    private void drawShape(double endX, double endY) {
        Drawable shape = null;
        boolean flagEraser = false;

        // Зберігаємо копію drawings перед додаванням нового елемента
        drawingsHistory.push(new ArrayList<>(drawings));

        switch (currentTool) {
            case "line":
                shape = new LineDrawable(startX, startY, endX, endY, Color.BLACK);
                break;
            case "rectangle":
                shape = new RectangleDrawable(startX, startY, endX, endY, Color.BLACK);
                break;
            case "eraser":
                flagEraser = true;
                shape = new EraserDrawable(startX, startY, endX, endY);
                break;
        }
        if (shape != null) {
            drawings.add(shape);
            shape.draw(gc);
            if (shape instanceof LineDrawable) {
                displayLineLength((LineDrawable) shape);
            }
        }
    }


    private void drawWire(double endX, double endY) {

        drawingsHistory.push(new ArrayList<>(drawings));
        double length = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        totalWireLength += length / 80;

        Drawable wire = new LineDrawable(startX, startY, endX, endY, Color.RED);
        drawings.add(wire);
        wire.draw(gc);

        lengthLabel.setText("Довжина дроту: " + String.format("%.2f", totalWireLength) + " м");

        startX = endX;
        startY = endY;
    }

    private void displayLineLength(LineDrawable line) {
        double length = Math.sqrt(Math.pow(line.getEndX() - line.getStartX(), 2) +
                Math.pow(line.getEndY() - line.getStartY(), 2));
        double midX = (line.getStartX() + line.getEndX()) / 2;
        double midY = (line.getStartY() + line.getEndY()) / 2;

        gc.setFill(Color.BLACK);
    }

    private void drawPreviewShape(double endX, double endY) {
        clearPreview();
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(3);

        switch (currentTool) {
            case "line":
                gc.strokeLine(startX, startY, endX, endY);
                break;
            case "rectangle":
                gc.strokeRect(Math.min(startX, endX), Math.min(startY, endY),
                        Math.abs(endX - startX), Math.abs(endY - startY));
                break;
            case "eraser":
                gc.clearRect(Math.min(startX, endX), Math.min(startY, endY),
                        Math.abs(endX - startX), Math.abs(endY - startY));
                break;
        }
    }

    private void drawPreviewLine(double endX, double endY) {
        clearPreview();
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(3);
        gc.strokeLine(startX, startY, endX, endY);

        // Відображення довжини під час перетягування
        double length = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        double midX = (startX + endX) / 2;
        double midY = (startY + endY) / 2;
        gc.setFill(Color.BLACK);
        gc.fillText(String.format("%.2f м", length / 80), midX, midY - 5); // Відображення довжини над лінією при малюванні
    }

    private void drawPreviewWire(double endX, double endY) {
        clearPreview();
        gc.setStroke(Color.RED);
        gc.setLineWidth(3);
        gc.strokeLine(startX, startY, endX, endY);

        // Відображення довжини під час перетягування
        double length = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        double midX = (startX + endX) / 2;
        double midY = (startY + endY) / 2;
        gc.setFill(Color.BLACK);
        gc.fillText(String.format("%.2f м", length / 80), midX, midY - 5); // Відображення довжини над проводом при малюванні
    }

    private void drawPreviewRectangle(double endX, double endY) {
        clearPreview();
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(3);

        // Координати і розміри прямокутника
        double x = Math.min(startX, endX);
        double y = Math.min(startY, endY);
        double width = Math.abs(endX - startX);
        double height = Math.abs(endY - startY);

        // Малюємо прямокутник
        gc.strokeRect(x, y, width, height);

        // Відображення ширини та висоти під час перетягування
        gc.setFill(Color.BLACK);
        gc.fillText(String.format("%.2f м", width / 80), x + width / 2, y - 5);   // Ширина над прямокутником при малюванні
        gc.fillText(String.format("%.2f м", height / 80), x - 40, y + height / 2); // Висота зліва від прямокутника при малюванні
    }

    private void clearPreview() {
        gc.clearRect(0, 0, w, h);
        drawCanvasBorder();
        redrawAllShapes();
    }

    private void redrawAllShapes() {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        drawCanvasBorder(); // Перемалювання рамки канвасу

        // Перемалювання інших фігур
        for (Drawable shape : drawings) {
            shape.draw(gc);
        }

    }

    private void switchToWireMode(VBox toolBox) {
        wireMode = true;
        clearPreview();
        toolBox.getChildren().clear();
        createLengthInputAndButton(toolBox);
        Button finishWireButton = new Button("Завершити прокладку");
        electricMeterButton = new Button("Електрощиток");
        bulbMeterButton = new Button("Лампочка");
        rosetteMeterButton = new Button("Розетка");

        electricMeterButton.setOnAction(e -> {
            isPlacingElectricMeter = !isPlacingElectricMeter; // Перемикання режиму розміщення
        });

        bulbMeterButton.setOnAction(e -> {
            isPlacingBulbMeter = !isPlacingBulbMeter; // Перемикання режиму розміщення
        });

        rosetteMeterButton.setOnAction(e -> {
            isPlacingRosetteMeter = !isPlacingRosetteMeter; // Перемикання режиму розміщення
        });

        finishWireButton.setOnAction(e -> switchToDrawingMode(toolBox));
        toolBox.getChildren().addAll(lengthLabel, electricMeterButton, bulbMeterButton, rosetteMeterButton,finishWireButton);

    }

    private void switchToDrawingMode(VBox toolBox) {
        wireMode = false;
        clearPreview();

        toolBox.getChildren().clear();

        Button lineButton = new Button("Лінія");
        Button rectangleButton = new Button("Прямокутник");
        Button eraserButton = new Button("Гумка");
        Button switchModeButton = new Button("Перейти в режим проводів");

        lineButton.setOnAction(e -> {
            currentTool = "line";
            wireMode = false;
        });
        rectangleButton.setOnAction(e -> {
            currentTool = "rectangle";
            wireMode = false;
        });
        eraserButton.setOnAction(e -> {
            currentTool = "eraser";
            wireMode = false;
        });
        switchModeButton.setOnAction(e -> switchToWireMode(toolBox));


        toolBox.getChildren().addAll(lineButton, rectangleButton, eraserButton, switchModeButton, lengthLabel);
    }

    interface Drawable {
        void draw(GraphicsContext gc);
    }

    static class LineDrawable implements Drawable {
        private double startX, startY, endX, endY;
        private Color color;

        public LineDrawable(double startX, double startY, double endX, double endY, Color color) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.color = color;
        }

        @Override
        public void draw(GraphicsContext gc) {
            gc.setStroke(color);
            gc.setLineWidth(3);
            gc.strokeLine(startX, startY, endX, endY);

            double x = (startX + endX) / 2;
            double y = (startY + endY) / 2;

            double length = Math.sqrt(Math.pow(startX - endX, 2) + Math.pow(startY - endY, 2));
            gc.fillText(String.format("%.2f м", length / 80), x, y - 5);
        }

        public double getStartX() { return startX; }
        public double getStartY() { return startY; }
        public double getEndX() { return endX; }
        public double getEndY() { return endY; }
    }

    static class RectangleDrawable implements Drawable {
        private double startX, startY, endX, endY;
        private Color color;

        public RectangleDrawable(double startX, double startY, double endX, double endY, Color color) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.color = color;
        }

        @Override
        public void draw(GraphicsContext gc) {
            gc.setStroke(color);
            gc.setLineWidth(3);
            double x = Math.min(startX, endX);
            double y = Math.min(startY, endY);
            double width = Math.abs(endX - startX);
            double height = Math.abs(endY - startY);
            gc.strokeRect(x, y, width, height);
            gc.fillText(String.format("%.2f м", width / 80), x + width / 2, y - 5); // Відображення ширини
            gc.fillText(String.format("%.2f м", height / 80), x - 40, y + height / 2); // Відображення висоти
        }
    }

    static class EraserDrawable implements Drawable {
        private double startX, startY, endX, endY;

        public EraserDrawable(double startX, double startY, double endX, double endY) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }

        @Override
        public void draw(GraphicsContext gc) {
            gc.clearRect(Math.min(startX, endX), Math.min(startY, endY),
                    Math.abs(endX - startX), Math.abs(endY - startY));
        }
    }

    private class ElectricMeterDrawable implements Drawable {
        private double x, y, size;

        public ElectricMeterDrawable(double x, double y, double size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }

        public void updatePosition(double newX, double newY) {
            this.x = newX;
            this.y = newY;
        }

        @Override
        public void draw(GraphicsContext gc) {
            gc.setStroke(Color.BLUE);
            gc.setLineWidth(2);
            // Малюємо квадрат
            gc.strokeRect(x - size / 2, y - size / 2, size, size);
            // Малюємо горизонтальну лінію
            gc.strokeLine(x - size / 2, y, x + size / 2, y);
        }
    }

    private class LightBulbDrawable implements Drawable {
        private double x, y, radius;

        public LightBulbDrawable(double x, double y, double radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }

        public void updatePosition(double newX, double newY) {
            this.x = newX;
            this.y = newY;
        }

        @Override
        public void draw(GraphicsContext gc) {
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(2);

            gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);

            gc.strokeLine(x - radius, y - radius, x + radius, y + radius);
            gc.strokeLine(x - radius, y + radius, x + radius, y - radius);
        }
    }

    private class SocketDrawable implements Drawable {
        private double x, y, width, height;

        public SocketDrawable(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void updatePosition(double newX, double newY) {
            this.x = newX;
            this.y = newY;
        }

        @Override
        public void draw(GraphicsContext gc) {
            gc.setStroke(Color.GRAY);
            gc.setLineWidth(2);

            // Можна додати деталі для розетки, якщо потрібно
            // Наприклад, малюємо круги для контактів
            double contactRadius = 5;
            gc.fillOval(x - contactRadius, y - height / 4 - contactRadius, contactRadius * 2, contactRadius * 2);
            gc.fillOval(x + contactRadius, y - height / 4 - contactRadius, contactRadius * 2, contactRadius * 2);
        }
    }


}