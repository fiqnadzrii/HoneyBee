package com.example.honeybee;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GameView extends View {
    private Paint progressPaint, flowerPaint, groundPaint;
    private Bitmap beeBitmap, cloudBitmap, treeBitmap;
    private float beeVelocity;
    private float beeX, beeY;
    private float gravity = 0.5f;
    private float jumpForce = -15f;
    private boolean isPlaying = false;

    private ArrayList<Obstacle> obstacles;
    private Random random;
    private int score = 0;

    private static final float BEE_SIZE = 120f;
    private static final float OBSTACLE_SPEED = 8f;
    private static final int WIN_SCORE = 10;

    private PointF flowerPosition;
    private float flowerSize = 150f;
    private boolean flowerReached = false;
    private float flowerTargetX = -1;
    private float flowerSpeed = 15f;
    private float groundHeight = 100f;

    public GameView(Context context) {
        super(context);
        init(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        progressPaint = new Paint();
        progressPaint.setColor(Color.WHITE);
        progressPaint.setTextSize(50);
        progressPaint.setTextAlign(Paint.Align.LEFT);

        flowerPaint = new Paint();
        flowerPaint.setColor(Color.rgb(255, 105, 180));
        flowerPaint.setStyle(Paint.Style.FILL);

        groundPaint = new Paint();
        groundPaint.setColor(Color.rgb(139, 69, 19));

        beeBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.bee);
        beeBitmap = Bitmap.createScaledBitmap(beeBitmap, (int) BEE_SIZE, (int) BEE_SIZE, true);

        // Load cloud and tree as raw (unscaled)
        cloudBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.cloud);
        treeBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.tree);

        obstacles = new ArrayList<>();
        random = new Random();
        flowerPosition = new PointF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // obstacles size
        int cloudWidth = w / 6;
        int cloudHeight = h / 12;
        cloudBitmap = Bitmap.createScaledBitmap(cloudBitmap, cloudWidth, cloudHeight, true);

        int treeWidth = w / 10;
        int treeHeight = h / 6;
        treeBitmap = Bitmap.createScaledBitmap(treeBitmap, treeWidth, treeHeight, true);

        resetGame(); // Also reset bee and obstacles after resizing
    }

    public void startGame() {
        isPlaying = true;
        score = 0;
        resetGame();
        invalidate();
    }

    public void resetGame() {
        beeX = getWidth() / 4f;
        beeY = getHeight() / 2f;
        beeVelocity = 0;
        obstacles.clear();
        score = 0;
        flowerReached = false;
        addObstacle();
    }

    private void addObstacle() {
        float x = getWidth();
        int numberOfClouds = 2 + random.nextInt(2); // 2 or 3 clouds

        // Choose a cloud pattern group (low, mid, high)
        float[] levels = {
                getHeight() * 0.15f, // low
                getHeight() * 0.35f, // mid
                getHeight() * 0.55f  // high
        };
        // Shuffle the levels so clouds don't always appear in the same order
        ArrayList<Float> levelList = new ArrayList<>();
        for (float level : levels) levelList.add(level);
        java.util.Collections.shuffle(levelList);

        for (int i = 0; i < numberOfClouds; i++) {
            float cloudY = levelList.get(i % levelList.size()) + random.nextInt(30); // add slight jitter
            float cloudX = x + i * (cloudBitmap.getWidth() + 40); // space out clouds
            obstacles.add(new Obstacle(cloudX, cloudY, true));
        }

        // Add tree with varying height
        float treeScale = 0.7f + random.nextFloat() * 0.8f;
        float treeY = getHeight() - groundHeight - treeBitmap.getHeight() * treeScale;
        obstacles.add(new Obstacle(x, treeY, false, treeScale));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(Color.rgb(135, 206, 235)); // Sky blue

        // Draw ground
        canvas.drawRect(0, getHeight() - groundHeight, getWidth(), getHeight(), groundPaint);

        // Draw obstacles
        for (Obstacle obs : obstacles) {
            if (obs.isCloud) {
                canvas.drawBitmap(cloudBitmap, obs.x, obs.y, null);
            } else {
                // Draw tree with scaling
                Rect src = new Rect(0, 0, treeBitmap.getWidth(), treeBitmap.getHeight());
                @SuppressLint("DrawAllocation") RectF dst = new RectF(
                        obs.x,
                        obs.y,
                        obs.x + treeBitmap.getWidth() * obs.scaleFactor,      // scale width
                        obs.y + treeBitmap.getHeight() * obs.scaleFactor       // scale height
                );
                canvas.drawBitmap(treeBitmap, src, dst, null);
            }
        }

        // draw bee
        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1); // Flip horizontally
        matrix.postTranslate(beeX + BEE_SIZE / 2, beeY - BEE_SIZE / 2); // Adjust position after flip
        canvas.drawBitmap(beeBitmap, matrix, null);

        // Flower
        if (score >= WIN_SCORE) {
            updateFlowerPosition();
            drawFlower(canvas);
        }

        // Progress text
        String text = "Progress to Flower: " + Math.min(score, WIN_SCORE) + "/" + WIN_SCORE;
        float textX = 20;
        float textY = 120;
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);
        bgPaint.setAlpha(128);
        float padding = 20;
        float width = progressPaint.measureText(text);
        canvas.drawRect(textX - padding, textY - 50, textX + width + padding, textY + 10, bgPaint);
        canvas.drawText(text, textX, textY, progressPaint);

        if (isPlaying) {
            update();
            invalidate();
        }
    }

    private void update() {
        beeVelocity += gravity;
        beeY += beeVelocity;

        if (score >= WIN_SCORE && !flowerReached) {
            float dx = beeX - flowerPosition.x;
            float dy = beeY - flowerPosition.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            float hitRadius = (flowerSize / 2) + (BEE_SIZE / 2);
            if (distance < hitRadius) {
                flowerReached = true;
                gameWon();
                return;
            }
        }

        Iterator<Obstacle> iterator = obstacles.iterator();
        boolean needNew = true;

        while (iterator.hasNext()) {
            Obstacle obs = iterator.next();
            obs.x -= OBSTACLE_SPEED;

            if (obs.x + obs.getWidth() < 0) {
                iterator.remove();
                if (obs.isCloud && score < WIN_SCORE) score++;
            }

            if (obs.x + obs.getWidth() > 0) {
                needNew = false;
            }
        }

        if (needNew && score < WIN_SCORE) {
            addObstacle();
        }

        // Collision
        RectF beeRect = new RectF(
                beeX - BEE_SIZE / 3,
                beeY - BEE_SIZE / 3,
                beeX + BEE_SIZE / 3,
                beeY + BEE_SIZE / 3
        );

        for (Obstacle obs : obstacles) {
            RectF rect = obs.getRect();
            if (RectF.intersects(beeRect, rect)) {
                gameOver();
                return;
            }
        }

        // Ground collision
        if (beeY + BEE_SIZE / 2 > getHeight() - groundHeight) {
            beeY = getHeight() - groundHeight - BEE_SIZE / 2;
            gameOver();
        }

        if (beeY < BEE_SIZE / 2) {
            beeY = BEE_SIZE / 2;
            beeVelocity = 0;
        }
    }

    private void drawFlower(Canvas canvas) {
        flowerPosition.y = getHeight() / 2;
        for (int i = 0; i < 8; i++) {
            float angle = (float) (i * Math.PI / 4);
            float px = flowerPosition.x + (float) Math.cos(angle) * (flowerSize / 3);
            float py = flowerPosition.y + (float) Math.sin(angle) * (flowerSize / 3);
            canvas.drawCircle(px, py, flowerSize / 4, flowerPaint);
        }

        Paint centerPaint = new Paint();
        centerPaint.setColor(Color.YELLOW);
        canvas.drawCircle(flowerPosition.x, flowerPosition.y, flowerSize / 4, centerPaint);
    }

    private void updateFlowerPosition() {
        if (flowerTargetX < 0) {
            flowerTargetX = getWidth() / 3f;
            flowerPosition.x = getWidth() - 150;
        }

        if (flowerPosition.x > flowerTargetX) {
            flowerPosition.x -= flowerSpeed;
            if (flowerPosition.x < flowerTargetX) {
                flowerPosition.x = flowerTargetX;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            beeVelocity = jumpForce;
            // Play interaction sound on tap
            if (getContext() instanceof Level1Activity) {
                ((Level1Activity) getContext()).playInteractSound();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void gameOver() {
        isPlaying = false;
        if (getContext() instanceof Level1Activity) {
            ((Level1Activity) getContext()).onGameOver(score);
        }
    }

    private void gameWon() {
        isPlaying = false;
        if (getContext() instanceof Level1Activity) {
            ((Level1Activity) getContext()).onGameWon(score);
        }
    }

    public int getScore() {
        return score;
    }

    public boolean isGameActive() {
        return isPlaying;
    }

    private class Obstacle {
        float x, y;
        boolean isCloud;
        float scaleFactor = 1f;

        Obstacle(float x, float y, boolean isCloud) {
            this.x = x;
            this.y = y;
            this.isCloud = isCloud;
        }

        Obstacle(float x, float y, boolean isCloud, float scaleFactor) {
            this(x, y, isCloud);
            this.scaleFactor = scaleFactor;
        }

        int getWidth() {
            Bitmap bmp = isCloud ? cloudBitmap : treeBitmap;
            return (int) (bmp.getWidth());
        }

        int getHeight() {
            Bitmap bmp = isCloud ? cloudBitmap : treeBitmap;
            return (int) (bmp.getHeight() * scaleFactor);
        }

        RectF getRect() {
            return new RectF(x, y, x + getWidth(), y + getHeight());
        }
    }
}
