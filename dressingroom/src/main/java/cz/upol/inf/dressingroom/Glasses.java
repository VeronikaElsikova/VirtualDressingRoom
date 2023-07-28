package cz.upol.inf.dressingroom;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.core.Point;


public class Glasses extends DressingRoomClothes {
    /***
     * Creates new Glasses object with source and reference points.
     * @param src source image
     * @param leftReferencePoint x = pixel value where glasses sit on the left edge of the face,
     *                           y = pixel value that should be matched to the eye level
     * @param rightReferencePoint x = pixel value where glasses sit on the right edge of the face,
     *                            y = pixel value that should be matched to the eye level
     */
    public Glasses(Mat src, Point leftReferencePoint, Point rightReferencePoint) {
        super(src, leftReferencePoint, rightReferencePoint);
    }

    /***
     * Creates new Glasses object with source and reference points. Converts Bitmap source to Mat.
     * @param src source image
     * @param leftReferencePoint x = pixel value where glasses sit on the left edge of the face,
     *                           y = pixel value that should be matched to the eye level
     * @param rightReferencePoint x = pixel value where glasses sit on the right edge of the face,
     *                            y = pixel value that should be matched to the eye level
     */
    public Glasses(Bitmap src, Point leftReferencePoint, Point rightReferencePoint) {
        super(src, leftReferencePoint, rightReferencePoint);
    }

    /***
     * Creates new Glasses object with source and reference points. Reference points are calculated automatically,
     * for better precision add reference points manually. Converts Bitmap source to Mat.
     * @param src source image
     */
    public Glasses(Bitmap src) {
        super(src, new Point(0, (double)src.getHeight()/3), new Point(src.getWidth(), (double)src.getHeight()/3));
    }

    /***
     * Creates new Glasses object with source and reference points. Reference points are calculated automatically,
     * for better precision add reference points manually.
     * @param src source image
     */
    public Glasses(Mat src) {
        super(src, new Point(0, (double)src.height()/3), new Point(src.width(), (double)src.height()/3));
    }

}
