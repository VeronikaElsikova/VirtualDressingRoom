package cz.upol.inf.dressingroom;

import java.util.ArrayList;
import java.util.List;

/***
 * WARNING: Class Outfit uses OpenCV's class Mat to store images in lists to allow multiple clothes of the same type to bet added. When images are added
 * as Bitmap, they are automatically converted to Mat. Mat variables may cause memory leaks, caution is advised when working with Mat variables
 * or classes using them. In case of inconsistencies in Android Studio's profiler try closing the project, deleting the app from device, restart
 * all programs and devices and open a copy of the project. This seems to have fixed the issue, but cause of the issue is unknown to me.
 */
public class Outfit {
    //if any other clothing type is added to outfit, add check to isEmpty() method
    private List<Glasses> glasses = new ArrayList<>();
    private List<FaceMask> faceMasks = new ArrayList<>();
    private List<Top> tops = new ArrayList<>();

    /*** Creates an empty Outfit. */
    public Outfit() {}

    /***
     * Created Outfit from given glasses, faceMasks and Tops. Values can be null.
     * @param glasses glasses
     * @param faceMask faceMask
     * @param top top
     */
    public Outfit(Glasses glasses, FaceMask faceMask, Top top) {
        if(glasses!=null) this.glasses.add(glasses);
        if(faceMask!=null) this.faceMasks.add(faceMask);
        if(top!=null) this.tops.add(top);
    }

    /***
     *  Creates a shallow copy of the given outfit. Method creates new lists, but clothing classes aren't cloned.
     * @param outfit outfit
     */
    public Outfit(Outfit outfit) {
        this.glasses = new ArrayList<>(outfit.getGlasses());
        this.faceMasks = new ArrayList<>(outfit.getFaceMasks());
        this.tops = new ArrayList<>(outfit.getTops());
    }

    // GLASSES
    /*** Returns all glasses */
    public List<Glasses> getGlasses() {
        return glasses;
    }
    /*** Adds glasses to list of glasses */
    public void addGlasses(Glasses glasses) {
        this.glasses.add(glasses);
    }
    /*** Deletes all glasses and adds glasses to list of glasses */
    public void setGlasses(Glasses glasses) {
        this.glasses.clear();
        if(glasses!=null) this.glasses.add(glasses);
    }
    /*** deletes all glasses */
    public void clearGlasses() {
        this.glasses.clear();
    }

    // MASKS
    /*** Returns all masks */
    public List<FaceMask> getFaceMasks() {
        return faceMasks;
    }
    /*** Adds faceMask to list of face masks */
    public void addFaceMask(FaceMask faceMask) {
        this.faceMasks.add(faceMask);
    }
    /*** Deletes all face masks and adds faceMask to list of face masks */
    public void setFaceMask(FaceMask faceMask) {
        this.faceMasks.clear();
        if(faceMask!=null) this.faceMasks.add(faceMask);
    }
    /*** deletes all faceMasks */
    public void clearMasks() {
        this.faceMasks.clear();
    }

    // TOPS
    /*** Returns all tops */
    public List<Top> getTops() {
        return tops;
    }
    /*** Adds top to list of tops */
    public void addTop(Top top) {
        this.tops.add(top);
    }
    /*** Deletes all tops and adds top to list of tops */
    public void setTop(Top top) {
        this.tops.clear();
        if(top!=null) this.tops.add(top);
    }
    /*** deletes all tops */
    public void clearTops() {
        this.tops.clear();
    }

    /***
     * Method checks if all list of all types of clothing are empty. If adding new clothing type, additional condition must be added to this method!
     * @return true if outfit is empty, false if not
     */
    public boolean isEmpty() {
        //if added any other clothing to outfit, add null check here
        return tops.isEmpty() && glasses.isEmpty() && faceMasks.isEmpty();
    }

}
