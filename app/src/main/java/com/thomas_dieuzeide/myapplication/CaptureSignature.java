package com.thomas_dieuzeide.myapplication;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class CaptureSignature extends Activity {

    LinearLayout mContent;
    signature mSignature;
    Button mClear, mGetSign, mCancel;
    public static String tempDir;
    private Bitmap mBitmap;
    boolean unavailable = false;
    View mView;
    File mypath;
    String value;
    int session;

    private String uniqueId;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.signature);
        Bundle b = this.getIntent().getExtras();
        value = b.getString("key");
        session = b.getInt("session");

        tempDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + getResources().getString(R.string.external_dir) + "/";
        prepareDirectory();
        uniqueId = getTodaysDate() +":"+ value+":"+session+".png";
        mypath = new File(tempDir,uniqueId);

        mContent = (LinearLayout) findViewById(R.id.linearLayout1);
        mSignature = new signature(this, null);
        mSignature.setBackgroundColor(Color.WHITE);
        mContent.addView(mSignature, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mClear = (Button)findViewById(R.id.clear);
        mGetSign = (Button)findViewById(R.id.getsign);
        mGetSign.setEnabled(false);
        mCancel = (Button)findViewById(R.id.cancel);
        mView = mContent;

        mClear.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                Log.v("log_tag", "Panel Canceled");
                Bundle b = new Bundle();
                b.putString("status", "cancel");
                Intent intent = new Intent();
                intent.putExtras(b);
                setResult(RESULT_OK,intent);
                finish();
            }
        });

        mGetSign.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                mClear.setVisibility(View.GONE);
                mGetSign.setVisibility(View.GONE);
                mCancel.setVisibility(View.GONE);
                Log.v("log_tag", "Panel Saved");
                boolean error = captureSignature();
                if(!error){
                    mView.setDrawingCacheEnabled(true);
                    mSignature.save(mView);
                    Bundle b = new Bundle();
                    b.putString("status", "done");
                    Intent intent = new Intent();
                    intent.putExtras(b);
                    setResult(RESULT_OK, intent);
                    DatabaseHelper db = new DatabaseHelper(MyApplication.getAppContext());
                    java.util.Date dt = new java.util.Date();
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String currentTime = sdf.format(dt);
                    Cursor c = db.getSession(value,session);
                    if(c.moveToNext()) {
                        db.updateSession(value,session,c.getString(2),currentTime);
                    } else {
                        Toast.makeText(CaptureSignature.this,"SESSION INEXISTANTE", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }
            }
        });

        mCancel.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                mClear.setVisibility(View.GONE);
                mGetSign.setVisibility(View.GONE);
                mCancel.setVisibility(View.GONE);
                Log.v("log_tag", "Panel Saved");
                unavailable = true;

                mView.setDrawingCacheEnabled(true);
                mSignature.save(mView);
                Bundle b = new Bundle();
                b.putString("status", "done");
                Intent intent = new Intent();
                intent.putExtras(b);
                setResult(RESULT_OK, intent);
                DatabaseHelper db = new DatabaseHelper(MyApplication.getAppContext());
                java.util.Date dt = new java.util.Date();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String currentTime = sdf.format(dt);
                Cursor c = db.getSession(value,session);
                if(c.moveToNext()) {
                    db.updateSession(value,session,c.getString(2),currentTime);
                } else {
                    Toast.makeText(CaptureSignature.this,"SESSION INEXISTANTE", Toast.LENGTH_SHORT).show();
                }
                finish();

            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.w("GetSignature", "onDestroy");
        super.onDestroy();
    }

    private boolean captureSignature() {

        boolean error = false;
        String errorMessage = "";

        if(error){
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 105, 50);
            toast.show();
        }

        return error;
    }

    private String getTodaysDate() {

        final Calendar c = Calendar.getInstance();
        String res =  c.get(Calendar.YEAR) +"-";

        if(c.get(Calendar.MONTH)+1 >= 10) {
            res += (c.get(Calendar.MONTH)+1) + "-";
        } else {
            res += "0" + (c.get(Calendar.MONTH)+1) + "-";
        }

        if (c.get(Calendar.DAY_OF_MONTH) <= 10) {
            res += "0" +c.get(Calendar.DAY_OF_MONTH);
        } else {
            res += c.get(Calendar.DAY_OF_MONTH);
        }
        return  res ;
    }

    private boolean prepareDirectory()
    {
        try
        {
            if (makedirs())
            {
                return true;
            } else {
                return false;
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(this, "Could not initiate File System.. Is Sdcard mounted properly?", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean makedirs()
    {
        File tempdir = new File(tempDir);
        if (!tempdir.exists())
            tempdir.mkdirs();

        return (tempdir.isDirectory());
    }

    public class signature extends View
    {
        private static final float STROKE_WIDTH = 5f;
        private static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;
        private Paint paint = new Paint();
        private Path path = new Path();

        private float lastTouchX;
        private float lastTouchY;
        private RectF dirtyRect = new RectF();

        public signature(Context context, AttributeSet attrs)
        {
            super(context, attrs);
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(STROKE_WIDTH);
            dirtyRect.set(dirtyRect.left,dirtyRect.top,dirtyRect.right,dirtyRect.bottom/2);
        }

        public void save(View v)
        {
            Log.v("log_tag", "Width: " + v.getWidth());
            Log.v("log_tag", "Height: " + v.getHeight());
            mBitmap = addWaterMark(getBitmapFromView(mSignature));
            try
            {
                FileOutputStream mFileOutStream = new FileOutputStream(mypath.getAbsolutePath());
                mBitmap.compress(Bitmap.CompressFormat.PNG, 90, mFileOutStream);
                mFileOutStream.flush();
                mFileOutStream.close();
                //In case you want to delete the file
                //boolean deleted = mypath.delete();
                Log.v("log_tag","where: " + mypath.toString() );
                //If you want to convert the image to string use base64 converter
            }
            catch(Exception e)
            {
                Log.v("log_tag", e.toString());
            }
        }

        public Bitmap getBitmapFromView(View view) {
            //Define a bitmap with the same size as the view
            Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),Bitmap.Config.ARGB_8888);
            //Bind a canvas to it
            Canvas canvas = new Canvas(returnedBitmap);
            //Get the view's background
            Drawable bgDrawable =view.getBackground();
            if (bgDrawable!=null)
                //has background drawable, then draw it on the canvas
                bgDrawable.draw(canvas);
            else
                //does not have background drawable, then draw white background on the canvas
                canvas.drawColor(Color.WHITE);
            // draw the view on the canvas
            view.draw(canvas);
            //return the bitmap
            return returnedBitmap;
        }

        public void clear()
        {
            path.reset();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            canvas.drawPath(path, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            float eventX = event.getX();
            float eventY = event.getY();
            mGetSign.setEnabled(true);

            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(eventX, eventY);
                    lastTouchX = eventX;
                    lastTouchY = eventY;
                    return true;

                case MotionEvent.ACTION_MOVE:

                case MotionEvent.ACTION_UP:

                    resetDirtyRect(eventX, eventY);
                    int historySize = event.getHistorySize();
                    for (int i = 0; i < historySize; i++)
                    {
                        float historicalX = event.getHistoricalX(i);
                        float historicalY = event.getHistoricalY(i);
                        expandDirtyRect(historicalX, historicalY);
                        path.lineTo(historicalX, historicalY);
                    }
                    path.lineTo(eventX, eventY);
                    break;

                default:
                    debug("Ignored touch event: " + event.toString());
                    return false;
            }

            invalidate((int) (dirtyRect.left - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                    (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));

            lastTouchX = eventX;
            lastTouchY = eventY;

            return true;
        }

        private void debug(String string){
        }

        private void expandDirtyRect(float historicalX, float historicalY)
        {
            if (historicalX < dirtyRect.left)
            {
                dirtyRect.left = historicalX;
            }
            else if (historicalX > dirtyRect.right)
            {
                dirtyRect.right = historicalX;
            }

            if (historicalY < dirtyRect.top)
            {
                dirtyRect.top = historicalY;
            }
            else if (historicalY > dirtyRect.bottom)
            {
                dirtyRect.bottom = historicalY;
            }
        }

        private void resetDirtyRect(float eventX, float eventY)
        {
            dirtyRect.left = Math.min(lastTouchX, eventX);
            dirtyRect.right = Math.max(lastTouchX, eventX);
            dirtyRect.top = Math.min(lastTouchY, eventY);
            dirtyRect.bottom = Math.max(lastTouchY, eventY);
        }

        private Bitmap addWaterMark(Bitmap src) {
            int w = src.getWidth();
            int h = src.getHeight();
            Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
            Canvas canvas = new Canvas(result);

            if(unavailable == false ) {
                canvas.drawBitmap(src, 0, 0, null);
            } else {
                canvas.drawBitmap(textAsBitmap("Client indisponible",100.0f,100),(w/2) - 300,h/2,null);
            }
            Bitmap waterMark = textAsBitmap(value,70.0f,50);
            canvas.drawBitmap(waterMark, (w/2) - 150, h/2, null);
            canvas.drawBitmap(waterMark, 30, 50, null);
            canvas.drawBitmap(waterMark, 30, h - 100, null);
            canvas.drawBitmap(waterMark, w - 300, 50, null);
            canvas.drawBitmap(waterMark, w - 300, h - 100, null);

            return result;
        }

        public Bitmap textAsBitmap(String text, float textSize,int alpha) {
            Paint paint = new Paint();
            paint.setTextSize(textSize);
            paint.setColor(Color.BLACK);
            paint.setAlpha(alpha);
            paint.setTextAlign(Paint.Align.LEFT);
            float baseline = -paint.ascent(); // ascent() is negative
            int width = (int) (paint.measureText(text) + 0.5f); // round
            int height = (int) (baseline + paint.descent() + 10.0f);
            Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(image);
            canvas.drawText(text, 0, baseline, paint);
            return image;
        }
    }
}