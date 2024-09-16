package org.textprocess;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Timer;
import java.util.TimerTask;

public class LiveFeedCapture {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        captureLiveFeed();
    }

    public static void captureLiveFeed() {
        VideoCapture camera = new VideoCapture(0); //need to expand this to support IP/stream url

        if (!camera.isOpened()) {
            System.out.println("Error: camera can not be detected");
            return;
        }

        Mat frame = new Mat(); //need linear matrix transpose lib
        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (camera.read(frame)) {
                    System.out.println("Frame captured!");

                    String fileName = "live_frame.png";
                    Imgcodecs.imwrite(fileName, frame);

                    File imageFile = new File("live_frame.png");
                    processImageWithGoogleVision(imageFile);
                }
            }
        }, 0, 1000);  // Capture a frame every second (1000 ms)

        // Release the camera resource on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            camera.release();
            System.out.println("Camera resource released.");
        }));
    }

    public static void processImageWithGoogleVision(File imageFile) {
        try {
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());

            System.out.println("Processing frame with Google Vision API...");

            extractTextFromImage(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void extractTextFromImage(File imageFile) {
        System.out.println("Sending image to Google Cloud Vision...");
    }
}
