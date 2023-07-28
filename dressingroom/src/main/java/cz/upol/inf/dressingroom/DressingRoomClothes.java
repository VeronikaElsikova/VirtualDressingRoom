package cz.upol.inf.dressingroom;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.core.Point;

/***
 * Abstract class for all clothing types, all clothing types must extend this class.
 */
abstract class DressingRoomClothes {
    private Mat src;
    private Point leftReferencePoint;
    private Point rightReferencePoint;

    public DressingRoomClothes(Mat src, Point leftReferencePoint, Point rightReferencePoint) {
        this.src = src;
        this.leftReferencePoint = leftReferencePoint;
        this.rightReferencePoint = rightReferencePoint;
    }

    public DressingRoomClothes(Bitmap src, Point leftReferencePoint, Point rightReferencePoint) {
        this.src = Convert.bitmapToMat(src);
        this.leftReferencePoint = leftReferencePoint;
        this.rightReferencePoint = rightReferencePoint;
    }

    public Mat getSourceImage() {
        return src;
    }
    public void setSourceImage(Bitmap src) {
        this.src = Convert.bitmapToMat(src);
    }
    public void setSourceImage(Mat src) {
        this.src = src;
    }

    public Point getLeftReferencePoint() {
        return leftReferencePoint;
    }
    public void setLeftReferencePoint(Point leftReferencePoint) {
        this.leftReferencePoint = leftReferencePoint;
    }

    public Point getRightReferencePoint() {
        return rightReferencePoint;
    }
    public void setRightReferencePoint(Point rightReferencePoint) {
        this.rightReferencePoint = rightReferencePoint;
    }

    /***
     * @return center of a line created by the two reference points
     */
    public Point getReferenceCenter() {
        return new Point((rightReferencePoint.x + leftReferencePoint.x)/2, (rightReferencePoint.y + leftReferencePoint.y)/2);
    }

    /***
     * @return the distance between the two reference points
     * */
    public float getReferenceWidth() {
        Point diff = new Point(rightReferencePoint.x - leftReferencePoint.x, rightReferencePoint.y - leftReferencePoint.y);
        return Math.round(Math.sqrt(diff.x*diff.x + diff.y*diff.y));
    }
}
