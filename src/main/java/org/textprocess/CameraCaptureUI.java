import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;

public class CameraCaptureUI extends JFrame {

    private static final String API_KEY = "INSERT_YOUR_KEY_HERE";  // Replace with your API key

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);  // Load the OpenCV native library
    }

    private JLabel cameraFeedLabel;
    private VideoCapture camera;
    private Timer timer;
    private JButton captureButton;
    private JLabel cameraInfoLabel;

    public CameraCaptureUI() {
        super("Camera Capture UI");

        setLayout(new BorderLayout());

        // Camera feed label
        cameraFeedLabel = new JLabel();
        add(cameraFeedLabel, BorderLayout.CENTER);

        // Capture button
        captureButton = new JButton("Capture Text");
        add(captureButton, BorderLayout.SOUTH);

        // Camera information label
        cameraInfoLabel = new JLabel("Camera Info: Not Available");
        add(cameraInfoLabel, BorderLayout.NORTH);

        // Open the camera
        camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.out.println("Error: Cannot open camera.");
            System.exit(1);
        }

        // Set camera information
        cameraInfoLabel.setText("Camera Info: Resolution: " + camera.get(3) + "x" + camera.get(4));

        // Timer to update the UI with the current frame
        timer = new Timer(33, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Mat frame = new Mat();
                if (camera.read(frame)) {
                    ImageIcon image = new ImageIcon(matToBufferedImage(frame));
                    cameraFeedLabel.setIcon(image);
                }
            }
        });
        timer.start();

        // Capture frame on button click and process it using Google Vision API through HTTP request
        captureButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                captureAndProcessFrame();
            }
        });

        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // Capture the current frame and process it using Google Vision API via HTTP request
    private void captureAndProcessFrame() {
        Mat frame = new Mat();
        if (camera.read(frame)) {
            String fileName = "captured_frame.png";
            Imgcodecs.imwrite(fileName, frame);  // Save the frame locally as an image
            File imageFile = new File(fileName);
            JOptionPane.showMessageDialog(this, "Frame captured. Now processing with Google Vision...");
            extractTextFromImage(imageFile);  // Pass the saved image to Google Vision API using HTTP request
        }
    }

    // Extract text from the image using Google Vision API (via HTTP request)
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
                // Log the response and show in a dialog box
                System.out.println("Text detection successful! Full response:");
                System.out.println(response.toString());

                // Show the extracted text in a dialog box
                JOptionPane.showMessageDialog(this, "Extracted Text: " + response.toString());

                // Save the full response to a file
                saveFullResponse(response.toString(), "vision_api_full_response.json");
            } else {
                System.out.println("Error: " + connection.getResponseCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Save the full response to a file
    private void saveFullResponse(String jsonResponse, String fileName) {
        try {
            File responseFile = new File(fileName);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(responseFile))) {
                writer.write(jsonResponse);
                System.out.println("Full response saved to " + responseFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Convert Mat object from OpenCV to BufferedImage
    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] b = new byte[bufferSize];
        mat.get(0, 0, b);
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CameraCaptureUI());
    }
}