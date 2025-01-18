package com.winlator.xserver.requests;
import android.util.Log;
import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xserver.Bitmask;
import com.winlator.xserver.Window;
import com.winlator.xserver.WindowAttributes;
import com.winlator.xserver.XClient;
import com.winlator.xserver.XServer;

public abstract class ColormapRequests {
    
    public static void createColormap(XClient client, XInputStream inputStream, XOutputStream outputStream) {
        byte alloc = client.getRequestData();
        int colormapId = inputStream.readInt();
        int windowId = inputStream.readInt();
        int visualId = inputStream.readInt();
        client.xServer.colormapManager.createColorMap(colormapId);
    }
    
    public static void freeColormap(XClient client, XInputStream inputStream, XOutputStream outputStream) {
        client.xServer.colormapManager.removeColorMap(inputStream.readInt());
    }
}
