package com.example.honeybee;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class Level2GameView extends View {
    private static final int PAIR_COUNT = 4;
    private static final float NECTAR_RADIUS = 140f;
    private static final float INVERTASE_RADIUS = 120f;
    private static final float AMYLASE_RADIUS = 120f;
    private static final float SUGAR_RADIUS = 90f;
    private ArrayList<PointF> nectarPositions;
    private ArrayList<PointF> nectarVelocities;
    private ArrayList<PointF> invertasePositions;
    private ArrayList<PointF> invertaseVelocities;
    private ArrayList<PointF> amylasePositions;
    private ArrayList<PointF> amylaseVelocities;
    private ArrayList<PointF> sugarPositions;
    private ArrayList<PointF> sugarVelocities;
    private ArrayList<Integer> sugarTypes; // 0 = glucose, 1 = fructose
    private Paint nectarPaint, invertasePaint, amylasePaint, invertaseTextPaint, amylaseTextPaint, textPaint, sugarTextPaint, glucosePaint, fructosePaint;
    private Random random;
    private boolean gameComplete = false;
    private boolean wrongEnzyme = false;
    private int draggingIndex = -1;
    private boolean isDraggingAmylase = false;
    private float dragOffsetX, dragOffsetY;
    private Level2Activity activity;

    public Level2GameView(Context context) {
        super(context);
        init();
    }
    
    public Level2GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setActivity(Level2Activity activity) {
        this.activity = activity;
    }

    private void init() {
        nectarPositions = new ArrayList<>();
        nectarVelocities = new ArrayList<>();
        invertasePositions = new ArrayList<>();
        invertaseVelocities = new ArrayList<>();
        amylasePositions = new ArrayList<>();
        amylaseVelocities = new ArrayList<>();
        sugarPositions = new ArrayList<>();
        sugarVelocities = new ArrayList<>();
        sugarTypes = new ArrayList<>();
        random = new Random();
        
        nectarPaint = new Paint();
        nectarPaint.setColor(Color.YELLOW);
        nectarPaint.setStyle(Paint.Style.FILL);
        
        invertasePaint = new Paint();
        invertasePaint.setColor(Color.BLUE);
        invertasePaint.setStyle(Paint.Style.FILL);
        
        amylasePaint = new Paint();
        amylasePaint.setColor(Color.RED);
        amylasePaint.setStyle(Paint.Style.FILL);
        
        invertaseTextPaint = new Paint();
        invertaseTextPaint.setColor(Color.WHITE);
        invertaseTextPaint.setTextSize(50f);
        invertaseTextPaint.setTextAlign(Paint.Align.CENTER);
        
        amylaseTextPaint = new Paint();
        amylaseTextPaint.setColor(Color.WHITE);
        amylaseTextPaint.setTextSize(50f);
        amylaseTextPaint.setTextAlign(Paint.Align.CENTER);
        
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(50f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        sugarTextPaint = new Paint();
        sugarTextPaint.setColor(Color.BLACK);
        sugarTextPaint.setTextSize(32f);
        sugarTextPaint.setTextAlign(Paint.Align.CENTER);
        
        glucosePaint = new Paint();
        glucosePaint.setColor(Color.GREEN);
        glucosePaint.setStyle(Paint.Style.FILL);
        
        fructosePaint = new Paint();
        fructosePaint.setColor(Color.MAGENTA);
        fructosePaint.setStyle(Paint.Style.FILL);
        
        resetGame();
    }

    public void resetGame() {
        nectarPositions.clear();
        nectarVelocities.clear();
        invertasePositions.clear();
        invertaseVelocities.clear();
        amylasePositions.clear();
        amylaseVelocities.clear();
        sugarPositions.clear();
        sugarVelocities.clear();
        sugarTypes.clear();
        wrongEnzyme = false;
        int w = getWidth();
        int h = getHeight();
        
        // Create nectar and invertase pairs
        for (int i = 0; i < PAIR_COUNT; i++) {
            // Add nectar
            float nx = 200 + random.nextInt(Math.max(1, w - 400));
            float ny = 200 + random.nextInt(Math.max(1, h - 400));
            float nvx = 6 + random.nextFloat() * 4;
            float nvy = 6 + random.nextFloat() * 4;
            nectarPositions.add(new PointF(nx, ny));
            nectarVelocities.add(new PointF(nvx * (random.nextBoolean() ? 1 : -1), nvy * (random.nextBoolean() ? 1 : -1)));

            // Add invertase
            float ex = 200 + random.nextInt(Math.max(1, w - 400));
            float ey = 200 + random.nextInt(Math.max(1, h - 400));
            float evx = 6 + random.nextFloat() * 4;
            float evy = 6 + random.nextFloat() * 4;
            invertasePositions.add(new PointF(ex, ey));
            invertaseVelocities.add(new PointF(evx * (random.nextBoolean() ? 1 : -1), evy * (random.nextBoolean() ? 1 : -1)));
        }

        // Add amylase enzymes (2 of them)
        for (int i = 0; i < 2; i++) {
            float ax = 200 + random.nextInt(Math.max(1, w - 400));
            float ay = 200 + random.nextInt(Math.max(1, h - 400));
            float avx = 6 + random.nextFloat() * 4;
            float avy = 6 + random.nextFloat() * 4;
            amylasePositions.add(new PointF(ax, ay));
            amylaseVelocities.add(new PointF(avx * (random.nextBoolean() ? 1 : -1), avy * (random.nextBoolean() ? 1 : -1)));
        }

        draggingIndex = -1;
        gameComplete = false;
        invalidate();
    }

    public boolean isGameComplete() {
        return gameComplete;
    }

    public int getScore() {
        return PAIR_COUNT - nectarPositions.size();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.parseColor("#FFCDD2"));
        
        if (!gameComplete && !wrongEnzyme) moveShapes();
        
        // Draw nectar
        for (PointF pos : nectarPositions) {
            canvas.drawCircle(pos.x, pos.y, NECTAR_RADIUS, nectarPaint);
            canvas.drawText("Nectar", pos.x, pos.y + 20, textPaint);
        }
        
        // Draw invertase
        for (PointF pos : invertasePositions) {
            canvas.drawCircle(pos.x, pos.y, INVERTASE_RADIUS, invertasePaint);
            canvas.drawText("Invertase", pos.x, pos.y + 20, invertaseTextPaint);
        }

        // Draw amylase
        for (PointF pos : amylasePositions) {
            canvas.drawCircle(pos.x, pos.y, AMYLASE_RADIUS, amylasePaint);
            canvas.drawText("Amylase", pos.x, pos.y + 20, amylaseTextPaint);
        }
        
        // Draw sugars
        for (int i = 0; i < sugarPositions.size(); i++) {
            PointF pos = sugarPositions.get(i);
            Paint paint = sugarTypes.get(i) == 0 ? glucosePaint : fructosePaint;
            canvas.drawCircle(pos.x, pos.y, SUGAR_RADIUS, paint);
            canvas.drawText(sugarTypes.get(i) == 0 ? "Glucose" : "Fructose", pos.x, pos.y + 12, sugarTextPaint);
        }

        if (!gameComplete && !wrongEnzyme) invalidate();
    }

    private void moveShapes() {
        int w = getWidth();
        int h = getHeight();
        // Move nectar
        for (int i = 0; i < nectarPositions.size(); i++) {
            PointF pos = nectarPositions.get(i);
            PointF vel = nectarVelocities.get(i);
            pos.x += vel.x;
            pos.y += vel.y;
            if (pos.x < NECTAR_RADIUS || pos.x > w - NECTAR_RADIUS) vel.x *= -1;
            if (pos.y < NECTAR_RADIUS || pos.y > h - NECTAR_RADIUS) vel.y *= -1;
        }
        
        // Move invertase (except the one being dragged)
        for (int i = 0; i < invertasePositions.size(); i++) {
            if (!isDraggingAmylase && i == draggingIndex) continue;
            PointF pos = invertasePositions.get(i);
            PointF vel = invertaseVelocities.get(i);
            pos.x += vel.x;
            pos.y += vel.y;
            if (pos.x < INVERTASE_RADIUS || pos.x > w - INVERTASE_RADIUS) vel.x *= -1;
            if (pos.y < INVERTASE_RADIUS || pos.y > h - INVERTASE_RADIUS) vel.y *= -1;
        }

        // Move amylase (except the one being dragged)
        for (int i = 0; i < amylasePositions.size(); i++) {
            if (isDraggingAmylase && i == draggingIndex) continue;
            PointF pos = amylasePositions.get(i);
            PointF vel = amylaseVelocities.get(i);
            pos.x += vel.x;
            pos.y += vel.y;
            if (pos.x < AMYLASE_RADIUS || pos.x > w - AMYLASE_RADIUS) vel.x *= -1;
            if (pos.y < AMYLASE_RADIUS || pos.y > h - AMYLASE_RADIUS) vel.y *= -1;
        }
        
        // Move sugars
        for (int i = 0; i < sugarPositions.size(); i++) {
            PointF pos = sugarPositions.get(i);
            PointF vel = sugarVelocities.get(i);
            pos.x += vel.x;
            pos.y += vel.y;
            if (pos.x < SUGAR_RADIUS || pos.x > w - SUGAR_RADIUS) vel.x *= -1;
            if (pos.y < SUGAR_RADIUS || pos.y > h - SUGAR_RADIUS) vel.y *= -1;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || gameComplete || wrongEnzyme) return false;
        
        float x = event.getX();
        float y = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Check if touching an invertase
                draggingIndex = getTouchedInvertase(x, y);
                if (draggingIndex != -1) {
                    isDraggingAmylase = false;
                    PointF pos = invertasePositions.get(draggingIndex);
                    dragOffsetX = pos.x - x;
                    dragOffsetY = pos.y - y;
                    return true;
                }
                
                // Check if touching an amylase
                draggingIndex = getTouchedAmylase(x, y);
                if (draggingIndex != -1) {
                    isDraggingAmylase = true;
                    PointF pos = amylasePositions.get(draggingIndex);
                    dragOffsetX = pos.x - x;
                    dragOffsetY = pos.y - y;
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (draggingIndex != -1) {
                    ArrayList<PointF> positions = isDraggingAmylase ? amylasePositions : invertasePositions;
                    PointF pos = positions.get(draggingIndex);
                    pos.x = x + dragOffsetX;
                    pos.y = y + dragOffsetY;
                    checkCollision();
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
                draggingIndex = -1;
                isDraggingAmylase = false;
                break;
        }
        return false;
    }

    private int getTouchedInvertase(float x, float y) {
        for (int i = invertasePositions.size() - 1; i >= 0; i--) {
            PointF pos = invertasePositions.get(i);
            float dx = x - pos.x;
            float dy = y - pos.y;
            if (Math.sqrt(dx*dx + dy*dy) <= INVERTASE_RADIUS) {
                return i;
            }
        }
        return -1;
    }

    private int getTouchedAmylase(float x, float y) {
        for (int i = 0; i < amylasePositions.size(); i++) {
            PointF pos = amylasePositions.get(i);
            float dx = pos.x - x;
            float dy = pos.y - y;
            if (dx * dx + dy * dy < AMYLASE_RADIUS * AMYLASE_RADIUS) {
                return i;
            }
        }
        return -1;
    }

    private void checkCollision() {
        if (draggingIndex == -1) return;
        
        ArrayList<PointF> positions = isDraggingAmylase ? amylasePositions : invertasePositions;
        PointF draggedPos = positions.get(draggingIndex);
        
        for (int i = 0; i < nectarPositions.size(); i++) {
            PointF nectarPos = nectarPositions.get(i);
            float dx = draggedPos.x - nectarPos.x;
            float dy = draggedPos.y - nectarPos.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            
            if (distance < NECTAR_RADIUS + INVERTASE_RADIUS) {
                if (isDraggingAmylase) {
                    // Wrong enzyme used!
                    wrongEnzyme = true;
                    if (activity != null) {
                        activity.onWrongEnzyme();
                    }
                    return;
                }
                
                // Correct enzyme (invertase) - create sugar molecules
                float angle = random.nextFloat() * (float) Math.PI * 2;
                float speed = 4f;
                
                // Create glucose
                PointF glucosePos = new PointF(nectarPos.x, nectarPos.y);
                PointF glucoseVel = new PointF(
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed
                );
                sugarPositions.add(glucosePos);
                sugarVelocities.add(glucoseVel);
                sugarTypes.add(0); // glucose
                
                // Create fructose
                angle += Math.PI; // opposite direction
                PointF fructosePos = new PointF(nectarPos.x, nectarPos.y);
                PointF fructoseVel = new PointF(
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed
                );
                sugarPositions.add(fructosePos);
                sugarVelocities.add(fructoseVel);
                sugarTypes.add(1); // fructose
                
                // Remove the matched pair
                nectarPositions.remove(i);
                nectarVelocities.remove(i);
                positions.remove(draggingIndex);
                if (!isDraggingAmylase) {
                    invertaseVelocities.remove(draggingIndex);
                } else {
                    amylaseVelocities.remove(draggingIndex);
                }
                draggingIndex = -1;
                
                // Check if game is complete
                if (nectarPositions.isEmpty()) {
                    gameComplete = true;
                    if (activity != null) {
                        activity.onGameWon(PAIR_COUNT);
                    }
                }
                break;
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resetGame();
    }
} 