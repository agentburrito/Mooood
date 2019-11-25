package com.example.mooood;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Date;

/**
 * FILE PURPOSE: This is for create new mood event
 **/

public class EditEventActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "Debugging";
    public static final String MOOD_EVENT = "Mood Event";

    //Declare variables for later use
    ViewPager moodRoster;
    SwipeMoodsAdapter moodRosterAdapter;
    List<Emoticon> moodImages;

    //Firebase setup!
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentReference documentReference;

    TextView socialSituation;
    EditText reason;

    //for image upload
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int PICK_IMAGE_REQUEST = 1;
    String currentPhotoPath;
    ImageView imageUpload;
    Uri imageUri;

    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private StorageTask uploadTask;

    SimpleDateFormat simpleDateFormat;
    Calendar calendar;
    TextView dateAndTimeMood;
    Button submitButton;
    Button locationButton;

    //For location services inside the activity
    private static final String MAP_VIEW_BUNDLE_KEY="MapViewBundleKey";
    private MapView mapView;
    private GoogleMap gmap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng moodLocation;

    MoodEvent moodEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        //connecting variables to their respective element (should be in setup, need to edit CreateEventActivity)
        reason = findViewById(R.id.reason);
        socialSituation = findViewById(R.id.social_situation);
        imageUpload = findViewById(R.id.image_reason);
        dateAndTimeMood = findViewById((R.id.date_and_time));

        //Accessing the document
        Intent intent = getIntent();
        moodEvent = intent.getParcelableExtra(MOOD_EVENT);
        documentReference = db.collection("MoodEvents").document(moodEvent.getAuthor());

        //Setup the EditEventActivity [VIEW]
        createMoodRoster();

        swipeMoodAdapterSetup();
        customSwipeMoodStyling();

        //Set initial values according to MoodEvent
        setProperEmoticon();
        setProperReason();
        setProperSocialSituation();
        setProperImageReason();
        setProperTimeAndDate();

        //Invoke methods for selection of MoodEvent details
        moodSelection();
        socialSituationClickListener();
//        dateAndTimePickerClickListener();

        //setup for image upload
        storageReference = FirebaseStorage.getInstance().getReference("reason_image");
        databaseReference = FirebaseDatabase.getInstance().getReference("reason_image");

        imageUploadClickListener();

        //==============================================================================================
        // LOCATION services
        //==============================================================================================
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mapView = findViewById(R.id.createMapView);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        submitBtnClickListener();

    } //end of onCreate

    //==============================================================================================
    // Setup CreateEventActivity [VIEW]
    //==============================================================================================
    /**
     * This creates the actual mood roster and populates it with Emoticons
     **/
    private void createMoodRoster(){
        moodImages = new ArrayList<>();
        moodImages.add(new Emoticon("HAPPY", 2));
        moodImages.add(new Emoticon("SAD", 2));
        moodImages.add(new Emoticon("LAUGHING", 2));
        moodImages.add(new Emoticon("IN LOVE", 2));
        moodImages.add(new Emoticon("ANGRY", 2));
        moodImages.add(new Emoticon("SICK", 2));
        moodImages.add(new Emoticon("AFRAID", 2));
    }

    /**
     * This is the setup for the adapter that contains all the emoticons
     **/
    private void swipeMoodAdapterSetup(){
        moodRosterAdapter = new SwipeMoodsAdapter(moodImages, this);
        moodRoster = findViewById(R.id.mood_roster);
        moodRoster.setAdapter(moodRosterAdapter);
    }

    /**
     * This just customs the mood roster so the next and previous emoticon can be seen partially
     **/
    private void customSwipeMoodStyling(){
        moodRoster.setClipToPadding(false);
        moodRoster.setPadding(250,0,250,0);
        moodRoster.setPageMargin(50);
    }

    /**
     * This sets the proper emoticon according to MoodEvent (not empty)
     */
    private void setProperEmoticon(){
        int pos=0;
        for(int i = 0; i < 6; i++){
            if(moodEvent.getEmotionalState().equals(moodImages.get(i).getEmotionalState())){
                pos = i;
                break;
            }
            else pos = 0;
        }
        moodRoster.setCurrentItem(pos);
    }

    /**
     * This sets the proper reason according to MoodEvent (might be null)
     **/
    private void setProperReason(){
        String reasonData = moodEvent.getReason();
        if(reasonData != null){
            reason.setText(reasonData);
        }
    }

    /**
     * This sets the proper social situation according to MoodEvent (might be null)
     */
    private void setProperSocialSituation(){
        String socialSituationData = moodEvent.getSocialSituation();
        if(socialSituationData != null){
            socialSituation.setText(moodEvent.getSocialSituation());
        }
    }

    /**
     * This sets the proper image for reason according to MoodEvent (might be null)
     */
    private void setProperImageReason(){
        String imageUrl = moodEvent.getImageUrl();
        if(imageUrl != null){
            Picasso.get().load(imageUrl).into(imageUpload);

        } else{
            imageUpload.setImageResource(R.drawable.temp_image_upload);
        }
    }
    /**
     * This checks if reason is only 3 words or 20 characters
     */

//    private void inputChecker(){
//        submitButton.setEnabled(false);
//        if(moodDate != null && moodTime != null){
//            submitButton.setEnabled(true);
//        }
//        final EditText reasonText = findViewById(R.id.reason);
//        reasonText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void afterTextChanged(Editable s) {
//            }
//
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                if (s.length() > 0)
//                {
//                    int number = countWords(s.toString());
//                    if (number < 4){
//                        moodReason = reasonText.getText().toString();
//                        reasonCount = true;
//                        submitButton.setEnabled(true);
//
//                    }
//                    else{
//                        Toast.makeText(EditEventActivity.this, "reason cannot be more than 3 words!",
//                                Toast.LENGTH_SHORT).show();
//                        reasonCount = false;
//                        submitButton.setEnabled(false);
//                    }
//                }
//
//            }
//        });
//    }

    /**
     * This sets the proper time and date for the textView and the dialog picker as well (not null)
     */
    private void setProperTimeAndDate(){
        String dateData = moodEvent.getDate();
        String timeData = moodEvent.getTime();

        //set value of TextView
        String dateAndTimeText = dateData + " " + timeData;
        dateAndTimeMood.setText(dateAndTimeText);

        //get the current date to set value for date and time pickers
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM/dd/yyyy h:mm a", Locale.getDefault());

        try{
            Date dateAndTime = simpleDateFormat.parse(dateAndTimeText);
            dateAndTimePickerClickListener(dateAndTime);

        } catch (ParseException e){
            e.printStackTrace();

        }
    }

    //==============================================================================================
    // Methods for Selection of MoodEvent details
    //==============================================================================================
    /**
     * This is a listener of the selections of mood from Mood Roster
     **/
    private void moodSelection(){

        //click listener for Emoticon
        moodRoster.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //no need to use but must be here
            }

            @Override
            public void onPageSelected(int position) {
                moodEvent.setEmotionalState(moodImages.get(position).getEmotionalState());
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //no need to use but must be here
            }
        });

    }

    /**
     * This is a listener for social situation
     **/
    private void socialSituationClickListener(){
        socialSituation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                socialSituationOptions();
            }
        });
    }

    /**
     * This contains the options for social situation
     **/
    private void socialSituationOptions(){
        final CharSequence[] options = {
                "Alone",
                "With Someone",
                "With Group",
                "With Crowd"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(EditEventActivity.this);

        builder.setTitle("Choose a Social Situation");

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                socialSituation.setText(options[i].toString());
                moodEvent.setSocialSituation(options[i].toString());
            }
        });

        builder.show();
    }

    /**
     * This is accesses the fragment that is used to obtain date and time of MoodEvent
     */
    private void dateAndTimePickerClickListener(final Date currentDate){
        simpleDateFormat = new SimpleDateFormat("MMM/dd/yyyy h:mm a", Locale.getDefault());
        dateAndTimeMood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calendar = Calendar.getInstance();
                calendar.setTime(currentDate);

                new DatePickerDialog(EditEventActivity.this, DateDataSet, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(calendar.DAY_OF_MONTH)).show();
            }
        });
    }

    // After user decided on a date, store those in our calendar variable and then start the TimePickerDialog immediately
    private final DatePickerDialog.OnDateSetListener DateDataSet = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthOfYear);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            //get Date
            moodEvent.setDate( new SimpleDateFormat("MMM dd yyyy", Locale.getDefault()).format(calendar.getTime()) );

            //go to TimePicker
            new TimePickerDialog(EditEventActivity.this, TimeDataSet, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
        }
    };

    // After user decided on a time, save them into our calendar instance, and now parse what our calendar has into the TextView
    private final TimePickerDialog.OnTimeSetListener TimeDataSet = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);

            //get Time
            moodEvent.setTime( new SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.getTime()) );

            //set TexView to correspond with input data
            dateAndTimeMood.setText(simpleDateFormat.format(calendar.getTime()));
        }
    };

    //==========================================================================================
    // UPLOAD IMAGE METHODS (Note: Use design pattern to put all this into a different file)
    //==========================================================================================
    /**
     * This opens the image gallery of the phone for image upload
     * */
    private void imageUploadClickListener(){
        imageUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraOrGallery();
            }
        });
    }

    /**
     * This gives a choice between camera or photo gallery
     **/
    private void cameraOrGallery(){
        final CharSequence[] options = {
                "Take a photo",
                "Choose from gallery"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(EditEventActivity.this);

        builder.setTitle("Select");

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(options[i].equals("Take a photo")){
                    dispatchTakePictureIntent();
                } else if(options[i].equals("Choose from gallery")){
                    openFileChooser();
                }
            }
        });

        builder.show();
    }

    /**
     * This is for selecting image from photo gallery
     **/
    private void openFileChooser(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * This creates a temp file for the camera taken photo
     **/
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * This is for capturing image from camera
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }
        }
    }

    /**
     * This is for displaying the preview for image provided by User
     * @param requestCode
     *      This is the request code from openFileChooser or dispatchTakePictureIntent
     * @param resultCode
     *      This is the result code from openFileChooser or dispatchTakePictureIntent
     * @param data
     *      This is the data from openFileChooser or dispatchTakePictureIntent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        //if from photo gallery
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
            imageUri = data.getData();
            Picasso.get().load(imageUri).into(imageUpload);

        } else if(requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK){ //if from camera
            galleryAddPic();

            File f = new File(currentPhotoPath);
            imageUri = Uri.fromFile(f);
            Picasso.get().load(imageUri).into(imageUpload);

        }
    }

    /**
     * This returns a string which represents the extension of the given uri
     * @param uri
     *      This is the uri of the image to be uploaded
     * @return
     *      The String extension of the uri
     **/
    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    /**
     * This saves the image to the gallery of the android device
     */
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    /**
     * This uploads the image into Firebase (note: this is async)
     **/
    private void uploadImage(){
        if(imageUri != null){
            final StorageReference fileReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));

            uploadTask = fileReference.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {

                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //set up progress bar on later dev
                                }
                            }, 500);

//                            Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
//                            Uri downloadUrl = urlTask.getResult();

//                            UploadImage uploadImage = new UploadImage(taskSnapshot.getStorage().getDownloadUrl().toString());
//
//
//                            Log.d(TAG, "uploaded image " + uploadImage.getImageUrl());


                        }

                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "failed to upload image");
                        }
                    });

            getUploadedImageUrl(uploadTask, fileReference);

        } else{
            Log.d(TAG, "IMAGE CAPTURE HAS NO URI");
        }

    }
    /**
     * This is needed for checking word lengths on text input fields
     **/
    public static int countWords(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }

        String[] words = input.split("\\s+");
        return words.length;
    }


    /**
     * This gets the url of the uploaded image
     */
    private void getUploadedImageUrl(StorageTask uploadTask, final StorageReference imageReference){
        uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return imageReference.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    moodEvent.setImageUrl(downloadUri.toString());

                    String uploadId = databaseReference.push().getKey();
                    databaseReference.child(uploadId).setValue(downloadUri.toString());

                    submitMoodEventToDB(documentReference, moodEvent);
                } else {
                    // Handle failures
                    // ...
                }
            }
        });
    }

    //==========================================================================================
    // GEOLOCATION METHODS
    //==========================================================================================

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;

        gmap.setIndoorEnabled(true);
        gmap.setMyLocationEnabled(true);
        UiSettings uiSettings = gmap.getUiSettings();
        uiSettings.setIndoorLevelPickerEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);

        final LatLng Edmonton = new LatLng(53.5, -113.5);
        CameraPosition.Builder camBuilder = CameraPosition.builder();
        camBuilder.bearing(0);
        camBuilder.tilt(0);
        camBuilder.target(Edmonton);
        camBuilder.zoom(11);

        CameraPosition cp = camBuilder.build();
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double myLatitude=location.getLatitude();
                            double myLongitude= location.getLongitude();
                            LatLng myLocation= new LatLng(myLatitude,myLongitude);
                            setMoodLocation(myLocation);
                            gmap.addMarker(new MarkerOptions().position(myLocation).title("Current Location"));

                        }
                        else{
                            gmap.addMarker(new MarkerOptions().position(Edmonton).title("Current Location"));
                            setMoodLocation(Edmonton);
                        }
                    }
                });
        gmap.moveCamera(CameraUpdateFactory.newCameraPosition(cp));

        gmap.setOnPoiClickListener(new GoogleMap.OnPoiClickListener() {
            @Override
            public void onPoiClick(PointOfInterest pointOfInterest) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(pointOfInterest.latLng);
                gmap.addMarker(markerOptions);
                gmap.moveCamera(CameraUpdateFactory.newLatLng(pointOfInterest.latLng));
            }
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }
    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    public LatLng getMoodLocation() {
        return moodLocation;
    }

    public void setMoodLocation(LatLng moodLocation) {
        this.moodLocation = moodLocation;
    }



    //==========================================================================================
    // ASSEMBLING MOODEVENT AND SUBMITTING IT TO DB
    //==========================================================================================
    /**
     * This is a click listener for submit button. This actually submits the new MoodEvent ito DB
     * @params accountName
     * This is the accountName of the user that is logged in
     */
    private void submitBtnClickListener(){
        submitButton = findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //necessary methods before MoodEvent submission
                createTimeStamp();
                obtainReason();

                if(uploadTask != null && uploadTask.isInProgress()) {
                    Log.d(TAG, "Upload in Progress");

                } else if(uploadTask == null && imageUri == null){
                    Log.d(TAG, "Image Capture fail");
                    submitMoodEventToDB(documentReference, moodEvent);
                }

                else{
                    uploadImage();
                }

            }
        });
    }

    /**
     * This obtains the reason for MoodEvent
     **/
    private void obtainReason(){
        moodEvent.setReason(reason.getText().toString());
    }

    /**
     * This creates timestamp for moodEvent
     **/
    private void createTimeStamp(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd yyyy h:mm a");

        try {
            moodEvent.setTimeStamp(simpleDateFormat.parse(moodEvent.getDate() + ' ' + moodEvent.getTime()));

        } catch (ParseException e){
            e.printStackTrace();
        }
    }

    /**
     * This adds the MoodEvent to DB
     * **/
    private void submitMoodEventToDB(DocumentReference documentReference, MoodEvent moodEvent){

        documentReference.collection("MoodActivities")
                .document(moodEvent.getDocumentId())
                .set(moodEvent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //These are a method which gets executed when the the task is successful
                        Log.d(TAG, "CreateEventActivity - Data addition successful");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Not successful =(
                        Log.d(TAG, "CreateEventActivity - Data addition failed" + e.toString());
                    }
                });

        //redirect to UserFeedActivity
        Intent intent = new Intent(EditEventActivity.this, UserFeedActivity.class);
        intent.putExtra("accountKey", moodEvent.getAuthor());
        startActivity(intent);
    }
}