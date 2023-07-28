package cz.upol.inf.dressingroom;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/***
 * Dressing room contains variety of methods for mapping clothing on standalone images and on frames from camera in real time. When initializing
 * this class, method loadClassifiers() must be called before any other calls to this class. Some method will not work, unless classifiers are loaded.
 * For clothes mapping on standalone images use method detectAndAddClothing(), this method excepts images as Mat or Bitmap. Image given to these methods
 * will not be changed and the result of clothes mapping will be returned respectively as Mat or Bitmap.
 *
 * For real time image mapping use methods: addClothesRT(), getFace() and recalculateWaistWidth(). Method addClothesRT requires rectangle representing
 * a face, you can acquire this rectangle with the help of the method getFace(). I recommend not running method getFace() in UI thread as it is computationally
 * expensive and will result in slowing the application down heavily. Instead run method getFace() in a separate thread. Example of the usage of the two methods
 * can be seen in the app example. When the method addClothesRT() is called with Outfit that contains Tops, waistWidth is calculated. This width is saved
 * and used until the face disappears from the camera's view. If needed the method recalculateWaistWidth() deletes previously calculated waist width,
 * which will be recalculated by method addClothesRT() once needed again.
 *
 * For more specific information see method descriptions.
 *
 * WARNING: DressingRoom uses OpenCV's class Mat to store images as it is the main image representation OpenCV works with. Mat variables seem to be
 * causing memory leaks, when used wrong. Caution is advised when working with Mat variables or classes using them. In case of inconsistencies
 * in Android Studio's profiler, when detecting a memory leak, try: closing the project, deleting the app from device, restart all programs
 * and devices and open a copy of the project. This seems to have fixed the issue, but cause of the issue is unknown to me.
 */
public class DressingRoom {
    private static final String TAG = "Dressing Room";
    private static final double MIN_FACE_SIZE = 0.07; // percentage of image's width, minSize for face detection
    private static final double MAX_FACE_SIZE = 0.8;  // percentage of image's width, maxSize for face detection

    private static CascadeClassifier frontalFaceClassifier; // better results
    private static CascadeClassifier frontalFaceClassifier2; // slightly faster

    private static CascadeClassifier eyesClassifier2; // second classifier detects eyes with glasses better

    private static final double EYE_LEVEL = 0.4; //percentage of the face's HEIGHT, manipulates GLASSES' Y coordinate
    private static final double MASK_SHIFT = 1.15; //percentage of the face's HEIGHT, manipulates MASK's Y coordinate
    private static final double FACE_WIDTH_MULTIPLIER = 0.8; //percentage of the face's WIDTH, changes the width of GLASSES and MASK

    private static final double NECK_SHIFT = 1.3; //percentage of the face's HEIGHT, manipulates TOP's Y coordinate
    private static final double WAIST_SHIFT = 3.2; //percentage of the face's HEIGHT, determines where will the waist be measured
    private static final double WAIST_WIDTH = 1.05; //changes TOP's width
    private static final double MAX_WAIST_REF_DIFFERENCE = 0.05; //percentage of the input image's width

    //photo
    private static final double WAIST_WIDTH_APPROX = 1.85; //percentage of the face's WIDTH, used when estimating hip width

    //real time
    private static double previousWaist = 0;


    /***
     * Method initializes Haar cascade classifiers and has to be called before calling any other methods, that require detection.
     * @param context application context
     * @throws AssertionError loading of classifiers failed
     */
    public static void loadClassifiers(Context context) {
        frontalFaceClassifier = HaarCascade.loadClassifier(context, R.raw.haarcascade_frontalface_alt, "haarcascade_frontalface_alt.xml");
        frontalFaceClassifier2 = HaarCascade.loadClassifier(context, R.raw.haarcascade_frontalface_alt2, "haarcascade_frontalface_alt2.xml");
        eyesClassifier2 = HaarCascade.loadClassifier(context, R.raw.haarcascade_eye_tree_eyeglasses, "haarcascade_eye_tree_eyeglasses.xml");
        if(frontalFaceClassifier == null || frontalFaceClassifier2 == null || eyesClassifier2 == null) throw new AssertionError("One of the Haar Cascade classifiers for face detecting in DressingRoom.java wasn't loaded properly.");
    }


    /**
     * Detects a person in Bitmap and adds clothing from Outfit. Note that this method should only be called for standalone images.
     * @param source bitmap with a person
     * @param outfit outfit with all the clothes, that are going to be added
     * @return Bitmap with added clothes to the original image.
     */
    public static Bitmap detectAndAddClothing(Bitmap source, Outfit outfit) throws IOException {
        Mat imageMat = Convert.bitmapToMat(source);
        return Convert.matToBitmap(detectAndAddClothing(imageMat, outfit));
    }

    private static final ImageProcessor imageProcessor = new ImageProcessor();
    /**
     * Method detects a person in sourceImage and adds clothing from Outfit. Note that this method should only be called for standalone images.
     * This method makes sure the clothing is added, even if the waist isn't detected.
     * In cases where waist isn't detected an estimate is used. For more precise adding of faceMasks and Glasses,
     * a face rotation is calculated and the whole image is rotated by detected angle * -1.
     * Face detection is then run again with the rotated image as a parameter, that's when faceMasks and Glasses are added
     * and the image is then rotated back and cropped to it's original size.
     * @param sourceImage bitmap with a person (method throws IOException if no face is detected)
     * @param outfit outfit with all the clothes, that are going to be added
     * @return Mat with added clothes to the original image.
     */
    public static Mat detectAndAddClothing(Mat sourceImage, Outfit outfit) throws IOException {
        if(outfit.isEmpty()) return sourceImage;

        // works with a face, that is closest to the camera (the biggest detected rectangle representing a face)
        Rect face = detectFace(sourceImage);
        if(face.empty()) throw new IOException("No face detected");

        Mat result = sourceImage.clone();

        // detecting head rotation, rotating image and running face detection again to get face measurements
        double angle = imageProcessor.getHeadRotationAngle(sourceImage, face, eyesClassifier2, EYE_LEVEL);
        Mat rotatedImage = new Mat();
        imageProcessor.rotateImage(sourceImage, rotatedImage, angle*-1);
        Rect rotatedFace = detectFace(rotatedImage);

        if(rotatedFace.empty()) {
            // ADDING MASK and GLASSES (won't be rotated)
            addGlasses(result, outfit, face);
            addFaceMasks(result, outfit, face);
            addTops(outfit, face, result);
            return result;
        }

        // ADDING MASK and GLASSES (will be rotated based on head rotation)
        addFaceMasks(rotatedImage, outfit, rotatedFace);
        addGlasses(rotatedImage, outfit, rotatedFace);

        //rotating image back and cropping it to it's original size
        Rect roi = new Rect((rotatedImage.width()-sourceImage.width())/2, (rotatedImage.height()-sourceImage.height())/2, sourceImage.width(), sourceImage.height());
        Mat temp = new Mat();
        imageProcessor.rotateImageBack(rotatedImage, temp, angle);
        result = temp.submat(roi);

        rotatedImage.release();

        addTops(outfit, face, result); // ADDING TOPS on unrotated image, needs to be added last otherwise it's changing face detection

        return result;
    }

    private static final HaarCascade haarCascade = new HaarCascade();
    /***
     * Runs basic face detection, no additional calculation are being made like it is in method getFace().
     * Method uses two different classifiers to ensure a detection. If no faces were detected by the first classifier, second one is used.
     * This methods should only be used for detection in standalone images.
     * @param source image
     * @return face closest to the camera
     */
    private static Rect detectFace(Mat source) {
        // finding face in the image
        List<Rect> haarDetectionResults = haarCascade.applyClassifier(source, frontalFaceClassifier, (int)Math.round(source.cols()*MIN_FACE_SIZE), (int)Math.round(source.cols()*MAX_FACE_SIZE));

        if(haarDetectionResults.isEmpty()) {
            // if no face is detected with the first classifier, detection is run again with the second classifier
            haarDetectionResults = haarCascade.applyClassifier(source, frontalFaceClassifier2, (int)Math.round(source.cols()*MIN_FACE_SIZE), (int)Math.round(source.cols()*MAX_FACE_SIZE));
            if(haarDetectionResults.isEmpty()) {
                // if the second classifier fails to detect face as well, empty rectangle is returned
                return new Rect();
            }
        }
        // works with a face, that is closest to the camera (the biggest detected rectangle representing a face)
        return haarDetectionResults.stream().max(Comparator.comparing(Rect::area)).orElse(haarDetectionResults.get(0));
    }

    /***
     * Method calculates the reference point and the width of the subject's waist. If waist can't be detected using contours,
     * an estimate is used instead. Method then calls method drawClothesOnImage to add all tops from Outfit to sourceImage.
     * Reference point for tops is the neckPoint. Reference point is calculated from the rectangle representing face.
     * X = middle of the face rectangle, Y = end of the face
     * WARNING: Method rewrites sourceImage! And should only be used for standalone images.
     * */
    private static void addTops(Outfit outfit, Rect face, Mat result) {
        if (!outfit.getTops().isEmpty()) {
            Point neckPoint = new Point(Math.round(face.x + (double) face.width/2), Math.round(face.y + face.height*NECK_SHIFT));
            double waistWidth = calculateWaistWidth(result, face, 0.08, 5);
            int waistWidthEstimate = (int) Math.round(face.width * WAIST_WIDTH_APPROX);
            double waist = (waistWidth==0 ? waistWidthEstimate : waistWidth* face.width) * WAIST_WIDTH;

            for (Top t : outfit.getTops()) {
                drawClothingOnImage(result, t, waist/t.getReferenceWidth(), neckPoint);
            }
        }
    }

    /***
     * Method calculates the reference point and calls method drawClothesOnImage to add all face masks from Outfit to sourceImage.
     * Reference point for a face mask is the chin. Reference point is calculated from the rectangle representing face.
     * X = middle of the face rectangle, Y = end of the face
     * WARNING: Method rewrites sourceImage!
     * */
    private static void addFaceMasks(Mat sourceImage, Outfit outfit, Rect face) {
        if (!outfit.getFaceMasks().isEmpty()) {
            Point chin = new Point(face.x + face.width/2d, face.y + face.height*MASK_SHIFT);
            for(FaceMask mask : outfit.getFaceMasks()) {
                double scale = (face.width*FACE_WIDTH_MULTIPLIER)/mask.getReferenceWidth();
                drawClothingOnImage(sourceImage, mask, scale, chin);
            }
        }
    }

    /***
     * Method calculates the reference point and calls method drawClothesOnImage to add all glasses from Outfit to sourceImage.
     * Reference point for a glasses is the nose bridge. Reference point is calculated from the rectangle representing face.
     * X = middle of the face rectangle, Y = eye level
     * WARNING: Method rewrites sourceImage!
     * */
    private static void addGlasses(Mat sourceImage, Outfit outfit, Rect face) {
        if (!outfit.getGlasses().isEmpty()) {
            Point noseBridge = new Point(Math.round(face.x + face.width/2d),Math.round(face.y + face.height*EYE_LEVEL));
            for (Glasses g: outfit.getGlasses()) {
                double scale = (face.width*FACE_WIDTH_MULTIPLIER)/g.getReferenceWidth();
                drawClothingOnImage(sourceImage, g, scale, noseBridge);
            }
        }
    }

    /***
     * Methods copies clothes from outfit to sourceImage. This method is meant to be used when adding clothes
     * to a person in real time. Do not use this method for adding clothing to standalone images, clothing might
     * not be properly displayed, use method detectAndAddClothing() instead.
     * @param sourceImage source image from camera
     * @param outfit outfit with all the clothes, that are going to be added
     * @param face rectangle representing a face, use method getFace() to obtain it
     */
    public static void addClothesRT(Mat sourceImage, Outfit outfit, Rect face) {
        if(sourceImage==null || sourceImage.empty()) throw new IllegalArgumentException("source image cannot be null or empty");

        addFaceMasks(sourceImage, outfit, face);
        addGlasses(sourceImage, outfit, face);
        addTopsRT(sourceImage, outfit, face);
    }

    /***
     * Method calculates the reference point and the width of the subject's waist. If waist wasn't detected, previously detected
     * waist width is used. Default value of waist is 0, so if no previous waist was detected clothes won't appear on camera until waist
     * is calibrated. This method should be used in real time and not on standalone images and appearance of clothes in images isn't promised.
     * Method then calls method drawClothesOnImage to add all tops from Outfit to sourceImage.
     * Reference point for tops is the neckPoint. Reference point is calculated from the rectangle representing face.
     * X = middle of the face rectangle, Y = end of the face
     * WARNING: Method rewrites sourceImage! And should only be used for real time clothes mapping.
     * */
    private static void addTopsRT(Mat sourceImage, Outfit outfit, Rect face) {
        if (!outfit.getTops().isEmpty()) {
            Point neckPoint = new Point(Math.round(face.x + (double) face.width/2), Math.round(face.y + face.height*NECK_SHIFT));
            if(previousWaist==0) previousWaist = calculateWaistWidth(sourceImage, face, 0.06, 25); //changes previousWaist
            if(previousWaist!=0) {
                double waist = previousWaist*WAIST_WIDTH* face.width;
                for (Top t : outfit.getTops()) {
                    drawClothingOnImage(sourceImage, t, waist/t.getReferenceWidth(), neckPoint);
                }
            }
        }
    }

    /*** Method resets waist values and new waist calculation will be done when waist width is needed. */
    public static void recalculateWaistWidth() {
        previousWaist = 0;
        waistValuesMap.clear();
    }

    /*** Method calculates ROIs (Regions Of Interest) in the approximate position of the waist, one on the left and one on the right side.
     * Contours are then extracted from these ROIs. For loop begins in the vertical center of the ROI, and then goes up and down
     * searching for a match. The loop first selects one point from each ROI, that is the closes to the center of the body (center of the face rectangle),
     * The points are considerate a match if the differance between the distances of these to points to the center is less than MAX_WAIST_REF_DIFFERENCE.
     * When a match is detected, waist width is passed to waistCalibration method. If the return value of waistCalibration() is true, waist was successfully
     * calibrated and waist width value is returned. */
    private static double calculateWaistWidth(Mat sourceImage, Rect face, double precision, int minMatches) {
        double roiWidthMultiplier = 1.5;
        int roiWidth = (int)Math.round(face.width*roiWidthMultiplier);

        int roiY = face.y+(int)Math.round(face.height*(WAIST_SHIFT-0.25)); //0.25 for half the roi's height (1/4 of the face's height)

        // trying to find left edge of the body at waist level using contours
        Rect roiLeftHip = new Rect((int) Math.round(face.x - face.width * roiWidthMultiplier), roiY, roiWidth, face.height / 2);
        List<Point> contoursLeft = getContoursFromROI(sourceImage, roiLeftHip);

        // trying to find right edge of the body at waist level using contours
        Rect roiRightHip = new Rect(face.x + face.width, roiY, roiWidth, face.height / 2);
        List<Point> contoursRight = getContoursFromROI(sourceImage, roiRightHip);

        int approxY = (int) Math.round(face.height/4.0);
        boolean decreasing = true;
        for(int i=0; i<approxY; i++) {
            int y = decreasing ? approxY - i : approxY + i;

            Optional<Point> left = Optional.empty();
            if(contoursLeft != null) left= contoursLeft.stream().filter(point -> point.y == y).max(Comparator.comparingDouble(p -> p.x));
            Optional<Point> right = Optional.empty();
            if(contoursRight != null) right = contoursRight.stream().filter(point -> point.y == y).min(Comparator.comparingDouble(p -> p.x));

            if(left.isPresent() && right.isPresent()) { //if both points were found
                Point refLeftHip = new Point(left.get().x + roiLeftHip.x, left.get().y + roiLeftHip.y); //getting global coords
                Point refRightHip = new Point(right.get().x + roiRightHip.x, right.get().y + roiRightHip.y); //getting global coords

                int middle = face.x + face.width / 2;
                int rightDist = (int)refRightHip.x - middle;
                int leftDist = (int)(middle - refLeftHip.x);

                // checks if detected points have roughly the same distance from the center of the body
                if (Math.abs(rightDist - leftDist) <= sourceImage.cols()*MAX_WAIST_REF_DIFFERENCE) {
                    double waistWidth = refRightHip.x-refLeftHip.x;
                    if(waistCalibration(waistWidth/face.width, precision, minMatches)) return previousWaist;
                }
            }

            // loop starts in the center and goes up and down
            if(i>0) {
                if(decreasing) i--;
                decreasing = !decreasing;
            }

        } //for loop end

        return 0;
    }

    private static Mat subMat; //declaration of Mat variables in the method was causing memory leaks
    /***
     * Method calculates ROI and detects contours in the region of the source image given by ROI.
     * @param source image
     * @param roi rectangle indicating an area of source image
     * @return contours from roi
     */
    private static List<Point> getContoursFromROI(Mat source, Rect roi) {
        // checking if roi is out of image's bounds
        if(roi.x<0) {
            roi.width += roi.x;
            roi.x = 0;
        }
        if(roi.y<0) {
            roi.height += roi.y;
            roi.y = 0;
        }
        if(roi.x+roi.width>source.width()) roi.width = source.width() - roi.x;
        if(roi.y+roi.height>source.height()) roi.height = source.height() - roi.y;

        if(roi.width <= 0 || roi.height <= 0) return null;

        // getting contours from submat
        subMat = source.submat(roi);
        List<Point> points = Contours.getVerticalContours(subMat);
        subMat.release();

        return points;
    }

    private static final Map<Double, List<Double>> waistValuesMap = new HashMap<>();
    /***
     * Every detected waist width is passed to this method. Method creates a HashMap, where keys are the increments of precision variable.
     * Precision indicates how precise calibration will be, because every HashMap entry will contain a  increment of the precision and a list.
     * The list contains waist width values satisfying the interval: key - precision/2 < waist <= key + (3*precision)/2.
     * When any list size passes minMatches, calibration is completed and a median of the values in this list is returned.
     * @param newValue detected waist width
     * @param precision detected waist width / face's width
     * @param minMatches minimum number of matches, that will end the calibration
     * @return calibrated waist width value in relation to face width
     */
    private static boolean waistCalibration(double newValue, double precision, int minMatches) {
        double i = (3*precision)/2;
        while(newValue>i) {
            i+=precision;
        }
        double key = i - precision/2;
        if(waistValuesMap.containsKey(key)) {
            // if map already contains an entry with this key, newValue is added to the corresponding list
            Objects.requireNonNull(waistValuesMap.get(key)).add(newValue);
        } else {
            // if map doesn't have an entry with this key, entry is create with a new list containing newValue
            List<Double> list = new ArrayList<>();
            list.add(newValue);
            waistValuesMap.put(key, list);
        }

        List<Double> waistValues = waistValuesMap.get(key);
        assert waistValues != null;
        int waistValuesSize = waistValues.size();

        // if number of values in the list reaches MIN_WAIST_MATCHES, the waist is set to median of the values in the list
        if(waistValuesSize >= minMatches) {
            Collections.sort(waistValues);
            if (waistValuesSize % 2 == 1) {
                previousWaist = waistValues.get((waistValuesSize + 1) / 2 - 1);
            }
            else {
                double lowerMiddle = waistValues.get(waistValuesSize / 2 - 1);
                double upperMiddle = waistValues.get(waistValuesSize / 2);
                previousWaist = (lowerMiddle + upperMiddle) / 2.0;
            }
            return true;
        }

        return false;
    }



    private static final HaarCascade haarCascade2 = new HaarCascade();
    private static Mat reducedImage; //declaration of Mat variables in the method was causing memory leaks
    private static Rect previousFace = new Rect(0,0,0,0); // used for stabilization and preventing false negative detections
    private static final int MAX_FACE_DIFFERENCE = 5; // the amount of pixels that two face detections can differ before the change is acknowledged, used in stabilization
    private static final int MAX_SKIPPED_FRAMES = 5;
    private static int skippedFramesCounter = 0;
    /***
     * Method runs Haar cascade detection in parameter image to find faces. If no faces are detected returns empty rectangle.
     * If multiple faces are detected, method returns face closest to the camera. Method should be used, when adding clothes in real time.
     * It's recommended not to run this method on the UI thread.
     * @param image frame from the camera
     * @return rectangle representing detected face
     */
    public static Rect getFace(Mat image) {
        // detection will run only on the upper half of the picture to reduce runtime
        Rect roi = new Rect(0,0, image.cols(), (int) Math.round(image.rows()/2d));
        reducedImage = image.submat(roi);

        // finding face in the image
        List<Rect> haarDetectionResults = haarCascade2.applyClassifier(reducedImage, frontalFaceClassifier2, (int)Math.round(image.cols()*MIN_FACE_SIZE), (int)Math.round(image.cols()*MAX_FACE_SIZE));

        reducedImage.release();
        image.release();

        if(haarDetectionResults.isEmpty()) {
            // fixes false negative detections
            skippedFramesCounter++;
            if (skippedFramesCounter > MAX_SKIPPED_FRAMES) {
                previousFace = new Rect(0,0,0,0);
                previousWaist = 0;
                skippedFramesCounter = 0;
                recalculateWaistWidth(); //when the face disappears from the frame, the waistWidth is reset
            }
            return previousFace;
        } else {
            skippedFramesCounter = 0;
        }

        // selects face closest to the camera (rect with the biggest area)
        Rect result = haarDetectionResults.stream().max(Comparator.comparing(Rect::area)).orElse(haarDetectionResults.get(0));
        // without stabilization clothing appears to be shaking, due to small changes in face detection even if the device isn't moving
        result = stabilizeFaceDetection(result);

        previousFace = result;
        return result;
    }

    /**
     * If any variables of the newly detected face differ from the variables of the previously detected face
     *  by more than MAX_FACE_DIFFERENCE method returns new face, otherwise method return previously detected face.
     *  Calculations of the clothes' width and position are heavily dependent of these values and when stabilization
     *  isn't used, slight changes to the face's rectangle result in rapid resizing and movements of the clothes.
     *  */
    private static Rect stabilizeFaceDetection(Rect newFace) {
        if(isMoreThanDiff(previousFace.x, newFace.x)
                || isMoreThanDiff(previousFace.y, newFace.y)
                || isMoreThanDiff(previousFace.width, newFace.width)
                || isMoreThanDiff(previousFace.height, newFace.height)) {
            return newFace;
        } else return previousFace;
    }

    private static boolean isMoreThanDiff(int oldValue, int newValue) {
        return Math.abs(oldValue-newValue) > MAX_FACE_DIFFERENCE;
    }

    private static Mat clothingROI, resultROI; //declaration of Mat variables in the method was causing memory leaks
    private static final Mat clothingResized = new Mat();
    /**
     * Method first resizes the clothes, then calculates regions of interest (ROIs) for clothes and the original image.
     * If clothing ranges outside the original image, ROI of the clothes is cropped.
     * Clothes ROI is then copied to original image's ROI. ROIs are necessary, because method copyTo() only works with
     * images that are the same size.
     * */
    private static void drawClothingOnImage(Mat orgImage, DressingRoomClothes clothes, double scale, Point orgCenter) {
        // resizing clothes to fit
        int width = (int) Math.round(clothes.getSourceImage().width() * scale);
        int height = (int) Math.round(clothes.getSourceImage().height() * scale);
        Imgproc.resize(clothes.getSourceImage(), clothingResized, new Size(width, height));

        // new center coordinates
        Point center = new Point(clothes.getReferenceCenter().x * scale, clothes.getReferenceCenter().y * scale);

        // calculating ROIs for source image and for clothes, method CopyTo() used for merging mats requires mats to be the same size
        int roiX = (int)(orgCenter.x-center.x);
        int roiY = (int)(orgCenter.y-center.y);
        int roiWidth = clothingResized.width();
        int roiHeight = clothingResized.height();

        int clothingRoiX = 0;
        int clothingRoiY = 0;

        // roi starts outside the original photo's plane, mat with clothing needs to be cropped
        if (roiX < 0) {
            clothingRoiX = -roiX;
            roiWidth += roiX;
            roiX = 0;
        }
        // roi is completely outside the original photo's plane, drawing isn't necessary
        else if (roiX > orgImage.width()) return;

        // the same, but for roi's Y coordinate
        if (roiY < 0) {
            clothingRoiY = -roiY;
            roiHeight += roiY;
            roiY = 0;
        }
        else if (roiY > orgImage.height()) return;

        // if the roi spreads outside the original photo's plane
        if(roiX+roiWidth > orgImage.width()) roiWidth = orgImage.width() - roiX;
        if(roiY+roiHeight > orgImage.height()) roiHeight = orgImage.height() - roiY;

        // creating clothing ROI in case the clothing is outside the original photo's plane
        Rect roiClothes = new Rect(clothingRoiX, clothingRoiY, roiWidth, roiHeight);
        clothingROI = clothingResized.submat(roiClothes);

        // creating original image's roi
        Rect roi = new Rect(roiX, roiY, roiWidth, roiHeight);
        resultROI = orgImage.submat(roi);

        // merging images
        List<Mat> channels = new ArrayList<>();
        Core.split(clothingROI, channels);
        if(channels.size() > 3) clothingROI.copyTo(resultROI, channels.get(3)); // overlaying images with alpha channel as a mask
        else {
            clothingROI.copyTo(resultROI);
            Log.e(TAG, "Alpha channel not found");
        }

        for(int i=0; i<channels.size(); i++) {
            channels.get(i).release();
        }
        clothingROI.release();
        resultROI.release();
        clothingResized.release();
    }

}