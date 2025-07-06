package com.winlator.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.winlator.R;
import com.winlator.contents.ContentProfile;
import com.winlator.contents.ContentsManager;
import com.winlator.core.AppUtils;
import com.winlator.core.FileUtils;
import com.winlator.core.KeyValueSet;
import com.winlator.core.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WineD3DConfigDialog extends ContentDialog {
    private final Context context;
    private JSONArray gpuCards;

    public WineD3DConfigDialog(View anchor) {
        super(anchor.getContext(), R.layout.wined3d_config_dialog);
        context = anchor.getContext();
        setIcon(R.drawable.icon_settings);
        setTitle("WineD3D "+context.getString(R.string.configuration));

        final Spinner sWineD3DVersion = findViewById(R.id.SWineD3DVersion);
        final Spinner sCSMT = findViewById(R.id.SCSMT);
        final Spinner sGPUName = findViewById(R.id.SGPUName);
        final Spinner sOffscreenRenderingMode = findViewById(R.id.SOffscreenRenderingMode);
        final Spinner sStrictShaderMath = findViewById(R.id.SStrictShaderMath);
        final Spinner sVideoMemorySize = findViewById(R.id.SVideoMemorySize);
        final Spinner sRenderer = findViewById(R.id.SRenderer);

        ContentsManager contentsManager = new ContentsManager(context);
        contentsManager.syncContents();
        loadWineD3DVersionSpinner(contentsManager, sWineD3DVersion);

        KeyValueSet config = DXVK_VKD3DConfigDialog.parseConfig(anchor.getTag());

        AppUtils.setSpinnerSelectionFromIdentifier(sWineD3DVersion, config.get("wined3d_version"));

        List<String> stateList = Arrays.asList(context.getString(R.string.disable), context.getString(R.string.enable));
        sCSMT.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, stateList));
        sCSMT.setSelection(Integer.parseInt(config.get("csmt")) != 0 ? 1 : 0);

        try {
            gpuCards = new JSONArray(FileUtils.readString(getContext(), "gpu_cards.json"));
        }
        catch (JSONException e) {}
        loadGPUNameSpinner(sGPUName, Integer.parseInt(config.get("deviceID")));

        List<String> offscreenRenderingModeList = Arrays.asList("Backbuffer", "FBO");
        sOffscreenRenderingMode.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, offscreenRenderingModeList));
        AppUtils.setSpinnerSelectionFromValue(sOffscreenRenderingMode, config.get("OffScreenRenderingMode"));

        sStrictShaderMath.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, stateList));
        sStrictShaderMath.setSelection(Math.min(Integer.parseInt(config.get("strict_shader_math")), 1));

        String videoMemorySize = config.get("VideoMemorySize");
        AppUtils.setSpinnerSelectionFromNumber(sVideoMemorySize, videoMemorySize);

        List<String> rendererList = Arrays.asList("gl", "vulkan", "no3d");
        sRenderer.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, rendererList));
        AppUtils.setSpinnerSelectionFromValue(sRenderer, config.get("renderer"));

        setOnConfirmCallback(() -> {
            config.put("wined3d_version", sWineD3DVersion.getSelectedItem().toString());
            config.put("csmt", sCSMT.getSelectedItemPosition() != 0 ? 3 : 0);
            config.put("OffScreenRenderingMode", sOffscreenRenderingMode.getSelectedItem().toString());
            config.put("strict_shader_math", sStrictShaderMath.getSelectedItemPosition());
            config.put("VideoMemorySize", StringUtils.parseNumber(sVideoMemorySize.getSelectedItem()));
            config.put("renderer", sRenderer.getSelectedItem().toString());

            try {
                JSONObject gpuName = gpuCards.getJSONObject(sGPUName.getSelectedItemPosition());
                config.put("deviceID", gpuName.getInt("deviceID"));
                config.put("vendorID", gpuName.getInt("vendorID"));
            }
            catch (JSONException e) {}

            anchor.setTag(config.toString());
        });
    }

    private void loadGPUNameSpinner(Spinner spinner, int selectedDeviceID) {
        List<String> values = new ArrayList<>();
        int selectedPosition = 0;

        try {
            for (int i = 0; i < gpuCards.length(); i++) {
                JSONObject item = gpuCards.getJSONObject(i);
                if (item.getInt("deviceID") == selectedDeviceID) selectedPosition = i;
                values.add(item.getString("name"));
            }
        }
        catch (JSONException e) {}

        spinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, values));
        spinner.setSelection(selectedPosition);
    }

    private void loadWineD3DVersionSpinner(ContentsManager manager, Spinner spinner) {
        String[] originalItems = context.getResources().getStringArray(R.array.wined3d_version_entries);
        List<String> itemList = new ArrayList<>(Arrays.asList(originalItems));

        for (ContentProfile profile : manager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_WINED3D)) {
            String entryName = ContentsManager.getEntryName(profile);
            int firstDashIndex = entryName.indexOf('-');
            itemList.add(entryName.substring(firstDashIndex + 1));
        }

        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, itemList));
    }
}
