package com.winlator.xserver;
import android.util.SparseArray;
import java.util.HashMap;
import java.util.Map;

public class ColormapManager extends XResourceManager {
    
    private SparseArray<Colormap> colorMaps; 
    
    public ColormapManager() {
        colorMaps = new SparseArray<>();
    }
    
    public void createColorMap(int id) {
        Colormap map = new Colormap(id);
        triggerOnCreateResourceListener(map);
        colorMaps.append(id, map);
    }
    
    public void removeColorMap(int id) {
        Colormap map = new Colormap(id);
        colorMaps.remove(id);
    }
    
    public Colormap getColorMap(int id) {
        return colorMaps.get(id);
    }
}
