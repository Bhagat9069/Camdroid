package org.hschott.camdroid.processor;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import org.hschott.camdroid.OnCameraPreviewListener.FrameDrawer;
import org.hschott.camdroid.R;
import org.hschott.camdroid.UIFragment;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.Iterator;

public class MovementDetectionProcessor extends AbstractOpenCVFrameProcessor {

    public static class BackgroundSubstractionUIFragment extends
            ConfigurationFragment implements UIFragment {

        public static BackgroundSubstractionUIFragment newInstance() {
            BackgroundSubstractionUIFragment f = new BackgroundSubstractionUIFragment();
            return f;
        }

        private SeekBar objectMaxSizeSeekBar;

        private SeekBar objectMinSizeSeekBar;

        @Override
        public int getLayoutId() {
            return R.layout.movementdetection_ui;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = super
                    .onCreateView(inflater, container, savedInstanceState);

            SeekBar learningRateSeekBar = (SeekBar) v
                    .findViewById(R.id.learning_rate);
            learningRateSeekBar.setMax(20);
            learningRateSeekBar.setProgress(learning_rate);

            learningRateSeekBar
                    .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            if (fromUser) {
                                learning_rate = progress;
                                BackgroundSubstractionUIFragment.this
                                        .showValue((double) (-10 + learning_rate) / 10);
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

            this.objectMinSizeSeekBar = (SeekBar) v
                    .findViewById(R.id.object_min_size);
            this.objectMinSizeSeekBar.setMax(100);
            this.objectMinSizeSeekBar.setProgress(object_min_size);

            this.objectMinSizeSeekBar
                    .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            object_min_size = progress == 0 ? 1 : progress;
                            if (fromUser) {
                                if (object_min_size > object_max_size) {
                                    BackgroundSubstractionUIFragment.this.objectMaxSizeSeekBar
                                            .setProgress(progress);
                                }
                                BackgroundSubstractionUIFragment.this
                                        .showValue(object_min_size
                                                + "% of height");
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

            this.objectMaxSizeSeekBar = (SeekBar) v
                    .findViewById(R.id.object_max_size);
            this.objectMaxSizeSeekBar.setMax(100);
            this.objectMaxSizeSeekBar.setProgress(object_max_size);

            this.objectMaxSizeSeekBar
                    .setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            object_max_size = progress == 0 ? 1 : progress;
                            if (fromUser) {
                                if (object_max_size < object_min_size) {
                                    BackgroundSubstractionUIFragment.this.objectMinSizeSeekBar
                                            .setProgress(progress);
                                }
                                BackgroundSubstractionUIFragment.this
                                        .showValue(object_max_size
                                                + "% of height");
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });

            return v;
        }
    }

    private static int object_min_size = 25;
    private static int object_max_size = 50;
    private static int learning_rate = 10;
    private BackgroundSubtractorMOG2 mog = Video.createBackgroundSubtractorMOG2(50, 0, true);

    public MovementDetectionProcessor(FrameDrawer drawer) {
        super(drawer);
    }

    @Override
    public Fragment getConfigUiFragment() {
        return BackgroundSubstractionUIFragment.newInstance();
    }

    @Override
    public FrameWorker createFrameWorker() {
        return new MovementDetectionFrameWorker(drawer);
    }

    public class MovementDetectionFrameWorker extends AbstractOpenCVFrameWorker {

        private Mat mask;

        public MovementDetectionFrameWorker(FrameDrawer drawer) {
            super(drawer);
        }

        @Override
        public void allocate(int width, int height) {
            super.allocate(width, height);
            this.mask = new Mat();
        }

        @Override
        public void release() {
            super.release();
            this.mask.release();
        }

        protected void execute() {
            out = gray();

            synchronized (mog) {
                mog.apply(out, this.mask, (double) (-10 + learning_rate) / 1000);
            }

            Imgproc.dilate(this.mask, this.mask, new Mat());

            ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Imgproc.findContours(this.mask, contours, new Mat(),
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            double maxheight = object_max_size * this.in.height() / 100;
            double minheight = object_min_size * this.in.height() / 100;

            Iterator<MatOfPoint> each = contours.iterator();
            each = contours.iterator();
            while (each.hasNext()) {
                MatOfPoint contour = each.next();
                Rect rect = Imgproc.boundingRect(contour);
                if (rect.height > minheight && rect.height < maxheight) {
                    Imgproc.rectangle(out, rect.tl(), rect.br(), new Scalar(255,
                            0, 0), 1);
                }
            }
        }

    }

}