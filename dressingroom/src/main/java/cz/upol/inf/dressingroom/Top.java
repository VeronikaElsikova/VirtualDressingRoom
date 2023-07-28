package cz.upol.inf.dressingroom;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.core.Point;

public class Top extends DressingRoomClothes {
    /***
     * Creates new Top object with source and reference points.
     * @param src source image
     * @param leftReferencePoint x = left edge of top on waist level, y = pixel value that should be matched to the shoulder level
     * @param rightReferencePoint x = right edge of top on waist level, y = pixel value that should be matched to the shoulder level
     */
    public Top(Mat src, Point leftReferencePoint, Point rightReferencePoint) {
        super(src, leftReferencePoint, rightReferencePoint);
    }

    /***
     * Creates new Top object with source and reference points. Converts Bitmap source to Mat.
     * @param src source image
     * @param leftReferencePoint x = left edge of top on waist level, y = pixel value that should be matched to the shoulder level
     * @param rightReferencePoint x = right edge of top on waist level, y = pixel value that should be matched to the shoulder level
     */
    public Top(Bitmap src, Point leftReferencePoint, Point rightReferencePoint) {
        super(src, leftReferencePoint, rightReferencePoint);
    }

}
