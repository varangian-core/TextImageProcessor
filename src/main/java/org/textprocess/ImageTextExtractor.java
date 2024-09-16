import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

public class ImageTextExtractor {
    private static final String API_KEY = "INSERT_YOUR_KEY_HERE";
    private Point startPoint;
    private Point endPoint;
    private JFrame captureFrame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ImageTextExtractor().createUI());
    }

    public void createUI() {
        JFrame frame = new JFrame("Text Extractor");
        JButton captureButton = new JButton("Capture Text");

        captureButton.addActionListener(e -> startScreenCapture());

        frame.add(captureButton, BorderLayout.CENTER);
        frame.setSize(200, 100);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void startScreenCapture() {
        captureFrame = new JFrame();
        captureFrame.setUndecorated(true);
        captureFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        captureFrame.setOpacity(0.5f);
        captureFrame.setAlwaysOnTop(true);

        // Capture the region selected by the user
        captureFrame.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                endPoint = e.getPoint();
                captureFrame.dispose();
                captureSelectedRegion(startPoint, endPoint);
            }
        });

        // Draw a rectangle as the user drags the mouse
        captureFrame.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                endPoint = e.getPoint();
                captureFrame.repaint();
            }
        });

        // Paint the rectangle to visually show the selected region
        captureFrame.add(new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (startPoint != null && endPoint != null) {
                    g.setColor(Color.RED);
                    g.drawRect(Math.min(startPoint.x, endPoint.x), Math.min(startPoint.y, endPoint.y),
                            Math.abs(endPoint.x - startPoint.x), Math.abs(endPoint.y - startPoint.y));
                }
            }
        });

        captureFrame.setVisible(true);
    }

    // Capture the selected region and pass it to Google Vision API for text extraction
    private void captureSelectedRegion(Point startPoint, Point endPoint) {
        try {
            Robot robot = new Robot();
            Rectangle captureRect = new Rectangle(Math.min(startPoint.x, endPoint.x),
                    Math.min(startPoint.y, endPoint.y),
                    Math.abs(endPoint.x - startPoint.x),
                    Math.abs(endPoint.y - startPoint.y));
            BufferedImage screenFullImage = robot.createScreenCapture(captureRect);
            File imageFile = new File("captured_region.png");
            ImageIO.write(screenFullImage, "png", imageFile);

            extractTextFromImage(imageFile);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void extractTextFromImage(File imageFile) {
        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Create a JSON request body
            JsonObject requestBody = new JsonObject();
            JsonArray requestsArray = new JsonArray();
            JsonObject imageObject = new JsonObject();
            imageObject.addProperty("content", base64Image);

            JsonObject featureObject = new JsonObject();
            featureObject.addProperty("type", "TEXT_DETECTION");

            JsonObject requestObject = new JsonObject();
            requestObject.add("image", imageObject);
            JsonArray featuresArray = new JsonArray();
            featuresArray.add(featureObject);
            requestObject.add("features", featuresArray);

            requestsArray.add(requestObject);
            requestBody.add("requests", requestsArray);

            // Perform the request
            URL url = new URL("https://vision.googleapis.com/v1/images:annotate?key=" + API_KEY);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            // Send the request
            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.toString().getBytes());
                os.flush();
            }

            // Get the response
            StringBuilder response = new StringBuilder();
            if (connection.getResponseCode() == 200) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                }
                // Log the response and save to file
                System.out.println("Text detection successful! Full response:");
                System.out.println(response.toString());

                // Save the full response to a file
                saveFullResponse(response.toString(), "vision_api_full_response.json");
            } else {
                System.out.println("Error: " + connection.getResponseCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveFullResponse(String jsonResponse, String fileName) {
        try {
            // Get the path to the resources folder
            String resourcesFolder = "src/main/resources/";
            File directory = new File(resourcesFolder);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File responseFile = new File(directory, fileName);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(responseFile))) {
                writer.write(jsonResponse);
                System.out.println("Full response saved to " + responseFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}