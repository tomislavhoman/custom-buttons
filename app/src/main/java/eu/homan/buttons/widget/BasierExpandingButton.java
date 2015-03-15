package eu.homan.buttons.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.lang.ref.WeakReference;

public class BasierExpandingButton extends View {

    private static final int PAINT_COLOR = 0xFF33B5E5;
    private static final int PAINT_STROKE = 4;
    private static final int OUTER_LIMIT = PAINT_STROKE + 20;

    private final Paint paint = new Paint();
    private final Animator animator = new Animator(this);

    private DynamicPoint[] border;

    public BasierExpandingButton(Context context) {
        super(context);
        initView();
    }

    public BasierExpandingButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public BasierExpandingButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        initPaint();
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        expand();
                        return true;
                    case MotionEvent.ACTION_UP:
                        shrink();
                        break;
                }
                return false;
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        border = calculateInnerPoints();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.drawPath(calculateBorderPath(), paint);
        canvas.restore();
    }

    private void expand() {
        border = calculateInnerToOuterPoints();
        animateView();
    }

    private void shrink() {
        border = calculateOuterToInnerPoints();
        animateView();
    }

    private void animateView() {
        removeCallbacks(animator);
        post(animator);
    }

    private void initPaint() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(PAINT_COLOR);
        paint.setStrokeWidth(PAINT_STROKE);
        paint.setAntiAlias(true);
    }

    private Path calculateBorderPath() {
        return calculateBorderPath(border);
    }

    private Path calculateBorderPath(DynamicPoint[] points) {
        Path path = new Path();

        path.moveTo(points[0].getX(), points[0].getY());
        int len = points.length;
        for (int i = 0; i < len; i++) {
            path.lineTo(points[i].getX(), points[i].getY());
        }

        return path;
    }

    private StaticPoint[] calculateInnerPoints() {
        int pl = getPaddingLeft();
        int pr = getPaddingRight();
        int pt = getPaddingTop();
        int pb = getPaddingBottom();
        int innerWidth = getWidth() - pl - pr;
        int innerHeight = getHeight() - pt - pb;

        return new StaticPoint[]{
                new StaticPoint(pl, pt),
                new StaticPoint(pl + innerWidth, pt),
                new StaticPoint(pl + innerWidth, pt + innerHeight),
                new StaticPoint(pl, pt + innerHeight),
                new StaticPoint(pl, pt)
        };
    }

    private DynamicPoint[] calculateOuterToInnerPoints() {
        int pl = getPaddingLeft();
        int pr = getPaddingRight();
        int pt = getPaddingTop();
        int pb = getPaddingBottom();
        int width = getWidth();
        int height = getHeight();
        int innerWidth = width - pl - pr;
        int innerHeight = height - pt - pb;

        return new DynamicPoint[]{
                new DynamicPoint(border[0].getX(), border[0].getY(), pl, pt),
                new DynamicPoint(border[1].getX(), border[1].getY(), pl + innerWidth, pt),
                new DynamicPoint(border[2].getX(), border[2].getY(), pl + innerWidth, pt + innerHeight),
                new DynamicPoint(border[3].getX(), border[3].getY(), pl, pt + innerHeight),
                new DynamicPoint(border[4].getX(), border[4].getY(), pl, pt)
        };
    }

    private DynamicPoint[] calculateInnerToOuterPoints() {
        int width = getWidth();
        int height = getHeight();

        return new DynamicPoint[]{
                new DynamicPoint(border[0].getX(), border[0].getY(), OUTER_LIMIT, OUTER_LIMIT),
                new DynamicPoint(border[1].getX(), border[1].getY(), width - OUTER_LIMIT, OUTER_LIMIT),
                new DynamicPoint(border[2].getX(), border[2].getY(), width - OUTER_LIMIT, height - OUTER_LIMIT),
                new DynamicPoint(border[3].getX(), border[3].getY(), OUTER_LIMIT, height - OUTER_LIMIT),
                new DynamicPoint(border[4].getX(), border[4].getY(), OUTER_LIMIT, OUTER_LIMIT)
        };
    }

    private static class DynamicPoint {

        private static final float TOLERANCE = 0.1f;
        private static final int MINIMAL_DT = 50;
        private static final float K = 0.5f;//Spring constant
        private static final float VISCOSITY = 0.9f;

        //Speed
        private float vx;
        private float vy;

        //Positions
        private float x;
        private float y;

        private float targetX;
        private float targetY;


        private long lastTime = System.currentTimeMillis();

        private DynamicPoint(float x, float y, float targetX, float targetY) {
            this.x = x;
            this.y = y;
            this.targetX = targetX;
            this.targetY = targetY;
        }

        public void update() {
            long now = System.currentTimeMillis();
            float dt = Math.min(MINIMAL_DT, now - lastTime);
            lastTime = now;

            //Euler integration
            float ax = K * (targetX - x);//The force is stronger when we are farther of the target
            float ay = K * (targetY - y);

            float dvx = ax * dt;
            float dvy = ay * dt;

            vx += dvx;
            vy += dvy;

            vx *= VISCOSITY;
            vy *= VISCOSITY;

            float dx = vx * dt / 1000;
            float dy = vy * dt / 1000;

            x += dx;
            y += dy;
        }

        public boolean isFinished() {
            return Math.abs(x - targetX) <= TOLERANCE &&
                    Math.abs(y - targetY) <= TOLERANCE;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }
    }

    private static final class StaticPoint extends DynamicPoint {
        private StaticPoint(float x, float y) {
            super(x, y, x, y);
        }
    }

    private static final class Animator implements Runnable {

        private static final int REFRESH_TIME = 15;//ms

        private final WeakReference<BasierExpandingButton> expandingButtonWeakReference;

        private Animator(BasierExpandingButton constantExpandingButton) {
            this.expandingButtonWeakReference = new WeakReference<>(constantExpandingButton);
        }

        @Override
        public void run() {
            BasierExpandingButton parent = expandingButtonWeakReference.get();
            if (parent == null) {
                return;
            }

            int len = parent.border.length;
            boolean finished = true;
            for (int i = 0; i < len; i++) {
                parent.border[i].update();
                finished = finished && parent.border[i].isFinished();
            }
            parent.invalidate();

            if (!finished) {
                parent.postDelayed(this, REFRESH_TIME);
            }
        }
    }
}
