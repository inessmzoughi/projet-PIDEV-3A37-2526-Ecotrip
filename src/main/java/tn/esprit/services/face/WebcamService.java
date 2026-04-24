package tn.esprit.services.face;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class WebcamService {

    private VideoCapture capture;
    private boolean      running = false;

    static {
        // Load OpenCV native library
        nu.pattern.OpenCV.loadLocally();
    }

    /** Open the default camera (index 0) */
    public boolean start() {
        capture = new VideoCapture(0);
        running = capture.isOpened();
        return running;
    }

    /** Release the camera */
    public void stop() {
        running = false;
        if (capture != null && capture.isOpened())
            capture.release();
    }

    public boolean isRunning() { return running; }

    /** Grab one frame — returns null if camera is closed or frame is empty */
    public Mat grabFrame() {
        if (!running || capture == null) return null;
        Mat frame = new Mat();
        capture.read(frame);
        return frame.empty() ? null : frame;
    }

    /**
     * Convert OpenCV Mat → JavaFX WritableImage.
     * No SwingFXUtils needed — pure JavaFX pixel writing.
     */
    public WritableImage matToWritableImage(Mat frame) {
        // OpenCV gives BGR — JavaFX needs RGB
        Mat rgb = new Mat();
        Imgproc.cvtColor(frame, rgb, Imgproc.COLOR_BGR2RGB);

        int width    = rgb.cols();
        int height   = rgb.rows();
        int channels = (int) rgb.channels();

        byte[] buffer = new byte[width * height * channels];
        rgb.get(0, 0, buffer);

        WritableImage image  = new WritableImage(width, height);
        PixelWriter   writer = image.getPixelWriter();
        writer.setPixels(
                0, 0, width, height,
                PixelFormat.getByteRgbInstance(),
                buffer, 0, width * channels
        );
        return image;
    }

    /**
     * Convert Mat → raw JPEG bytes.
     * Used to send to HuggingFace API.
     */
    public byte[] matToJpegBytes(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".jpg", frame, buffer);
        return buffer.toArray();
    }
}