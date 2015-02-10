package edu.cmu.colornamedemo;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;

public class FullscreenActivity extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;
    private HashMap<String, Integer> colors;
    private DrawOnTop mDrawOnTop;
    private final static String TAG = "FullScreenActivity";
    private static final String filePath = "colors.json";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Hide the window title and set full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
        getColors();

        mDrawOnTop = new DrawOnTop(this);
        mCamera = getCameraInstance();
        mPreview = new CameraPreview(this, mCamera, colors, mDrawOnTop);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        addContentView(mDrawOnTop, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
            Log.i(TAG, "Camera is available");
        }
        catch (Exception e){
            Log.e(TAG, "Camera is not available");
        }
        return c;
    }

    private void getColors() {
        InputStream is;
        try {
            is = getAssets().open(filePath);
            int sizeOfJSONFile = is.available();
            byte[] bytes = new byte[sizeOfJSONFile];
            is.read(bytes);
            is.close();
            String jsonStr = new String(bytes, "UTF-8");
            JSONObject jsonObj = new JSONObject(jsonStr);

            colors = new HashMap<>();

            Iterator<String> keys = jsonObj.keys();
            while (keys.hasNext()) {
                String name = String.valueOf(keys.next());
                String value = jsonObj.getString(name);
                int rgb = Integer.valueOf(value, 16);
                colors.put(name, Color.rgb(Color.red(rgb), Color.green(rgb), Color.blue(rgb)));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
