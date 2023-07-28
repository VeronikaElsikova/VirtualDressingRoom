package cz.upol.inf.dressingroom;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.core.Point;

public class FaceMask extends DressingRoomClothes {
    /***
     * Creates new FaceMask object with source and reference points.
     * @param src source image
     * @param leftReferencePoint x = pixel value where mask sits on the left edge of the face,
     *                           y = pixel value where mask sits on the bottom of the face
     * @param rightReferencePoint x = pixel value where mask sits on the right edge of the face,
     *                            y = pixel value where mask sits on the bottom of the face
     */
    public FaceMask(Mat src, Point leftReferencePoint, Point rightReferencePoint) {
        super(src, leftReferencePoint, rightReferencePoint);
    }

    /***
     * Creates new FaceMask object with source and reference points. Converts Bitmap source to Mat.
     * @param src source image
     * @param leftReferencePoint x = pixel value where mask sits on the left edge of the face,
     *                           y = pixel value where mask sits on the bottom of the face
     * @param rightReferencePoint x = pixel value where mask sits on the right edge of the face,
     *                            y = pixel value where mask sits on the bottom of the face
     */
    public FaceMask(Bitmap src, Point leftReferencePoint, Point rightReferencePoint) {
        super(src, leftReferencePoint, rightReferencePoint);
    }

    /***
     * Creates new FaceMask object with source and reference points. Reference points are calculated automatically,
     * for better precision add reference points manually. Converts Bitmap source to Mat.
     * @param src source image
     */
    public FaceMask(Bitmap src) {
        super(src, new Point(0, src.getHeight()), new Point(src.getWidth(), src.getHeight()));
    }

    /***
     * Creates new FaceMask object with source and reference points. Reference points are calculated automatically,
     * for better precision add reference points manually.
     * @param src source image
     */
    public FaceMask(Mat src) {
        super(src, new Point(0, src.height()), new Point(src.width(), src.height()));
    }
}
