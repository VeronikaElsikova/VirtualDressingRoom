package cz.upol.inf.dressingroom;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import org.jetbrains.annotations.TestOnly;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/***
 * Contains methods that are needed by DressingRoom class for detecting objects using CascadeClassifiers.
 */
class HaarCascade {

    private static final String TAG = "HaarCascade";

    private final Mat mImageGray = new Mat(); // initialization of Mat variables in methods is causing memory leaks
    private final MatOfRect results1 = new MatOfRect();
    /***
     * Method converts image to grayscale, equalizes it's histogram and then applies given classifier to the image.
     * Results are converted from MatOfRect to a list and are returned.
     * @param image image
     * @param classifier cascade classifier (must be loaded)
     * @return list of detected faces
     */
    protected List<Rect> applyClassifier(Mat image, CascadeClassifier classifier) {
        //preparing image for detection
        Imgproc.cvtColor(image, mImageGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(mImageGray, mImageGray);

        // getting results
        classifier.detectMultiScale(mImageGray, results1);

        // clean up
        mImageGray.release();
        List<Rect> result = results1.toList();
        results1.release();
        return result;
    }

    private final Mat nImageGray = new Mat(); // initialization of Mat variables in methods is causing memory leaks
    private final MatOfRect results2 = new MatOfRect();
    /***
     * Method converts image to grayscale, equalizes it's histogram and then applies given classifier to the image with given limits.
     * Results are converted from MatOfRect to a list and are returned.
     * @param image image
     * @param classifier cascade classifier (must be loaded)
     * @param minFaceSize determines the smallest size of the object that can be detected
     * @param maxFaceSize determines the largest size of the object that can be detected
     * @return list of detected faces
     */
    protected List<Rect> applyClassifier(Mat image, CascadeClassifier classifier, int minFaceSize, int maxFaceSize) {
        //preparing image for detection
        Imgproc.cvtColor(image, nImageGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(nImageGray, nImageGray);

        // getting results
        classifier.detectMultiScale(nImageGray, results2,1.1,3, Objdetect.CASCADE_SCALE_IMAGE,
                new Size(minFaceSize, minFaceSize),
                new Size(maxFaceSize, maxFaceSize)
        );

        nImageGray.release();
        List<Rect> result = results2.toList();
        results2.release();
        return result;
    }

    /***
     * Method loads classifier from given fileName and resource id and returns it, if loading was successful.
     * classifier should be in "raw" folder in resources. Context is needed because of a workaround that was used for fetching the classifier file.
     * OpenCV only accepts filepath of HaarCascade in CascadeClassifier and I wasn't able to access it reliably from the resource files, so new file is
     * being created, which requires application context.
     * @param context application context
     * @param resourceRaw resource id (R.raw.filename)
     * @param fileName name of xml file that contains the classifier
     * @return If loading was successful, method returns loaded CascadeClassifier. If loading fails null is returned.
     */
    protected static CascadeClassifier loadClassifier(Context context, int resourceRaw, String fileName) {
        try (InputStream inputStream = context.getResources().openRawResource(resourceRaw)) {
            /*
            workaround as described here:  https://laxmantidake.medium.com/real-time-face-detection-with-android-studio-and-opencv-e0b2e86a04eb
            I haven't found a reliable way of getting a path to a resource raw file and OpenCV only accepts filepath of HaarCascade in CascadeClassifier
             */
            File cascadeDir = context.getDir("cascadeDir", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, fileName);
            FileOutputStream outputStream = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();

            // loading cascade xml file and creating CascadeClassifier
            CascadeClassifier cascade = new CascadeClassifier();
            if (!cascade.load(cascadeFile.getAbsolutePath())) {
                Log.e(TAG, "failed to load " + fileName + " cascade");
                return null;
            }

            // deleting temporary file
            if(!cascadeFile.delete()) Log.e(TAG, "cascadeFile not deleted");

            Log.d(TAG, fileName + " cascade loaded");
            return cascade;

        } catch (Resources.NotFoundException | IOException e) {
            Log.e(TAG, "failed to open raw resource:" + fileName + "\n" + e);
            return null;
        }
    }

    /***!!!Method is intended to be used only for testing purposes!!!*/
    @TestOnly
    protected static Mat detectAndDisplay(Mat mat, CascadeClassifier classifier) {
        //Mat mat = Convert.bitmapToMat(image);
        Mat imageGray = new Mat();
        Imgproc.cvtColor(mat, imageGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(imageGray, imageGray);
        //CascadeClassifier frontalFaceClassifier2 = loadClassifier(context, R.raw.haarcascade_frontalface_alt, "haarcascade_frontalface_alt.xml");
        //CascadeClassifier eyeClassifier = loadClassifier(context, R.raw.haarcascade_eye, "haarcascade_eye.xml");
        return applyClassifierAndDraw(mat, imageGray, classifier);
    }

    /***!!!Method is intended to be used only for testing purposes!!!*/
    @TestOnly
    protected static Mat detectAndDisplay(Context context, Mat mat) {
        //Mat mat = Convert.bitmapToMat(image);
        Mat imageGray = new Mat();
        Imgproc.cvtColor(mat, imageGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(imageGray, imageGray);
        CascadeClassifier frontalFaceClassifier2 = loadClassifier(context, R.raw.haarcascade_frontalface_alt, "haarcascade_frontalface_alt.xml");
        if(frontalFaceClassifier2==null) {
            Log.e(TAG, "classifier not loaded");
            return mat;
        }
        return applyClassifierAndDraw(mat, imageGray, frontalFaceClassifier2);
    }

    /***!!!Method is intended to be used only for testing purposes!!!*/
    @TestOnly
    private static Mat applyClassifierAndDraw(Mat image, Mat imageGray, CascadeClassifier cascade) {
        Mat resultImage = image.clone();
        MatOfRect results = new MatOfRect();
        cascade.detectMultiScale(imageGray, results);
        List<Rect> listOfResults = results.toList();
        for (Rect result : listOfResults) {
            Point pt1 = new Point(result.x, result.y);
            Point pt2 = new Point(result.x+result.width, result.y+result.height);
            Imgproc.rectangle(resultImage, pt1, pt2, new Scalar(255, 0, 0, 255) , 2, Imgproc.LINE_8, 0);
            Point a = new Point(result.x + (double)result.width/2, result.y + (double)result.height/2);
            Imgproc.circle(resultImage, a, 2, new Scalar(0, 0, 255, 255));
        }
        return resultImage;
    }

}