package cz.upol.inf.dressingroom;

import android.graphics.Bitmap;

import org.jetbrains.annotations.TestOnly;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/***
 * Contains methods that are needed by DressingRoom class for detection of contours.
 */
class Contours {

    private static final int THRESHOLD1 = 85;
    private static final int THRESHOLD2 = THRESHOLD1 * 2;
    private static final Size BLUR_KERNEL = new Size(3, 3);

    /**
     * Method converts image to grayscale, applies Gaussian blur for better edge detection. Image is ten eroded and dilated afterward
     * to filter our horizontal or vertical lines, if needed. For detection of all edges use kernel with equal sizes.
     * Canny edge detection is then run on the image and the result is returned.
     * @param image input image
     * @param kernelSize vertical or horizontal kernel to detect only vertical/horizontal lines
     * @return Mat with detected edges
     */
    protected static Mat cannyEdgeDetection(Mat image, Size kernelSize) {
        // converting to grayscale
        Mat imageGray = new Mat();
        Imgproc.cvtColor(image, imageGray, Imgproc.COLOR_BGR2GRAY);

        // blur for more effective edge detection
        Imgproc.blur(imageGray, imageGray, BLUR_KERNEL);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, kernelSize);

        // eroding and dilating the image with the kernel to get rid of the horizontal/vertical lines
        Imgproc.erode(imageGray, imageGray, kernel);
        Imgproc.dilate(imageGray, imageGray, kernel);

        // Canny edge detection
        Mat cannyOutput = new Mat();
        Imgproc.Canny(imageGray, cannyOutput, THRESHOLD1, THRESHOLD2);

        imageGray.release();
        return cannyOutput;
    }

    /***
     *  Method creates a vertical kernel and extracts vertical contours from given image.
     * @param image source image
     * @return List of points representing contours
     */
    protected static List<Point> getVerticalContours(Mat image) {
        /* kernel height needs to be bigger than 0 otherwise the method getStructuringElement() used in cannyEdgeDetection()
            throws an error and the height should be more than one, so the kernel would eliminate horizontal lines. */
        int verticalSize = image.cols() / 30;
        if(verticalSize<2) verticalSize = 2;

        return getContours(image, 1, verticalSize);
    }

    /***
     *  Method creates a horizontal kernel and extracts horizontal contours from given image.
     * @param image source image
     * @return List of points representing contours
     */
    protected static List<Point> getHorizontalContours(Mat image) {
        /* kernel width needs to be bigger than 0 otherwise the method getStructuringElement() used in cannyEdgeDetection()
            throws an error and the width should be more than one, so the kernel would eliminate vertical lines. */
        int horizontalSize = image.rows() / 30;
        if(horizontalSize<2) horizontalSize = 2;

        return getContours(image, horizontalSize, 1);
    }

    /***
     * Contours are extracted from result of Canny edge detection and are converted to points. ratio of the kernel sizes will determine what contours
     * are detected: bigger horizontal size means horizontal contours will be detected, bigger vertical size means vertical contours will be detected,
     * and equal sizes will result in detection of all edges.
     * @param image source image
     * @param horizontalSize horizontalSize of the kernel
     * @param verticalSize verticalSize of the kernel
     * @return List of points representing contours
     */
    private static List<Point> getContours(Mat image, int horizontalSize, int verticalSize) {
        Mat cannyOutput = cannyEdgeDetection(image, new Size(horizontalSize, verticalSize));
        Mat hierarchy = new Mat();

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(cannyOutput, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);

        // merging contours to one list with Points
        List<Point> points = new ArrayList<>();
        for(MatOfPoint contour : contours) {
            points.addAll(contour.toList());
            contour.release();
        }

        hierarchy.release();
        cannyOutput.release();
        return points;
    }


    /***
     * !!!Method is intended to be used only for testing purposes!!!
     * Canny edge detection is applied to given image. Then contours are detected on the result of edge detection. Contours are then drawn onto the image.
     * @param image source image
     * @return image with highlighted contours
     */
    @TestOnly
    protected static Bitmap drawContours(Bitmap image) {
        Mat imageMat = Convert.bitmapToMat(image);
        drawContours(imageMat);
        return Convert.matToBitmap(imageMat);
    }

    /***
     * !!!Method is intended to be used only for testing purposes!!!
     * Canny edge detection is applied to given image. Then contours are detected on the result of edge detection. Contours are then drawn onto the image.
     * @param image source image, destination (image will be changed)
     */
    @TestOnly
    protected static void drawContours(Mat image) {
        Mat cannyOutput = cannyEdgeDetection(image, new Size(1,1));
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(cannyOutput, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(255, 0, 0, 255);
            Imgproc.drawContours(image, contours, i, color, 2, Imgproc.LINE_8, hierarchy, 0, new Point());
        }
        hierarchy.release();
        cannyOutput.release();
    }

    /***
     * !!!Method is intended to be used only for testing purposes!!!
     * Canny edge detection is applied to given image. Then contours are detected on the result of edge detection. Contours are then drawn onto the image.
     * @param image source image, destination (image will be changed)
     */
    @TestOnly
    protected static void drawContours(Mat image, List<MatOfPoint> contours) {
        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(255, 0, 0, 255);
            Imgproc.drawContours(image, contours, i, color, 2, Imgproc.LINE_8, new Mat(), 0, new Point());
        }
    }

}
