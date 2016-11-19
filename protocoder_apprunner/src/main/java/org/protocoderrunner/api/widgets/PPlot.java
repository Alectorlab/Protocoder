package org.protocoderrunner.api.widgets;

import android.graphics.Color;
import android.os.Handler;
import android.os.SystemClock;

import org.protocoderrunner.apprunner.AppRunner;
import org.protocoderrunner.apprunner.StyleProperties;
import org.protocoderrunner.base.utils.AndroidUtils;
import org.protocoderrunner.base.utils.MLog;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by biquillo on 11/09/16.
 */
public class PPlot extends PCanvas implements PViewMethodsInterface {

    private static final String TAG = PPlot.class.getSimpleName();
    private final Handler handler;
    private final Runnable r;

    StyleProperties props = new StyleProperties();
    private Styler styler;

    private ArrayList<PlotPoint> arrayData = new ArrayList<>();
    public ArrayList<PlotPoint> arrayViz = new ArrayList<>();

    private float yMax = Float.MIN_VALUE;
    private float yMin = Float.MAX_VALUE;
    private float xMax = Float.MIN_VALUE;
    private float xMin = Float.MAX_VALUE;

    String name = "";

    public PPlot(AppRunner appRunner) {
        super(appRunner);

        draw = mydraw;
        styler = new Styler(appRunner, this, props);
        styler.apply();

        mAppRunner.whatIsRunning.add(this);

        MLog.d(TAG, "starting runnable");

        handler = new Handler();
        r = new Runnable() {
            @Override
            public void run() {
                // exit if no data
                if (arrayData.size() > 0) {
                    MLog.d(TAG, "runnable " + arrayData.size());
                    arrayViz.clear();

                    // if auto scale
                    float xfrom = arrayData.get(0).x;
                    float xto = xMax;
                    float yfrom = yMax;
                    float yto = yMin;

                    for (int i = 0; i < arrayData.size(); i++) {
                        PlotPoint p = arrayData.get(i);

                        float x = mAppRunner.pUtil.map(p.x, xfrom, xto, 0 + 10, width - 10);
                        float y = mAppRunner.pUtil.map(p.y, yfrom, yto, 0 + 20, height - 20);
                        arrayViz.add(new PlotPoint(x, y));

                        // MLog.d(TAG, width + " " + height);
                        // MLog.d(TAG, "arrayData: " + i + " " + p.x + " " + p.y + " -> " + x + ", " + y);
                    }
                }

                handler.postDelayed(r, 20);
                postInvalidate();
            }
        };

        handler.post(r);
    }

    // plot color
    // plot thickness
    // plot background
    // rangeX [x1, x2]
    // rangeY [y1, y2]

    OnDrawCallback mydraw = new OnDrawCallback() {
        @Override
        public void event(PCanvas c) {
            MLog.d(TAG, "paint ");

            c.clear();
            c.mode(false);

            /*
            Shader shader = new LinearGradient(0, 0, 0, c.height, new int[] {Color.parseColor("#00000000"), Color.parseColor("#000000") }, null, Shader.TileMode.MIRROR);
            Matrix matrix = new Matrix();
            matrix.setRotate(90);
            shader.setLocalMatrix(matrix);
            c.mPaintFill.setShader(shader);
            */

            c.stroke("#55000000"); // styler.plotColor);
            c.strokeWidth(1);           // center line
            c.line(0, c.height / 2, c.width, c.height / 2);

            if (!arrayViz.isEmpty()) {
                PlotPoint p;

                c.strokeWidth(AndroidUtils.dpToPixels(mAppRunner.getAppContext(), 2)); // styler.plotWidth);
                c.noFill();
                c.stroke("#FF000000"); // styler.plotColor);

                c.beginPath();
                p = arrayViz.get(0);
                c.pointPath(p.x, p.y);

                for (int i = 0; i < arrayViz.size(); i++) {
                    p = arrayViz.get(i);
                    c.pointPath(p.x, p.y);
                }

                p = arrayViz.get(arrayViz.size() - 1);
                c.pointPath(p.x, p.y);
                c.closePath();

                if (false) {
                    c.fill(Color.BLUE);
                    c.noStroke();
                    for (int i = 0; i < arrayViz.size(); i++) {
                        p = arrayViz.get(i);
                        c.text("" + p.y, p.x, p.y);
                    }
                }

            }

            // text
            c.textSize(AndroidUtils.spToPixels(mAppRunner.getAppContext(), 12));
            c.fill("#55000000");
            c.noStroke();
            c.textAlign("right");

            c.text(name, width - 20, 50);
        }
    };

    public PPlot update(float y) {
        this.update(now(), y);

        return this;
    }

    public PPlot update(float x, float y) {

        if (y < yMin) yMin = y;
        else if (y > yMax) yMax = y;

        if (x < xMin) xMin = x;
        else if (x > xMax) xMax = x;

        // MLog.d(TAG, "adding " + x + " " + y);

        if (arrayData.size() > 100) {
            arrayData.remove(0);
        }

        arrayData.add(new PlotPoint(x, y));

        return this;
    }

    /*
    public void map(float[] xAxis, ReturnInterfaceWithReturn callback) {
        ReturnObject r = new ReturnObject();

        float[][] xyAxis = new float[xAxis.length][2];
        for (int i = 0; i < xAxis.length; i++) {
            xyAxis[i][0] = xAxis[i];
        }

        r.put("xyAxis", xyAxis);
        array2d = (float[][]) callback.event(r);
        invalidate();
    }
    */

    public void array2d(float[][] val) {
        arrayViz.clear();

        for (int i = 0; i < val.length; i++) {
           arrayData.add(new PlotPoint(val[i][0], val[i][1]));
        }

        invalidate();
    }

    public PPlot name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public void set(float x, float y, float w, float h) {
        styler.setLayoutProps(x, y, w, h);
    }

    @Override
    public void setStyle(Map style) {
        styler.setStyle(style);
    }

    @Override
    public Map getStyle() {
        return props;
    }

    public void __stop() {
        handler.removeCallbacks(r);
    }

    private long now() {
        return SystemClock.uptimeMillis();
    }

    class PlotPoint {
        float x;
        float y;

        public PlotPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
