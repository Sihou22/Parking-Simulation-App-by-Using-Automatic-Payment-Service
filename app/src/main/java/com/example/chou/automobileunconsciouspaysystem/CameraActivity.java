package com.example.chou.automobileunconsciouspaysystem;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.chou.automobileunconsciouspaysystem.PR.PlateRecognizer;
import com.example.chou.automobileunconsciouspaysystem.Notify.Notify;
import com.example.chou.automobileunconsciouspaysystem.platerecognizer.base.BaseActivity;
import com.example.chou.automobileunconsciouspaysystem.util.BitmapUtil;
import com.example.chou.automobileunconsciouspaysystem.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class CameraActivity extends BaseActivity implements SurfaceHolder.Callback{

    private Thread thread;
    public DBManager dbHelper;
    private SQLiteDatabase db;
    private BaseActivity alert;
    private Context mContext;
    private Camera.AutoFocusCallback myAutoFocusCallback = null;
    private String plate;

    @BindView(R.id.svCamera)
    SurfaceView mSvCamera;

    @BindView(R.id.ivPlateRect)
    ImageView PlateRectZone;

    @BindView(R.id.ivCapturePhoto)
    ImageView CapturePhotoBtn;

    @BindView(R.id.tvPlateResult)
    TextView PlateResultView;

    @BindView(R.id.parkSelectButton1)
    ToggleButton parkSelectButton1;

    @BindView(R.id.parkSelectButton2)
    ToggleButton parkSelectButton2;

    @BindView(R.id.parkSelectButton3)
    ToggleButton parkSelectButton3;

    @BindView(R.id.nowParkView)
    TextView nowParkView;

    @BindView(R.id.correctbn)
    Button correctbn;

    @BindView(R.id.recheckbn)
    Button recheckbn;

    @BindView(R.id.flashButton)
    ToggleButton flashButton;

    @BindView(R.id.ReMoButton)
    ToggleButton ReMoButton;

    private static final String TAG = CameraActivity.class.getSimpleName();

    private int cameraPosition = 0; // 0???????????????1????????????

    private SurfaceHolder mSvHolder;//SurfaceView????????????
    private Camera mCamera;//?????????
    private Camera.CameraInfo mCameraInfo;//?????????????????????
    private MediaPlayer mShootMP;
    private PlateRecognizer mPlateRecognizer;
    private int parkId =101;
    private String parkname="???????????????";
    private String imagePath="";
    private String imagePathR="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        ButterKnife.bind(this);

        mPlateRecognizer = new PlateRecognizer(this);

        initData();

        mContext = this;

        BaseActivity.showWarming("????????????","????????????????????????????????????????????????????????????",mContext);

        dbHelper = new DBManager(this);
        dbHelper.openDatabase();
        db = dbHelper.getDatabase();

        parkSelectButton1.setChecked(true);
        parkSelectButton2.setChecked(false);
        parkSelectButton3.setChecked(false);
        nowParkView.setText(parkname);

        thread = new Thread(new ScanThread());

        parkSelectButton1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (parkSelectButton1.isChecked()) {
                    parkSelectButton2.setChecked(false);
                    parkSelectButton3.setChecked(false);
                    parkId = 101;
                    parkname = "???????????????";
                    nowParkView.setText(parkname);
                    setToast("?????????"+parkname);
                }
            }
        });
        parkSelectButton2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (parkSelectButton2.isChecked()) {
                    parkSelectButton1.setChecked(false);
                    parkSelectButton3.setChecked(false);
                    parkId = 102;
                    parkname = "?????????????????????";
                    nowParkView.setText(parkname);
                    setToast("?????????"+parkname);
                }
            }
        });
        parkSelectButton3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (parkSelectButton3.isChecked()) {
                    parkSelectButton1.setChecked(false);
                    parkSelectButton2.setChecked(false);
                    parkId = 103;
                    parkname = "???????????????";
                    nowParkView.setText(parkname);
                    setToast("?????????"+parkname);
                }
            }
        });

        flashButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    if (!flashButton.isChecked()) {
                       turnLightOff();
                        setToast("??????????????????");
                    }else
                    {
                       turnLightOn();
                        setToast("??????????????????");
                    }
            }
        });

        ReMoButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (!ReMoButton.isChecked()) {
                    thread.interrupt();
                    setToast("?????????????????????");
                }else
                {
                    new SweetAlertDialog(mContext, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText("????????????")
                            .setContentText("???????????????????????????????????????????????????????????????????????????????????????????????????????????????")
                            .setConfirmText("?????????????????????")
                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sDialog) {
                                    thread.start();
                                    setToast("?????????????????????");
                                    sDialog.cancel();
                                }
                            })
                            .show();

                }
            }
        });


        recheckbn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                PlateResultView.setText("??????????????????");
                correctbn.setVisibility(View.GONE);
                recheckbn.setVisibility(View.GONE);
                deletePicture(imagePathR);
                deletePicture(imagePath);
                Toast.makeText(CameraActivity.this, "??????????????????", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.checkCameraHardware(this) && (mCamera == null)) {
            // ??????camera
            mCamera = getCamera();
            // ??????camera??????
            mCameraInfo = getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK);//?????????????????????
            if (null != mCameraInfo) {
                adjustCameraOrientation();
            }

            if (mSvHolder != null) {
                setStartPreview(mCamera, mSvHolder);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        /**
         * ????????????camera???????????????????????????
         */
        releaseCamera();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    //???????????????data
    private void initData() {
        // ????????????
        mSvHolder = mSvCamera.getHolder(); // ????????????SurfaceView
        // ????????????
        mSvHolder.addCallback(this);//????????????SurfaceHolder.Callback???????????????
    }

    private Camera getCamera() {//????????????
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            camera = null;
            Log.e(TAG, "Camera is not available (in use or does not exist)");
        }
        return camera;
    }

    private Camera.CameraInfo getCameraInfo(int facing) {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == facing) {
                return cameraInfo;
            }
        }
        return null;
    }

    private void adjustCameraOrientation() { // ?????????????????????
        if (null == mCameraInfo || null == mCamera) {
            return;
        }

        int orientation = this.getWindowManager().getDefaultDisplay().getOrientation();
        int degrees = 0;

        switch (orientation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else {
            // back-facing
            result = (mCameraInfo.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    //??????mCamera

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();// ??????????????????????????????
            mCamera.release();
            mCamera = null;
        }
    }



    @OnClick({R.id.ivCapturePhoto,R.id.correctbn})
    public void onClick(View view) {
        switch (view.getId()) {
            case 999: // R.id.id_switch_camera_btn:
                // ?????????????????????
                int cameraCount = 0;
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                cameraCount = Camera.getNumberOfCameras();// ????????????????????????

                for (int i = 0; i < cameraCount; i++) {
                    Camera.getCameraInfo(i, cameraInfo);// ?????????????????????????????????
                    if (cameraPosition == 1) {
                        // ?????????????????????????????????
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                            /**
                             * ????????????camera???????????????????????????
                             */
                            releaseCamera();
                            // ??????????????????????????????
                            mCamera = Camera.open(i);
                            // ??????surfaceview??????????????????
                            setStartPreview(mCamera, mSvHolder);
                            cameraPosition = 0;
                            break;
                        }
                    } else {
                        // ?????????????????? ???????????????
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            /**
                             * ????????????camera???????????????????????????
                             */
                            releaseCamera();
                            mCamera = Camera.open(i);
                            setStartPreview(mCamera, mSvHolder);
                            cameraPosition = 1;
                            break;
                        }
                    }

                }
                break;
            case R.id.ivCapturePhoto:
                // ??????,??????????????????
              //  Camera.Parameters params = mCamera.getParameters();
              //  params.setPictureFormat(ImageFormat.JPEG);
              // DisplayMetrics metric = new DisplayMetrics();
            //    getWindowManager().getDefaultDisplay().getMetrics(metric);
          //      int width = metric.widthPixels;  // ????????????????????????
        //        int height = metric.heightPixels;  // ????????????????????????
              //  params.setPreviewSize(width, height);
                // ????????????
                //params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
               // mCamera.setParameters(params);





                    try {

                  mCamera.takePicture(shutterCallback, null, jpgPictureCallback);
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                }

                break;
            case R.id.correctbn:
                Cursor cur = db.rawQuery("select * from car where carid = ?",new String[]{plate});
                if(cur!=null && cur.getCount() >= 1)//?????????????????????????????????
                {
                    boolean b=cur.moveToNext();
                    final String time = tools.getTime();
                    if(!cur.getString(cur.getColumnIndex("parkstate")).equals("1"))
                    {
                        Cursor nowid = db.rawQuery("select recordid from parkrecord order by recordid desc LIMIT 1",null);
                        nowid.moveToNext();
                        ContentValues values=new ContentValues();
                        values.put("carid",plate);
                        values.put("intime",time);
                        values.put("parkid",parkId);
                        values.put("recordid",nowid.getInt(nowid.getColumnIndex("recordid"))+1);
                        long erroralert = db.insert("parkrecord",null,values);
                        if(erroralert!=-1)
                        {
                            ContentValues n = new ContentValues();
                            n.put("parkstate","1");
                            long changealert = db.update("car", n, "carid=?", new String[]{plate});
                            if(changealert!=-1)
                            {
                                Notify notify01 = new Notify(mContext);
                                notify01.sendNotify("?????????????????????", "????????????"+parkname, mContext);
                            }
                        }
                    }else{
                        String grasp = "0";
                        int money = 0;
                        ContentValues values=new ContentValues();
                        values.put("outtime",time);
                        //????????????
                        Cursor t = db.rawQuery("select * from parkrecord where outtime is null and carid= ?",new String[]{plate});
                        t.moveToNext();
                        String intime = t.getString(t.getColumnIndex("intime"));
                        String outtime = time;
                        try{
                            grasp = tools.countDate(intime,outtime);
                            money = tools.countMoney(grasp);}
                        catch(ParseException e)
                        {}
                        values.put("pay",money);
                        values.put("state","?????????");
                        long erroralert = db.update("parkrecord", values, "carid=? and outtime is ?", new String[]{plate,null});
                        if(erroralert!=-1)
                        {
                            ContentValues n = new ContentValues();
                            n.put("parkstate","0");
                            long changealert = db.update("car", n, "carid=?", new String[]{plate});
                            if(changealert!=-1)
                            {   String payalert = "";
                                Cursor p = db.rawQuery("select balance,owner from car c  LEFT OUTER JOIN user u on c.owner = u.id where carid = ? and payway is null", new String[]{plate});
                                if(p!=null && p.getCount() >= 1)//?????????????????????????????????
                                {
                                    p.moveToFirst();
                                    ContentValues m = new ContentValues();
                                    m.put("balance", p.getInt(p.getColumnIndex("balance")) - money);
                                    long mchangealert = db.update("user", m, "id=?", new String[]{p.getString(p.getColumnIndex("owner"))});
                                    if (mchangealert > -1) {
                                        payalert = "????????????????????????????????????????????????????????????????????????????????????????????????";
                                    }
                                }
                                Notify notify01 = new Notify(mContext);
                                notify01.sendNotify("?????????????????????", "????????????"+parkname+"?????????????????????!??????????????????????????????" + grasp + "???,???????????????" + money + "??????"+payalert, mContext);
                            }
                        }
                    }

                }else{
                    final String time = tools.getTime();
                    Cursor excur = db.rawQuery("SELECT * FROM parkrecord WHERE state is null and carid = ?",new String[]{plate});
                    if(excur!=null && excur.getCount() >= 1 &&tools.getGusetCar()==true)//?????????????????????????????????,??????????????????????????????
                    {
                        int money = 0;
                        String grasp="0";
                        excur.moveToNext();
                        String intime = excur.getString(excur.getColumnIndex("intime"));
                        try {
                            grasp=tools.countDate(intime,time);
                            money = tools.countMoney(grasp);}
                        catch(ParseException e)
                        {}
                        ContentValues values=new ContentValues();
                        values.put("outtime",time);
                        values.put("state","?????????");
                        values.put("pay",money);
                        long erroralert = db.update("parkrecord", values, "carid=? and outtime is ?", new String[]{plate,null});
                        if(erroralert!=-1){
                            Notify notify01 = new Notify(mContext);
                            notify01.sendNotify("??????????????????", "????????????"+plate+"???????????????????????????????????????"+parkname+"?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????"+money+"??????????????????"+grasp+"???", mContext);
                            tools.setGusetCar(false);
                        }
                    }else{//?????????????????????????????????????????????
                        Cursor nowid = db.rawQuery("select recordid from parkrecord order by recordid desc LIMIT 1",null);
                        nowid.moveToNext();
                        ContentValues values=new ContentValues();
                        values.put("carid",plate);
                        values.put("intime",time);
                        values.put("parkid",parkId);
                        values.put("recordid",nowid.getInt(nowid.getColumnIndex("recordid"))+1);
                        long erroralert = db.insert("parkrecord",null,values);
                        if(erroralert!=-1){
                            Notify notify01 = new Notify(mContext);
                            notify01.sendNotify("??????????????????", "????????????"+plate+"?????????????????????????????????"+parkname+"?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????", mContext);
                            tools.setGusetCar(true);
                        }
                    }
                }
                PlateResultView.setText("??????????????????");
                correctbn.setVisibility(View.GONE);
                recheckbn.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Camera.Parameters params = mCamera.getParameters();
        //  params.setPictureFormat(ImageFormat.JPEG);
        // DisplayMetrics metric = new DisplayMetrics();
        //    getWindowManager().getDefaultDisplay().getMetrics(metric);
        //      int width = metric.widthPixels;  // ????????????????????????
        //        int height = metric.heightPixels;  // ????????????????????????
        //  params.setPreviewSize(width, height);
        // ????????????
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        mCamera.setParameters(params);
        setStartPreview(mCamera, mSvHolder);
    }//??????preview

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mSvHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        setStartPreview(mCamera, mSvHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // ???surfaceview???????????????????????????????????????
        /**
         * ????????????camera???????????????????????????
         */
        releaseCamera();
        holder = null;
        mSvCamera = null;
        thread.interrupt();
    }




    /**
     * TakePicture??????
     */
    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            shootSound();
            mCamera.setOneShotPreviewCallback(previewCallback);
        }
    };

    Camera.PictureCallback rawPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            camera.startPreview();
        }
    };

    Camera.PictureCallback jpgPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            camera.startPreview();

            File pictureFile = FileUtil.getOutputMediaFile(FileUtil.FILE_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                // ???????????????
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);//???????????????????????????????????????
                Bitmap normalBitmap = BitmapUtil.createRotateBitmap(bitmap);
//                fos.write(data);
                normalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
                // ????????????
                // ??????????????????????????????
//                try {
//                    MediaStore.Images.Media.insertImage(CameraActivity.this.getContentResolver(),
//                            pictureFile.getAbsolutePath(), pictureFile.getName(), "Photo taked by RoadParking.");
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                }
                // ????????????????????????
                CameraActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + pictureFile.getAbsolutePath())));
                imagePath =  pictureFile.getAbsolutePath();
                Toast.makeText(CameraActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * activity?????????????????????????????????
     *
     * @param mediaFile
     */
    private void returnResult(File mediaFile) {
//        Intent intent = new Intent();
//        intent.setData(Uri.fromFile(mediaFile));
//        this.setResult(RESULT_OK, intent);
        this.finish();
    }

    /**
     * ??????camera??????????????????,?????????
     *
     * @param camera
     */
    private void setStartPreview(Camera camera, SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);//???????????????????????????SurfaceView
            camera.startPreview();//??????
        } catch (IOException e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * ????????????????????????
     */
    private void shootSound() {
        AudioManager meng = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        int volume = meng.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

        if (volume != 0) {
            if (mShootMP == null)
                mShootMP = MediaPlayer.create(this, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            if (mShootMP != null)
                mShootMP.start();
        }
    }

    /**
     * ??????Preview???????????????????????????
     */
    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            // ??????Preview????????????bitmap?????????
            Camera.Size size = mCamera.getParameters().getPreviewSize(); //??????????????????
            final int w = size.width;  //??????
            final int h = size.height;
            final YuvImage image = new YuvImage(data, ImageFormat.NV21, w, h, null);
            // ???Bitmap
            ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
            if (!image.compressToJpeg(new Rect(0, 0, w, h), 100, os)) {
                return;
            }
            byte[] tmp = os.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
            Bitmap rotatedBitmap = BitmapUtil.createRotateBitmap(bitmap);

            cropBitmapAndRecognize(rotatedBitmap);

        }
    };

    public void cropBitmapAndRecognize(Bitmap originalBitmap) {//????????????????????????
        // ?????????????????????
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;  // ???????????????????????? //1080
        int height = metric.heightPixels;  // ????????????????????????//1920
        Bitmap sizeBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true);

        int rectWidth = (int) (PlateRectZone.getWidth() * 1.5);//523
        int rectHight = (int) (PlateRectZone.getHeight() * 1.5);//184
        int[] location = new int[2];
        PlateRectZone.getLocationOnScreen(location);//365???837
        location[0] -= PlateRectZone.getWidth() * 0.5 / 2;//277
        location[1] -= PlateRectZone.getHeight() * 0.5 / 2;//806
        Bitmap normalBitmap = Bitmap.createBitmap(sizeBitmap, location[0], location[1], rectWidth, rectHight);

        // ?????????????????????????????????
        File pictureFile = FileUtil.getOutputMediaFile(FileUtil.FILE_TYPE_PLATE);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");
            return;
        }

        try {
            PlateResultView.setText("????????????...");
            FileOutputStream fos = new FileOutputStream(pictureFile);
            normalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            // ????????????????????????
            CameraActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + pictureFile.getAbsolutePath())));

            // ??????????????????
            plate="";
            plate = mPlateRecognizer.recognize(pictureFile.getAbsolutePath());
            imagePathR=pictureFile.getAbsolutePath();
            //plate="???C11111";
            if (null != plate && !plate.equalsIgnoreCase("0")) {
                correctbn.setVisibility(View.VISIBLE);
                recheckbn.setVisibility(View.VISIBLE);
                plate = plate.substring(plate.indexOf(":")+1);
                PlateResultView.setText(plate);

               /*Cursor cur = db.rawQuery("select * from car where carid = ?",new String[]{plate});
                if(cur!=null && cur.getCount() >= 1)//?????????????????????????????????
                {
                    boolean b=cur.moveToNext();
                    Date now = new Date();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");//?????????????????????????????????
                    final String time = dateFormat.format( now );
                    if(!cur.getString(cur.getColumnIndex("parkstate")).equals("1"))
                    {
                        Cursor nowid = db.rawQuery("select recordid from parkrecord order by recordid desc LIMIT 1",null);
                        nowid.moveToNext();
                        ContentValues values=new ContentValues();
                        values.put("carid",plate);
                        values.put("intime",time);
                        values.put("parkid",parkId);
                        values.put("recordid",nowid.getInt(nowid.getColumnIndex("recordid"))+1);
                        long erroralert = db.insert("parkrecord",null,values);
                        if(erroralert!=-1)
                        {
                            ContentValues n = new ContentValues();
                            n.put("parkstate","1");
                            long changealert = db.update("car", n, "carid=?", new String[]{plate});
                            if(changealert!=-1)
                            {
                                Notify notify01 = new Notify(mContext);
                                notify01.sendNotify("?????????????????????", "????????????"+parkname, mContext);
                            }
                        }
                    }else{
                        String grasp = "0";
                        int money = 0;
                        ContentValues values=new ContentValues();
                        values.put("outtime",time);
                        //????????????
                        Cursor t = db.rawQuery("select * from parkrecord where outtime is null and carid= ?",new String[]{plate});
                        t.moveToNext();
                        String intime = t.getString(t.getColumnIndex("intime"));
                        String outtime = time;
                        try{
                            grasp=tools.countDate(intime,outtime);
                            money = tools.countMoney(grasp);}
                        catch(ParseException e)
                        {}
                        values.put("pay",money);
                        values.put("state","?????????");
                        long erroralert = db.update("parkrecord", values, "carid=? and outtime is ?", new String[]{plate,null});
                        if(erroralert!=-1)
                        {
                            ContentValues n = new ContentValues();
                            n.put("parkstate","0");
                            long changealert = db.update("car", n, "carid=?", new String[]{plate});
                            if(changealert!=-1)
                            {   String payalert = "";
                                Cursor p = db.rawQuery("select balance,owner from car c  LEFT OUTER JOIN user u on c.owner = u.id where carid = ? and payway is null", new String[]{plate});
                                if(p!=null && p.getCount() >= 1)//?????????????????????????????????
                                {
                                    p.moveToFirst();
                                    ContentValues m = new ContentValues();
                                    m.put("balance", p.getInt(p.getColumnIndex("balance")) - money);
                                    long mchangealert = db.update("user", m, "id=?", new String[]{p.getString(p.getColumnIndex("owner"))});
                                    if (mchangealert > -1) {
                                        payalert = "????????????????????????????????????????????????????????????????????????????????????????????????";
                                    }
                                }
                                Notify notify01 = new Notify(mContext);
                                notify01.sendNotify("?????????????????????", "????????????"+parkname+"?????????????????????!??????????????????????????????" + grasp + "???,???????????????" + money + "??????"+payalert, mContext);
                                }
                            }
                        }

                    }else{
                        final String time = tools.getTime();
                        PlateResultView.setText(plate);
                        Cursor excur = db.rawQuery("SELECT * FROM parkrecord WHERE state is null and carid = ?",new String[]{plate});
                        if(excur!=null && excur.getCount() >= 1 &&tools.getGusetCar()==true)//?????????????????????????????????,??????????????????????????????
                        {
                            int money = 0;
                            String grasp="0";
                            excur.moveToNext();
                            String intime = excur.getString(excur.getColumnIndex("intime"));
                            try {
                                grasp=tools.countDate(intime,time);
                                money = tools.countMoney(grasp);}
                            catch(ParseException e)
                            {}
                            ContentValues values=new ContentValues();
                            values.put("outtime",time);
                            values.put("state","?????????");
                            values.put("pay",money);
                            long erroralert = db.update("parkrecord", values, "carid=? and outtime is ?", new String[]{plate,null});
                        if(erroralert!=-1){
                            Notify notify01 = new Notify(mContext);
                            notify01.sendNotify("??????????????????", "????????????"+plate+"???????????????????????????????????????"+parkname+"?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????"+money+"??????????????????"+grasp+"???", mContext);
                            tools.setGusetCar(false);
                            }
                        }else{//?????????????????????????????????????????????
                            Cursor nowid = db.rawQuery("select recordid from parkrecord order by recordid desc LIMIT 1",null);
                            nowid.moveToNext();
                            ContentValues values=new ContentValues();
                            values.put("carid",plate);
                            values.put("intime",time);
                            values.put("parkid",parkId);
                            values.put("recordid",nowid.getInt(nowid.getColumnIndex("recordid"))+1);
                            long erroralert = db.insert("parkrecord",null,values);
                            if(erroralert!=-1){
                                Notify notify01 = new Notify(mContext);
                                notify01.sendNotify("??????????????????", "????????????"+plate+"?????????????????????????????????"+parkname+"?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????", mContext);
                                tools.setGusetCar(true);
                            }
                        }
                    }*/
            } else {
                /*Date now = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");//?????????????????????????????????
                String hehe = dateFormat.format( now );
                System.out.println(hehe);
                PlateResultView.setText(hehe);

                Date time=null;
                try {
                    time = dateFormat.parse(hehe);
                    //time= (Date)sdf.parse(sdf.format(new Date()));
                    System.out.println(time.toString());
                } catch (ParseException e) {

                    e.printStackTrace();
                }*/

                PlateResultView.setText("?????????????????????????????????");
                deletePicture(imagePathR);
                deletePicture(imagePath);
                setToast("???????????????");
            }

        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    public void deletePicture(String path)
    {
        File file =new File(path);
        file.delete();
        getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?",new String[]{path});
        CameraActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(path))));
    }

    public synchronized  void turnLightOn() {
        if (mCamera == null) {
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        if (parameters == null) {
            return;
        }
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(parameters);
    }
    /**
     * ????????????Camera???????????????
     */
    public synchronized void turnLightOff() {
        if (mCamera == null) {
            return;
        }
        Camera.Parameters parameters  = mCamera.getParameters();
        if (parameters == null) {
            return;
        }
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(parameters);
    }

    /*private class CarTask extends AsyncTask<Void, Void, Void> {
        private byte[] mData;
        //????????????
        PalmTask(byte[] data) {
        this.mData = data;
        return;}
    }

    @Override
    protected Void doInBackground(Void... params) {
        // TODO Auto-generated method stub
        Camera.Size size = mCamera.getParameters().getPreviewSize(); //??????????????????
        final int w = size.width;//??????
        final int h = size.height;
        final YuvImage image = new YuvImage(mData, ImageFormat.NV21, w, h, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream(mData.length);
        if(!image.compressToJpeg(new Rect(0, 0, w, h), 100, os))
            return null;
        byte[] tmp = os.toByteArray();
        Bitmap bmp = BitmapFactory.decodeByteArray(tmp, 0,tmp.length);
        doSomethingNeeded(bmp); //???????????????????????????????????????????????????
        return null;
        }

    public void onPreviewFrame(byte[] data, Camera camera) {
        // TODO Auto-generated method stub
        if(null != mFaceTask){
            switch(mFaceTask.getStatus()){
            case RUNNING:
            return;
            case PENDING:
            mFaceTask.cancel(false);
            break;
            }
        }
        mFaceTask = new PalmTask(data);
        mFaceTask.execute((Void)null);
    }*/

private class ScanThread implements Runnable{
    public volatile boolean exit = false;

    public void run() {
        // TODO Auto-generated method stub
        while(!Thread.currentThread().isInterrupted()){
            try {
                if(null != mCamera)
                {
//myCamera.autoFocus(myAutoFocusCallback);
                    mCamera.setOneShotPreviewCallback(previewCallback);
                    Log.i(TAG, "setOneShotPreview...");
                }
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

    }

}
public void setToast(String text)
{
    Toast.makeText(CameraActivity.this, text, Toast.LENGTH_SHORT).show();
}

}


