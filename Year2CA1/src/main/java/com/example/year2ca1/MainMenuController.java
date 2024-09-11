package com.example.year2ca1;

import javafx.event.ActionEvent;
import javafx.scene.control.Slider;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class MainMenuController {
    public ImageView origImage;
    public ImageView overlayImageView;
    public ImageView editedImage;
    public Text imageInfo;
    public Text boxCountText;
    public Slider redValue;
    public Slider greenValue;
    public Slider blueValue;
    private File selectedFile;

    public void openImagePicker(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif")
        );

        selectedFile = fileChooser.showOpenDialog(origImage.getScene().getWindow());

        if (selectedFile != null) {
            Image image = new Image(selectedFile.toURI().toString(), true); // Load image
            origImage.setImage(image);
            editedImage.setImage(image); //Copy original image to editedImage
            overlayImageView.setImage(new WritableImage((int) image.getWidth(), (int) image.getHeight())); // Init overlay image
            updateImageInfo(image);
        }
    }

    public void getImageInfo(ActionEvent actionEvent) {
        Image image = origImage.getImage();

        if (image != null) {
            updateImageInfo(image);
        }
    }

    private void updateImageInfo(Image image) {
        double width = image.getWidth();
        double height = image.getHeight();
        String filename = (selectedFile != null) ? selectedFile.getName() : "Unknown";
        imageInfo.setText(String.format("File: %s\nWidth: %.2f px\nHeight: %.2f px", filename, width, height));
    }

    public void greyscaleImage(ActionEvent actionEvent) {
        Image image = origImage.getImage();

        if (image != null) {
            WritableImage greyscaleImage = new WritableImage((int) image.getWidth(), (int) image.getHeight());
            PixelReader pixelReader = image.getPixelReader();
            PixelWriter pixelWriter = greyscaleImage.getPixelWriter();

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    Color color = pixelReader.getColor(x, y);
                    double greyscale = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
                    Color greyscaleColor = new Color(greyscale, greyscale, greyscale, color.getOpacity());
                    pixelWriter.setColor(x, y, greyscaleColor);
                }
            }

            editedImage.setImage(greyscaleImage);
        }
    }

    public void openColourChannels(ActionEvent actionEvent) {
        Image image = origImage.getImage();

        if (image != null) {
            WritableImage colorAdjustedImage = new WritableImage((int) image.getWidth(), (int) image.getHeight());
            PixelReader pixelReader = image.getPixelReader();
            PixelWriter pixelWriter = colorAdjustedImage.getPixelWriter();

            double redAdjust = redValue.getValue();
            double greenAdjust = greenValue.getValue();
            double blueAdjust = blueValue.getValue();

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    Color color = pixelReader.getColor(x, y);
                    double newRed = Math.min(1.0, color.getRed() * redAdjust);
                    double newGreen = Math.min(1.0, color.getGreen() * greenAdjust);
                    double newBlue = Math.min(1.0, color.getBlue() * blueAdjust);
                    Color adjustedColor = new Color(newRed, newGreen, newBlue, color.getOpacity());
                    pixelWriter.setColor(x, y, adjustedColor);
                }
            }

            editedImage.setImage(colorAdjustedImage);
        }
    }

    public void exitApplication(ActionEvent actionEvent) {
        Stage stage = (Stage) origImage.getScene().getWindow();
        stage.close();
    }

    public void onImageClick(MouseEvent mouseEvent) {
        int x = (int) mouseEvent.getX();
        int y = (int) mouseEvent.getY();
        Image image = origImage.getImage();

        if (image != null && x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
            PixelReader pixelReader = image.getPixelReader();
            if (pixelReader != null) {
                Color selectedColor = pixelReader.getColor(x, y);
                highlightSimilarColors(selectedColor, image);
                highlightRegions(image, selectedColor);
            } else {
                System.err.println("PixelReader is null.");
            }
        } else {
            System.err.println("Clicked coordinates are out of bounds.");
        }
    }

    private void highlightSimilarColors(Color targetColor, Image image) {
        double threshold = 0.1;
        WritableImage highlightedImage = new WritableImage((int) image.getWidth(), (int) image.getHeight());
        PixelReader pixelReader = image.getPixelReader();
        PixelWriter pixelWriter = highlightedImage.getPixelWriter();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color currentColor = pixelReader.getColor(x, y);

                if (areColorsSimilar(targetColor, currentColor, threshold)) {
                    pixelWriter.setColor(x, y, Color.WHITE);
                } else {
                    pixelWriter.setColor(x, y, Color.BLACK);
                }
            }
        }

        editedImage.setImage(highlightedImage);
    }

    private boolean areColorsSimilar(Color color1, Color color2, double threshold) {
        double redDiff = Math.abs(color1.getRed() - color2.getRed());
        double greenDiff = Math.abs(color1.getGreen() - color2.getGreen());
        double blueDiff = Math.abs(color1.getBlue() - color2.getBlue());
        return (redDiff + greenDiff + blueDiff) / 3 < threshold;
    }

    private void highlightRegions(Image image, Color targetColor) {
        double threshold = 0.1;
        PixelReader pixelReader = image.getPixelReader();
        WritableImage overlayImage = new WritableImage((int) image.getWidth(), (int) image.getHeight());
        PixelWriter pixelWriter = overlayImage.getPixelWriter();

        Set<Point> visited = new HashSet<>();
        int boxCount = 0;  // Counter for the yellow boxes

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Point point = new Point(x, y);
                if (!visited.contains(point) && areColorsSimilar(targetColor, pixelReader.getColor(x, y), threshold)) {
                    Rectangle boundingBox = findBoundingBox(image, x, y, targetColor, threshold, visited);
                    drawRectangle(pixelWriter, boundingBox);
                    boxCount++;
                }
            }
        }

        overlayImageView.setImage(overlayImage);  // Update overlay image
        boxCountText.setText("Pills: " + boxCount);  // Update box count text
    }


    private Rectangle findBoundingBox(Image image, int startX, int startY, Color targetColor, double threshold, Set<Point> visited) {
        int minX = startX, minY = startY, maxX = startX, maxY = startY;
        Set<Point> toVisit = new HashSet<>();
        toVisit.add(new Point(startX, startY));

        PixelReader pixelReader = image.getPixelReader();

        while (!toVisit.isEmpty()) {
            Point p = toVisit.iterator().next();  // Get any point from the set
            toVisit.remove(p);  // Remove it from the set

            if (p.x < 0 || p.y < 0 || p.x >= image.getWidth() || p.y >= image.getHeight()) continue;
            if (visited.contains(p)) continue;

            visited.add(p);
            Color currentColor = pixelReader.getColor(p.x, p.y);

            if (areColorsSimilar(targetColor, currentColor, threshold)) {
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);

                // Add neighboring points to the set if they haven't been visited
                addIfValid(toVisit, p.x + 1, p.y, visited, image, targetColor, threshold);
                addIfValid(toVisit, p.x - 1, p.y, visited, image, targetColor, threshold);
                addIfValid(toVisit, p.x, p.y + 1, visited, image, targetColor, threshold);
                addIfValid(toVisit, p.x, p.y - 1, visited, image, targetColor, threshold);
            }
        }

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private void addIfValid(Set<Point> toVisit, int x, int y, Set<Point> visited, Image image, Color targetColor, double threshold) {
        if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
            Point p = new Point(x, y);
            if (!visited.contains(p) && areColorsSimilar(targetColor, image.getPixelReader().getColor(x, y), threshold)) {
                toVisit.add(p);
            }
        }
    }

    private void drawRectangle(PixelWriter pixelWriter, Rectangle boundingBox) {
        Color borderColor = Color.YELLOW;

        for (int x = boundingBox.x; x <= boundingBox.x + boundingBox.width; x++) {
            if (x >= 0 && x < origImage.getImage().getWidth()) {
                if (boundingBox.y >= 0 && boundingBox.y < origImage.getImage().getHeight()) {
                    pixelWriter.setColor(x, boundingBox.y, borderColor);
                }
                if (boundingBox.y + boundingBox.height >= 0 && boundingBox.y + boundingBox.height < origImage.getImage().getHeight()) {
                    pixelWriter.setColor(x, boundingBox.y + boundingBox.height, borderColor);
                }
            }
        }

        for (int y = boundingBox.y; y <= boundingBox.y + boundingBox.height; y++) {
            if (y >= 0 && y < origImage.getImage().getHeight()) {
                if (boundingBox.x >= 0 && boundingBox.x < origImage.getImage().getWidth()) {
                    pixelWriter.setColor(boundingBox.x, y, borderColor);
                }
                if (boundingBox.x + boundingBox.width >= 0 && boundingBox.x + boundingBox.width < origImage.getImage().getWidth()) {
                    pixelWriter.setColor(boundingBox.x + boundingBox.width, y, borderColor);
                }
            }
        }
    }

    private static class Point {
        int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Point point = (Point) obj;
            return x == point.x && y == point.y;
        }
        @Override
        public int hashCode() {
            return 31 * x + y;
        }
    }

    private static class Rectangle {
        int x, y, width, height;
        Rectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}


