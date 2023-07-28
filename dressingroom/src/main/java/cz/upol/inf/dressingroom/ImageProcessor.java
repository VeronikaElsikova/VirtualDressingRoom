package cz.upol.inf.dressingroom;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/***
 * Contains image altering methods that are needed by DressingRoom class.
 */
class ImageProcessor {
    private static final String TAG = "ImageProcessor";
    private final HaarCascade haarCascade1 = new HaarCascade();
    private final HaarCascade haarCascade2 = new HaarCascade();

    private Mat hrReducedImage = new Mat(); //declaration of Mat variables in methods is causing memory leaks
    /***
     * Method calculates an angle of line with x-axis. This line is going through centers of both eyes. To get the centers method
     * detects eyes in submat of imaged given by face rectangle. Detected eyes are filtered using EYE_LEVEL, the eyes closest to the EYE_LEVEL
     * are selected and angle is calculated. If less than 2 eyes are detected, zero is returned as a default value.
     * @param image mat where angle in going to be detected in
     * @param face rectangle representing face
     * @param eyesClassifier cascadeClassifier for eye detection (must be loaded).
     * @param EYE_LEVEL percentage of face's height
     * @return angle representing head rotation, if detection failed 0 is returned as default value
     */
    protected double getHeadRotationAngle(Mat image, Rect face, CascadeClassifier eyesClassifier, double EYE_LEVEL) {
        hrReducedImage = image.submat(face);
        List<Rect> eyes = haarCascade1.applyClassifier(hrReducedImage, eyesClassifier);
        hrReducedImage.release();

        if (eyes.size() < 2) {
            Log.d(TAG, "Less than 2 eyes detected.");
            return 0; // needs two eyes to detect face rotation
        } else {
            Log.d(TAG, "Eyes detected.");
        }

        List<Point> centers = new ArrayList<>();
        double eyeLevel = face.height*EYE_LEVEL;
        for(Rect eye : eyes) { //TODO redo using streams?
            Point center =  new Point(eye.x + eye.width/2.0, eye.y + eye.height/2.0);
            centers.add(center);
        }

        // if more than two eyes are detected, the eyes closest to the eye level are selected
        centers = centers.stream().sorted((p1, p2) -> (int) (Math.abs(eyeLevel - p1.y) - Math.abs(eyeLevel - p2.y))).limit(2).collect(Collectors.toList());

        Point leftEye = centers.get(0).x>centers.get(1).x ? centers.get(1) : centers.get(0); // from viewer's perspective
        Point rightEye = centers.get(0).x>centers.get(1).x ? centers.get(0) : centers.get(1);

        double angle = Math.atan2( rightEye.y - leftEye.y, rightEye.x - leftEye.x ) * ( 180 / Math.PI );

        return angle*-1;// rotation compensation
    }

    private Mat rfFaceSubmat = new Mat(), rfRotationMatrix; //declaration of Mat variables in methods is causing memory leaks
    private final Mat rfRotatedImage = new Mat();
    /***
     * Method takes given image, crops an area when the face is located using the face variable with needed margin, and then the method
     * rotates the image and runs face detection using given cascadeClassifier.
     * @param source image where face is going to be detected
     * @param faceClassifier cascadeClassifier for face detection (must be loaded).
     * @param face face detected in the unrotated image
     * @param angle head rotation angle
     * @return rectangle representing the biggest face, that was detected in the the source images after it was rotated by given angle
     */
    protected Rect getRotatedFace(Mat source, CascadeClassifier faceClassifier, Rect face, double angle) {
        int x = face.x - face.width/2;
        int y = face.y - face.height/2;
        if(x<0) x = 0;
        if(y<0) y = 0;

        int width = face.height*2;
        int height = face.width*2;
        if(width + face.x > source.width()) width = source.width() - face.x;
        if(height + face.y > source.height()) height = source.height() - face.y; //bigger to avoid cropping of the face

        // cropping image
        Rect faceROI = new Rect(x, y, width, height);
        rfFaceSubmat = source.submat(faceROI);

        // rotating image
        rfRotationMatrix = Imgproc.getRotationMatrix2D(new Point(rfFaceSubmat.width()/2.0, rfFaceSubmat.height()/2.0), angle, 1.0);
        Imgproc.warpAffine(rfFaceSubmat, rfRotatedImage, rfRotationMatrix, rfFaceSubmat.size());

        // face detection
        List<Rect> haarDetectionResults = haarCascade2.applyClassifier(rfRotatedImage, faceClassifier);

        rfFaceSubmat.release();
        rfRotatedImage.release();
        rfRotationMatrix.release();

        if(haarDetectionResults.isEmpty()) return new Rect();
        else {
            // selects face closest to the camera (rect with the biggest area)
            Rect result = haarDetectionResults.stream().max(Comparator.comparing(Rect::area)).orElse(haarDetectionResults.get(0));
            return new Rect(result.x + faceROI.x, result.y + faceROI.y, result.width, result.height);
        }
    }

    private  Mat rRotationMatrix = new Mat(); //declaration of Mat variables in methods is causing memory leaks
    /***
     * Rotates given image by an angle and saves it to dst. Don't use for rotating image back, dimensions of the image will be wrong.
     * @param image source
     * @param dst destination
     * @param angle rotation angle
     */
    protected void rotateImage(Mat image, Mat dst, double angle) {
        Point center = new Point(image.width()/2.0, image.height()/2.0);
        rRotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0);

        //https://www.geeksforgeeks.org/rotate-image-without-cutting-off-sides-using-python-opencv/
        double cos = Math.abs(rRotationMatrix.get(0, 0)[0]);
        double sin = Math.abs(rRotationMatrix.get(0, 1)[0]);
        int newHeight = (int) ((image.width() * sin) + (image.height() * cos));
        int newWidth = (int) ((image.width() * cos) + (image.height() * sin));

        // from the article, but adjusted to center of image being at (clothes.width()/2, 0)
        rRotationMatrix.put(0, 2, rRotationMatrix.get(0, 2)[0] + ((newWidth / 2.0) - center.x));
        rRotationMatrix.put(1, 2, rRotationMatrix.get(1, 2)[0] + ((newHeight / 2.0) - center.y));

        //rotation
        Imgproc.warpAffine(image, dst, rRotationMatrix, new Size(newWidth, newHeight));
        rRotationMatrix.release();
    }

    private Mat rbRotationMatrix = new Mat(); //declaration of Mat variables in methods is causing memory leaks
    /***
     * Method is used to rotates image back after method rotateImage() was used.
     * @param image rotated image
     * @param dst destination
     * @param angle opposite value of the angle that was used in rotateImage()
     */
    protected void rotateImageBack(Mat image, Mat dst, double angle) {
        Point center = new Point(image.width()/2.0, image.height()/2.0);
        rbRotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0);
        Imgproc.warpAffine(image, dst, rbRotationMatrix, image.size());
        rbRotationMatrix.release();
    }
}