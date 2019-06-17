package studenty.biedne.imagewarper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int CAMERA_REQUEST_CODE = 1 ;
    private static final int IMAGE_GALLERY_REQUEST_CODE = 2 ;
    Button captureImageButton;
    Button openImageButton;
    ImageView imageEditorView;
    public static Uri storedPictureUri = null;
    Uri cameraUri = null;
    boolean imageSelected = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openImageButton=findViewById(R.id.openImageButton);
        openImageButton.setOnClickListener(this);
        captureImageButton=findViewById(R.id.captureImageButton);
        captureImageButton.setOnClickListener(this);
        imageEditorView=findViewById(R.id.launchEditorView);
        imageEditorView.setOnClickListener(this);

    }

    @Override
    public void onClick(View view){
        if(view == openImageButton){
            if(Build.VERSION.SDK_INT >=23){
                if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED) {
                    loadImage();
                }else{
                    String[] permissionRequested= {Manifest.permission.READ_EXTERNAL_STORAGE};
                    requestPermissions(permissionRequested, IMAGE_GALLERY_REQUEST_CODE);
                }
            }else{
                loadImage();
            }
        }
        else if(view == captureImageButton){
            if(Build.VERSION.SDK_INT >=23){
                if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) {
                    captureImage();
                }else{
                    String[] permissionRequested= {Manifest.permission.CAMERA};
                    requestPermissions(permissionRequested, CAMERA_REQUEST_CODE);
                }
            }else{
                captureImage();
            }
        }
        else if(view == imageEditorView){
            launchImageEditor();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==CAMERA_REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                captureImage();
            }else{
                Toast.makeText(this, getString(R.string.camera_permission_not_granted), Toast.LENGTH_LONG).show();
            }
        }else if(requestCode==IMAGE_GALLERY_REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                loadImage();
            }else{
                Toast.makeText(this, getString(R.string.storage_access_not_granted), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void launchImageEditor(){
        if(imageSelected) {
            Intent intent = new Intent(MainActivity.this, ImageEditorActivity.class);
            intent.putExtra("imagePath", storedPictureUri.toString());
            startActivity(intent);
        } else
            Toast.makeText(this, "Image not selected", Toast.LENGTH_LONG).show();
    }

    public void loadImage(){
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String pictureDirectoryPath = pictureDirectory.getPath();
        Uri data = Uri.parse(pictureDirectoryPath);
        photoPickerIntent.setDataAndType(data, "image/*");

        startActivityForResult(photoPickerIntent,IMAGE_GALLERY_REQUEST_CODE);
    }

    public void captureImage(){
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imageFile = null;
        if(cameraIntent.resolveActivity(getPackageManager())!=null){
            try{
                imageFile = getImageFile();

            }catch(IOException e){
                e.printStackTrace();
            }
        }
        if(imageFile != null){
            cameraUri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", imageFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            startActivityForResult(cameraIntent,CAMERA_REQUEST_CODE);
        }
    }

    private File getImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd__HHmmss").format(new Date());
        String imageName = "jpg_"+timeStamp+"_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(imageName,".jpg", storageDir);
        return imageFile;
    }

    private void displayImage() {
        if(storedPictureUri != null){
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),storedPictureUri);
                bitmap = new BitmapDrawable(getResources(), bitmap).getBitmap();
                imageEditorView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                storedPictureUri = cameraUri;
                displayImage();
                imageSelected = true;
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.photo_not_taken), Toast.LENGTH_LONG).show();
            }
        }else if(requestCode == IMAGE_GALLERY_REQUEST_CODE){
            if(resultCode==RESULT_OK){
                storedPictureUri = data.getData();
                displayImage();
                imageSelected = true;
            }else{
                Toast.makeText(getApplicationContext(), getString(R.string.image_not_selected), Toast.LENGTH_LONG).show();
            }
        }
    }

}
