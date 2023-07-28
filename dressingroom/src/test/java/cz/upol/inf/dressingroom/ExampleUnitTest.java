package cz.upol.inf.dressingroom;

import org.junit.Test;
import org.opencv.core.Rect;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void getRectWithLargestArea_isCorrect() {
        List<Rect> list = new ArrayList<>();
        Rect rect1 = new Rect(0,0, 50, 100);
        Rect rect2 = new Rect(50,100, 55, 105);
        Rect rect3 = new Rect(25,50, 20, 10);
        Rect rect4 = new Rect(80,90, 15, 10);
        Rect rect5 = new Rect(0,0, 25, 100);
        list.add(rect1);
        list.add(rect2);
        list.add(rect3);
        list.add(rect4);
        list.add(rect5);
        assertEquals(rect2, list.stream().max(Comparator.comparing(Rect::area)).orElse(list.get(0)));
    }

    @Test
    public void zeroRectange_isEmpty() {
        assertTrue((new Rect(0,0,0,0)).empty());
    }

}