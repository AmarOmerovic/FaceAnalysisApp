package com.amaromerovic.faceanalysisapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amaromerovic.faceanalysisapp.adapter.FaceAnalysisRecyclerViewAdapter;
import com.amaromerovic.faceanalysisapp.databinding.ActivityMainBinding;
import com.amaromerovic.faceanalysisapp.model.FaceModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Facing;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private Facing cameraFacing = Facing.FRONT;
    private ArrayList<FaceModel> faceModelArrayList;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private FaceDetector detector;
    private final DecimalFormat df = new DecimalFormat("0.00");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        faceModelArrayList = new ArrayList<>();
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetFromInclude.bottomSheet);

        FaceDetectorOptions highAccuracyOpts = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build();

        detector = FaceDetection.getClient(highAccuracyOpts);


        // Camera setup
        binding.cameraView.setFacing(cameraFacing);
        binding.cameraView.setLifecycleOwner(MainActivity.this);


        binding.cameraToggleButton.setOnClickListener(view -> {
            cameraFacing = cameraFacing == Facing.FRONT ? Facing.BACK : Facing.FRONT;
            binding.cameraView.setFacing(cameraFacing);
        });


        binding.bottomSheetFromInclude.openGalleryButton.setOnClickListener(view -> getContent.launch("image/*"));

        binding.bottomSheetFromInclude.takePictureButton.setOnClickListener(view -> {
            binding.cameraView.takePicture();
            binding.cameraView.addCameraListener(new CameraListener() {
                @Override
                public void onPictureTaken(@NonNull PictureResult result) {
                    super.onPictureTaken(result);
                    result.toBitmap(result.getSize().getWidth(), result.getSize().getHeight(), bitmap -> analyseImage(bitmap));
                }
            });
        });

        binding.bottomSheetFromInclude.bottomSheetRecyclerView.setHasFixedSize(true);
        binding.bottomSheetFromInclude.bottomSheetRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        binding.bottomSheetFromInclude.bottomSheetRecyclerView.setAdapter(new FaceAnalysisRecyclerViewAdapter(faceModelArrayList));


    }

    ActivityResultLauncher<String> getContent = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri result) {
            Intent intent = new Intent(MainActivity.this, CropperActivity.class);
            intent.putExtra("DATA", result.toString());
            cropImage.launch(intent);
        }
    });


    ActivityResultLauncher<Intent> cropImage = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Intent data = result.getData();
            if (data != null) {
                String resultOne = data.getStringExtra("RESULT");
                Uri resultUri = Uri.parse(resultOne);
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), resultUri);
                Bitmap bitmap = null;
                try {
                    bitmap = ImageDecoder.decodeBitmap(source);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                analyseImage(bitmap);
            }
        }
    });

    private void analyseImage(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(this, "Bitmap is null", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.bottomSheetFromInclude.imageView.setImageBitmap(null);
        faceModelArrayList.clear();
        Objects.requireNonNull(binding.bottomSheetFromInclude.bottomSheetRecyclerView.getAdapter()).notifyDataSetChanged();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        showProgress();


        InputImage image = InputImage.fromBitmap(bitmap, 0);
        Bitmap mutableImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);


        Canvas canvas = new Canvas(mutableImage);
        Paint facePaint = new Paint();
        facePaint.setColor(Color.RED);
        facePaint.setStyle(Paint.Style.STROKE);
        facePaint.setStrokeWidth(3f);

        Paint faceTextPaint = new Paint();
        faceTextPaint.setColor(Color.RED);
        faceTextPaint.setTextSize(50f);
        faceTextPaint.setTypeface(Typeface.SANS_SERIF);

        Paint landmarkPaint = new Paint();
        landmarkPaint.setColor(Color.WHITE);
        landmarkPaint.setStyle(Paint.Style.FILL);
        landmarkPaint.setStrokeWidth(5f);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        Snackbar snackbar = Snackbar.make(binding.cameraContainer, "No faces detected!", Snackbar.LENGTH_SHORT);
                        snackbar.setBackgroundTint(Color.BLACK);
                        View view = snackbar.getView();
                        TextView textView = view.findViewById(com.google.android.material.R.id.snackbar_text);
                        if (textView != null) {
                            textView.setTextColor(Color.WHITE);
                        }
                        snackbar.show();
                        hideProgress();
                        return;
                    }

                    for (Face face : faces) {

                        if (face != null && face.getSmilingProbability() != null && face.getLeftEyeOpenProbability() != null && face.getRightEyeOpenProbability() != null && face.getTrackingId() != null) {
                            int id = face.getTrackingId();
                            float smileProb = face.getSmilingProbability() * 100;
                            float leftEyeOpenProb = face.getLeftEyeOpenProbability() * 100;
                            float rightEyeOpenProb = face.getRightEyeOpenProbability() * 100;

                            faceModelArrayList.add(new FaceModel((id + 1), "\tSmile probability: " + df.format(smileProb) + "%\n\tLeft eye open probability: "
                                    + df.format(leftEyeOpenProb) + "%\n\tRight eye open probability:" + df.format(rightEyeOpenProb) + "%\n"));

                            FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                            if (leftEar != null) {
                                PointF leftEarPos = leftEar.getPosition();
                                canvas.drawCircle(leftEarPos.x, leftEarPos.y, 10f, landmarkPaint);
                            }
                            FaceLandmark nose = face.getLandmark(FaceLandmark.NOSE_BASE);
                            if (nose != null) {
                                PointF nosePos = nose.getPosition();
                                canvas.drawCircle(nosePos.x, nosePos.y, 10f, landmarkPaint);
                            }
                            FaceLandmark rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR);
                            if (rightEar != null) {
                                PointF rightEarPos = rightEar.getPosition();
                                canvas.drawCircle(rightEarPos.x, rightEarPos.y, 10f, landmarkPaint);
                            }

                            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                            if (leftEye != null) {
                                PointF leftEyePos = leftEye.getPosition();
                                canvas.drawCircle(leftEyePos.x, leftEyePos.y, 10f, landmarkPaint);
                            }

                            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                            if (rightEye != null) {
                                PointF rightEyePos = rightEye.getPosition();
                                canvas.drawCircle(rightEyePos.x, rightEyePos.y, 10f, landmarkPaint);
                            }

                            FaceLandmark mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM);
                            FaceLandmark mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT);
                            FaceLandmark mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT);
                            if (mouthBottom != null && mouthLeft != null && mouthRight != null) {
                                PointF mouthBottomPos = mouthBottom.getPosition();
                                PointF mouthLeftPos = mouthLeft.getPosition();
                                PointF mouthRightPos = mouthRight.getPosition();
                                canvas.drawLine(mouthLeftPos.x, mouthLeftPos.y, mouthBottomPos.x, mouthBottomPos.y, landmarkPaint);
                                canvas.drawLine(mouthBottomPos.x, mouthBottomPos.y, mouthRightPos.x, mouthRightPos.y, landmarkPaint);
                            }


                            Rect bounds = face.getBoundingBox();
                            canvas.drawRect(bounds, facePaint);
                            float textLength = faceTextPaint.measureText("Face: " + (id + 1));
                            canvas.drawText("Face: " + (id + 1), bounds.exactCenterX() - (textLength / 2f), bounds.exactCenterY() - (bounds.height() / 1.6f), faceTextPaint);
                            canvas.save();

                        }
                    }
                    Objects.requireNonNull(binding.bottomSheetFromInclude.bottomSheetRecyclerView.getAdapter()).notifyDataSetChanged();
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    binding.bottomSheetFromInclude.imageView.setImageBitmap(mutableImage);
                    hideProgress();
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    hideProgress();
                });

    }

    private void showProgress() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        binding.bottomSheetFromInclude.bottomSheetButtonProgressBar.setVisibility(View.VISIBLE);
        binding.bottomSheetFromInclude.takePictureButton.setVisibility(View.GONE);
        binding.bottomSheetFromInclude.openGalleryButton.setVisibility(View.GONE);
        binding.bottomSheetFromInclude.openGalleryButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
    }


    private void hideProgress() {
        binding.bottomSheetFromInclude.bottomSheetButtonProgressBar.setVisibility(View.GONE);
        binding.bottomSheetFromInclude.takePictureButton.setVisibility(View.VISIBLE);
        binding.bottomSheetFromInclude.openGalleryButton.setVisibility(View.VISIBLE);
        binding.bottomSheetFromInclude.openGalleryButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.cameraView.open();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.cameraView.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.cameraView.destroy();
    }


}