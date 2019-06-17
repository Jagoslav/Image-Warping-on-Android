package studenty.biedne.imagewarper;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ImageEditorActivity extends AppCompatActivity {
    private static final int WRITE_PERMISSION_CODE = 3;
    Button returnButton; // Return to MainActivity
    Button saveButton; //Save image as a new file;
    ImageView imageDisplay; //imageView for the displayed image
    Uri storedPictureUri = null;
    Bitmap originalPicture; // Original bitmap
    Bitmap displayedPicture; // Displayed bitmap
    BitmapShader mShader;
    Canvas drawingBoard;
    Paint paint;
    Point cursorOrigin; //warpingField
    float warpingRadius = 50f;
    Spinner methodSpinner;
    String fileName;
    private int warpingMethodId = 0;
    ArrayAdapter<CharSequence> adapter;

    float cursorScalingFactor;
    BGTask bgTask;
    Bitmap maskMap;
    Point maskMapOrigin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_editor);

        returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        methodSpinner = findViewById(R.id.methodSpinner);
        adapter = ArrayAdapter.createFromResource(this, R.array.warping_methods, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        methodSpinner.setAdapter(adapter);
        methodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                warpingMethodId = position;
                if(warpingMethodId == 0) {
                    cursorOrigin = null;
                    draw(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= 23) {
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        saveImage();
                    } else {
                        String[] permissionRequested = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permissionRequested, WRITE_PERMISSION_CODE);
                    }
                } else {
                    saveImage();
                }
            }
        });

        Intent intent = getIntent();
        storedPictureUri = Uri.parse(intent.getStringExtra("imagePath"));
        imageDisplay = findViewById(R.id.imageDisplay);
        try {
            originalPicture = MediaStore.Images.Media.getBitmap(this.getContentResolver(), storedPictureUri);
            displayedPicture = originalPicture.copy(originalPicture.getConfig(), true);
            mShader = new BitmapShader(originalPicture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            imageDisplay.setImageBitmap(displayedPicture);
            Cursor returnCursor = this.getContentResolver().query(storedPictureUri, null, null, null, null);
            assert returnCursor != null;
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();
            fileName = returnCursor.getString(nameIndex);
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
            returnCursor.close();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), getString(R.string.unable_to_open_image), Toast.LENGTH_LONG).show();
            finish();
        }
        imageDisplay.setOnTouchListener(new DrawingBoardListener());

        cursorScalingFactor = 1;
        bgTask = null;
        maskMap = null;
        maskMapOrigin = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImage();
            } else {
                Toast.makeText(this, getString(R.string.unable_to_save_file), Toast.LENGTH_LONG).show();
            }
        }
    }
    public void saveImage() {
        ContentResolver contentResolver = getContentResolver();
        String modification;
        if (warpingMethodId == 0){
            modification = "not edited";
            draw(false);
        }
        else if (warpingMethodId == 1){
            draw(true);
            modification = "Magnified";}
        else if (warpingMethodId == 2){
            draw(true);
            modification = "Shrunk";}
        else if (warpingMethodId == 3){
            draw(false);
            modification = "Swirled";}
        else if (warpingMethodId == 4){
            draw(false);
            modification = "Pincushioned";}
        else
            modification = "not viable modification";
        SimpleDateFormat simpleDate = new SimpleDateFormat("dd/MM/yyyy");
        String title = modification + "-" + simpleDate.format(new Date()) + "-" + fileName;
        String desc = "Image created using ImageWarper";
        String savedURL = MediaStore.Images.Media.insertImage(contentResolver, displayedPicture, title, desc);

        Toast.makeText(ImageEditorActivity.this, "Image saved as " + savedURL.substring(0, savedURL.lastIndexOf("/")) + title, Toast.LENGTH_LONG).show();

    }

    private void draw(boolean drawOutline) {
        drawingBoard.drawBitmap(originalPicture, 0, 0, null);
        if (cursorOrigin != null) {
            if(warpingMethodId != 3 && warpingMethodId != 4){
                maskMap = null;
                maskMapOrigin = null;
            }
            switch(warpingMethodId) {
                case 0:
                    break;
                case 1:
                    Paint mPaint = new Paint();
                    mPaint.setShader(mShader);
                    Matrix mMatrix = new Matrix();
                    mMatrix.postScale(2f,2f, cursorOrigin.x, cursorOrigin.y);
                    mPaint.getShader().setLocalMatrix(mMatrix);
                    drawingBoard.drawCircle(cursorOrigin.x, cursorOrigin.y, warpingRadius, mPaint);
                    break;
                case 2:
                    Paint sPaint = new Paint();
                    sPaint.setShader(mShader);
                    Matrix sMatrix = new Matrix();
                    sMatrix.postScale(.5f,.5f, cursorOrigin.x, cursorOrigin.y);
                    sPaint.getShader().setLocalMatrix(sMatrix);
                    drawingBoard.drawCircle(cursorOrigin.x, cursorOrigin.y, warpingRadius, sPaint);
                    break;
                case 3:
                case 4:
                    if(bgTask == null){
                        bgTask = new BGTask();
                        bgTask.execute();
                    }
                    else{
                        bgTask.scheduleUpdate();
                    }
                    if(maskMap != null && maskMapOrigin !=null){
                        drawingBoard.drawBitmap(maskMap, maskMapOrigin.x, maskMapOrigin.y, null);
                    }
                    break;
                default:
                    break;
            }
            if(drawOutline)
                drawingBoard.drawCircle(cursorOrigin.x, cursorOrigin.y, warpingRadius, paint);
        }
        imageDisplay.postInvalidate();
    }

    public class DrawingBoardListener implements View.OnTouchListener{

        DrawingBoardListener() {
            drawingBoard = new Canvas(displayedPicture);
            paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(getResources().getColor(R.color.black));
            paint.setStrokeWidth(1);
            paint.setAntiAlias(true);
        }
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch(event.getAction()){
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_DOWN:
                    if(event.getPointerCount() == 1){ //get cursor location
                        if(warpingMethodId == 0)
                            return true;
                        float cursorPosX = event.getX();
                        float cursorPosY = event.getY();
                        cursorOrigin = getRelativePosition(cursorPosX, cursorPosY);
                    }
                    else { //scale cursor size and get center location
                        float firstFingerX = event.getX();
                        float firstFingerY = event.getY();
                        float secondFingerX = event.getX(1);
                        float secondFingerY = event.getY(1);
                        float centerX = (firstFingerX + secondFingerX)/2f;
                        float centerY = (firstFingerY + secondFingerY)/2f;
                        float dx = (secondFingerX - firstFingerX);
                        float dy = (secondFingerY - firstFingerY);
                        cursorOrigin = getRelativePosition(centerX, centerY);
                        warpingRadius = (float)(Math.sqrt(dx*dx + dy*dy)/2f)*cursorScalingFactor;
                    }
                    break;
            }
            draw(true);
            return true;
        }

        protected Point getRelativePosition(float pX, float pY) {
            float pictureCenterX = displayedPicture.getWidth()/2f;
            float pictureCenterY = displayedPicture.getHeight()/2f;
            float viewCenterX = imageDisplay.getWidth()/2;
            float viewCenterY = imageDisplay.getHeight()/2;
            float delta = Math.max(pictureCenterX/viewCenterX, pictureCenterY/viewCenterY);
            cursorScalingFactor = delta;
            paint.setStrokeWidth(5*cursorScalingFactor);
            float posX = pictureCenterX + (pX - viewCenterX)*delta;
            float posY = pictureCenterY + (pY - viewCenterY)*delta;
            return new Point((int) posX, (int) posY);

        }
    }
    private class BGTask extends AsyncTask<Void, Void, Void> {
        Point cursorCopy;
        Bitmap tempMaskMap;
        float tempWarpingRadius;
        boolean outdated;

        @Override
        protected void onPreExecute() {
            outdated = false;
            cursorCopy = cursorOrigin;
            tempWarpingRadius = warpingRadius;
            tempMaskMap = Bitmap.createBitmap(
                    1+(int)(tempWarpingRadius*2),
                    1+(int)(tempWarpingRadius*2),Bitmap.Config.ARGB_8888);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if(!isCancelled()){
                int maskWidth = tempMaskMap.getWidth();
                int maskHeight = tempMaskMap.getHeight();
                float ox = maskWidth / 2f;
                float oy = maskHeight / 2f;
                switch(warpingMethodId) {
                    case 3: // Spiral
                        double toRadians = Math.PI / 180;
                        for(int y = 0; y < maskHeight; y++) {
                            for(int x = 0; x < maskWidth; x++) {
                                float tempRadius = (float)Math.sqrt((ox-x)*(ox-x) + (oy-y)*(oy-y));
                                if(tempRadius <= tempWarpingRadius) {
                                    double angle = toRadians * (tempRadius/tempWarpingRadius) * 360;
                                    double s = Math.sin(angle);
                                    double c = Math.cos(angle);
                                    int xSampled = (int)(c*(x-ox) - s*(y-oy) + cursorCopy.x);
                                    int ySampled = (int)(s*(x-ox) + c*(y-oy) + cursorCopy.y);
                                    int pixelColor;
                                    if(ySampled < 0 || ySampled >= displayedPicture.getHeight()
                                            || xSampled < 0 || xSampled >= displayedPicture.getWidth())
                                        pixelColor = Color.argb(0,0,0,0);
                                    else
                                        pixelColor = originalPicture.getPixel(xSampled,ySampled);
                                    tempMaskMap.setPixel(x,y,pixelColor);
                                }
                            }
                        }
                        break;
                    case 4: // Pincushion distortion
                        double correctionRadius = Math.sqrt(maskWidth*maskWidth + maskHeight*maskHeight)/5f;
                        for(int y = 0; y < maskHeight; y++) {
                            for (int x = 0; x < maskWidth; x++) {
                                float tempRadius = (float)Math.sqrt((ox-x)*(ox-x) + (oy-y)*(oy-y));
                                if (tempRadius <= tempWarpingRadius) {
                                    double newX = x - ox, newY = y - oy, theta = 1.0;
                                    double r = Math.sqrt(newX*newX + newY*newY) / correctionRadius;
                                    if(r != 1.0)
                                        theta = Math.atan(r) / r;
                                    int xSampled = (int)(theta * newX + cursorCopy.x);
                                    int ySampled = (int)(theta * newY + cursorCopy.y);
                                    int pixelColor;
                                    if(ySampled < 0 || ySampled >= displayedPicture.getHeight()
                                            || xSampled < 0 || xSampled >= displayedPicture.getWidth())
                                        pixelColor = Color.argb(0,0,0,0);
                                    else
                                        pixelColor = originalPicture.getPixel(xSampled,ySampled);
                                    tempMaskMap.setPixel(x,y,pixelColor);
                                }
                            }
                        }
                        break;
                }
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            maskMap = tempMaskMap.copy(tempMaskMap.getConfig(), true);
            maskMapOrigin = new Point(
                    (int)(cursorCopy.x -tempWarpingRadius),
                    (int)(cursorCopy.y -tempWarpingRadius));
            draw(true);
            tempMaskMap.recycle();
            if(outdated){
                bgTask.cancel(true);
                bgTask = new BGTask();
                        bgTask.execute();
            }
            else bgTask = null;
        }
        public void scheduleUpdate(){
            outdated = true;
        }
    }
}
