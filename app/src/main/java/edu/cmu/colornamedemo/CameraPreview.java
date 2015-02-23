package edu.cmu.colornamedemo;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Activity mActivity;
    private Context mContext;
    private DrawOnTop mDrawOnTop;
    private int frameHeight, frameWidth;
    private int[] rgb;
    private Map<String, Integer> mColors;
    private Toast previousToast;
    private Orientation currentOrientation;

    private final static String TAG = "CameraPreview";
    private final static int RADIUS = 20;
    private final static int TOLERANCE = 50;
    private final static int TOAST_TEXT_SIZE = 25;
    private final static int BRIGHTNESS_AMOUNT = 60;

    private enum Orientation {
        ROTATION_0, ROTATION_90, ROTATION_180, ROTATION_270
    }

    public CameraPreview(Activity activity, Camera camera,
                         Map<String, Integer> colors, DrawOnTop drawOnTop) {
        super(activity);
        mCamera = camera;
        mColors = colors;
        mActivity = activity;
        mContext = getContext();
        mDrawOnTop = drawOnTop;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            // Preview callback used whenever new viewfinder frame is available
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                public void onPreviewFrame(byte[] data, Camera camera) {
                    frameHeight = camera.getParameters().getPreviewSize().height;
                    frameWidth = camera.getParameters().getPreviewSize().width;
                    rgb = new int[frameWidth * frameHeight];
                    decodeYUV420RGB(rgb, data, frameWidth, frameHeight);
                }
            });
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        adjustRotation();

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.i(TAG, "onPreviewFrame called");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int x = Math.round(event.getX());
            int y = Math.round(event.getY());
            Log.i(TAG, "Touch coordinates : " +
                    String.valueOf(x) + "x" + String.valueOf(y));
            int color = getColor(x, y);
            Log.i(TAG, "Pixel color: " + getBestMatchingColorName(color));

            if (mDrawOnTop != null) {
                mDrawOnTop.setValues(x, y, RADIUS);
                mDrawOnTop.invalidate();
            }

            String bestColor = getMostDominantColour(x, y);

            showToast(bestColor);
        }
        return true;
    }

    private void showToast(String name) {
        CharSequence text = name;
        if (previousToast != null) {
            previousToast.cancel();
        }

        LinearLayout layout = new LinearLayout(mContext);
        layout.setBackgroundResource(R.color.trans);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                             LinearLayout.LayoutParams.MATCH_PARENT));
        TextView tv = new TextView(mContext);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TOAST_TEXT_SIZE);
        tv.setGravity(Gravity.TOP);
        tv.setText(text);

        layout.addView(tv);

        Toast toast = new Toast(mContext);
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);

        toast.show();
        previousToast = toast;
    }

    private int getColor(int x, int y) {
        switch (currentOrientation) {
            case ROTATION_0:
                return rgb[frameWidth * (frameHeight - 1 - x) + y];
            case ROTATION_90:
                return rgb[frameWidth * y + x];
            case ROTATION_180:
                return rgb[frameWidth * x + (frameWidth - 1 - y)];
            case ROTATION_270:
                return rgb[frameWidth * (frameHeight - 1 - y) + (frameWidth - 1 - x)];
            default:
                return rgb[frameWidth * y + x];
        }
    }

    private void adjustRotation() {
        int angle;
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                angle = 90;
                currentOrientation = Orientation.ROTATION_0;
                Log.d(TAG, "ROTATION_0");
                break;
            case Surface.ROTATION_90:
                angle = 0;
                currentOrientation = Orientation.ROTATION_90;
                Log.d(TAG, "ROTATION_90");
                break;
            case Surface.ROTATION_180:
                angle = 270;
                currentOrientation = Orientation.ROTATION_180;
                Log.d(TAG, "ROTATION_180");
                break;
            case Surface.ROTATION_270:
                currentOrientation = Orientation.ROTATION_270;
                Log.d(TAG, "ROTATION_270");
                angle = 180;
                break;
            default:
                angle = 90;
                currentOrientation = Orientation.ROTATION_0;
                Log.d(TAG, "ROTATION DEFAULT");
                break;
        }
        mCamera.setDisplayOrientation(angle);
    }

    private String getMostDominantColour(int x, int y) {
        Map colorsMap = new HashMap();
        Map graysMap = new HashMap();

        int minX = Math.max(0, x - RADIUS / 2);
        int maxX = isPortrait() ? Math.min(x + RADIUS / 2, frameHeight) : Math.min(x + RADIUS / 2, frameWidth);
        int minY = Math.max(0, y - RADIUS / 2);
        int maxY = isPortrait() ? Math.min(y + RADIUS / 2, frameWidth) : Math.min(y + RADIUS / 2, frameHeight);

        Log.i(TAG, "Circle region: (" + Integer.toString(minX) + "," + Integer.toString(minY) + ") - ("
                + Integer.toString(maxX) + "," + Integer.toString(maxY) + ")");

        for (int i = minX; i < maxX; i++) {
            for (int j = minY; j < maxY; j++) {
                if ((x - i) * (x - i) + (y - j) * (y - j) <= RADIUS * RADIUS) {
                    // Inside circle region
                    int color = getColor(i, j);
                    // Make the color brighter
                    int brighterColor = makeBrighter(color, BRIGHTNESS_AMOUNT);
                    if (!isGray(brighterColor)) {
                        // Black, white, and gray are not colors
                        Integer counter = (Integer) colorsMap.get(brighterColor);
                        if (counter == null) {
                            counter = 0;
                        }
                        counter++;
                        colorsMap.put(brighterColor, counter);
                    } else {
                        Integer counter = (Integer) graysMap.get(color);
                        if (counter == null) {
                            counter = 0;
                        }
                        counter++;
                        graysMap.put(color, counter);
                    }

                }
            }
        }

        int color;

        if (colorsMap.size() == 0) {
            color = getMostFrequentColor(graysMap);
        } else {
            color = getMostFrequentColor(colorsMap);
        }

        return getBestMatchingColorName(color);
    }

    private int makeBrighter(int color, int amount) {
        int red = Math.min(255, Color.red(color) + amount);
        int green = Math.min(255, Color.green(color) + amount);
        int blue = Math.min(255, Color.blue(color) + amount);
        return Color.argb(0, red, green, blue);
    }

    private int getMostFrequentColor(Map m) {
        List list = new LinkedList(m.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue())
                        .compareTo(((Map.Entry) (o2)).getValue());
            }
        });

        Map.Entry me = (Map.Entry) list.get(list.size() - 1);
        return (Integer) me.getKey();
    }

    private boolean isGray(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        int rgDiff = red - green;
        int rbDiff = red - blue;

        if (Math.abs(rgDiff) > TOLERANCE && Math.abs(rbDiff) > TOLERANCE) {
            return false;
        }
        return true;
    }

    private boolean isPortrait() {
        return mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    private String getBestMatchingColorName(int pixelColor) {
        // largest difference is 255 for every colour component
        int currentDifference = 3 * 255;
        // name of the best matching colour
        String closestColorName = null;
        // get int values for all three colour components of the pixel
        int pixelColorR = Color.red(pixelColor);
        int pixelColorG = Color.green(pixelColor);
        int pixelColorB = Color.blue(pixelColor);

        Iterator<String> colorNameIterator = mColors.keySet().iterator();
        // continue iterating if the map contains a next colour and the difference is greater than zero.
        // a difference of zero means we've found an exact match, so there's no point in iterating further.
        while (colorNameIterator.hasNext() && currentDifference > 0) {
            // this colour's name
            String currentColorName = colorNameIterator.next();
            // this colour's int value
            int color = mColors.get(currentColorName);
            // get int values for all three colour components of this colour
            int colorR = Color.red(color);
            int colorG = Color.green(color);
            int colorB = Color.blue(color);
            // calculate sum of absolute differences that indicates how good this match is
            int difference = Math.abs(pixelColorR - colorR) + Math.abs(pixelColorG - colorG) + Math.abs(pixelColorB - colorB);
            // a smaller difference means a better match, so keep track of it
            if (currentDifference > difference) {
                currentDifference = difference;
                closestColorName = currentColorName;
            }
        }
        return closestColorName;
    }

    // Convert YUV to RGB
    private void decodeYUV420RGB(int[] rgb, byte[] yuv420sp, int width, int height) {

        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0;
                else if (r > 262143) r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);

            }
        }
    }
}
