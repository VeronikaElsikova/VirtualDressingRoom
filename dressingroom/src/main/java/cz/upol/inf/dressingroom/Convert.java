package cz.upol.inf.dressingroom;

import android.graphics.Bitmap;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class Convert {
    /***
     * Creates new Mat from given Bitmap.
     * @param bitmap source image
     * @return created mat
     */
    public static Mat bitmapToMat(Bitmap bitmap) {
        Mat mat = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, mat);
        return mat;
    }

    /***
     * Creates new Bitmap from given Mat.
     * @param mat source image
     * @return created bitmap
     */
    public static Bitmap matToBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }
}
