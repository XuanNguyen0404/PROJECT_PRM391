package com.example.football_field_booking.daos;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.football_field_booking.dtos.CartItemDTO;
import com.example.football_field_booking.dtos.FootballFieldDTO;
import com.example.football_field_booking.dtos.TimePickerDTO;
import com.example.football_field_booking.dtos.UserDTO;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FootballFieldDAO {
    private FirebaseFirestore db;

    private static final String COLLECTION_FOOTBALL_FIELD = "footballFields";
    public static final String SUB_COLLECTION_BOOKING = "booking";
    public static final String FIELD_IMAGES_FOLDER = "football_field_images";
    private static final String COLLECTION_USERS = "users";
    public static final String CONST_OF_PROJECT = "constOfProject";
    public static final String SUB_COLLECTION_RATING = "rating";
    public static final String STATUS_ACTIVE = "active";



    public FootballFieldDAO() {
        db = FirebaseFirestore.getInstance();
    }

    public Task<Void> createNewFootballField(FootballFieldDTO fieldDTO, UserDTO owner, List<TimePickerDTO> timePickerDTOList) throws Exception {
        DocumentReference footballFieldReference = db.collection(COLLECTION_FOOTBALL_FIELD).document();
        fieldDTO.setFieldID(footballFieldReference.getId());
        WriteBatch batch = db.batch();
        Map<String, Object> dataFBField = new HashMap<>();
        dataFBField.put("fieldInfo", fieldDTO);
        batch.set(footballFieldReference, dataFBField, SetOptions.merge());

        Map<String, Object> dataOwner = new HashMap<>();
        dataOwner.put("ownerInfo", owner);
        batch.set(footballFieldReference, dataOwner, SetOptions.merge());

        DocumentReference footballFieldInfoReference = db.collection(COLLECTION_USERS).document(owner.getUserID());
        Map<String, Object> dataInOwner = new HashMap<>();
        dataInOwner.put("fieldsInfo", FieldValue.arrayUnion(fieldDTO));
        batch.update(footballFieldInfoReference, dataInOwner);

        Map<String, Object> dataTimePicker = new HashMap<>();
        dataTimePicker.put("timePicker", timePickerDTOList);
        batch.set(footballFieldReference, dataTimePicker, SetOptions.merge());

        return batch.commit();
    }

    public Task<Uri> uploadImgFootballFieldToFirebase(Uri uri) throws Exception {

        StorageReference mStoreRef = FirebaseStorage.getInstance().getReference(FIELD_IMAGES_FOLDER)
                .child(System.currentTimeMillis() + ".png");
        UploadTask uploadTask = mStoreRef.putFile(uri);
        return uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                return mStoreRef.getDownloadUrl();
            }
        });
    }

    public Task<DocumentSnapshot> getConstOfFootballField() throws Exception {
        DocumentReference doc = db.collection(CONST_OF_PROJECT).document("const");
        return doc.get();
    }

    public Task<QuerySnapshot> getAllFootballField() {
        return db.collection(COLLECTION_FOOTBALL_FIELD).whereEqualTo("fieldInfo.status",STATUS_ACTIVE).get();
    }

    public Task<DocumentSnapshot> getFieldByID(String fieldID) {
        DocumentReference doc = db.collection(COLLECTION_FOOTBALL_FIELD).document(fieldID);
        return doc.get();
    }

    public Task<Void> updateFootballField(FootballFieldDTO fieldDTO, FootballFieldDTO fieldOldDTO, String userID, List<TimePickerDTO> timePickerDTOList) throws Exception {
        DocumentReference docField = db.collection(COLLECTION_FOOTBALL_FIELD).document(fieldDTO.getFieldID());
        DocumentReference docOwner = db.collection(COLLECTION_USERS).document(userID);

        Log.d("USER", "docFieldOfOwner: " + docOwner);
        return db.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                Map<String, Object> dataField = new HashMap<>();
                dataField.put("fieldInfo.name", fieldDTO.getName());
                dataField.put("fieldInfo.location", fieldDTO.getLocation());
                dataField.put("fieldInfo.geoPoint", fieldDTO.getGeoPoint());
                dataField.put("fieldInfo.geoHash", fieldDTO.getGeoHash());
                dataField.put("fieldInfo.type", fieldDTO.getType());
                dataField.put("fieldInfo.image", fieldDTO.getImage());
                dataField.put("fieldInfo.status", fieldDTO.getStatus());

                transaction.update(docField, dataField);

                Map<String, Object> dataDeleteField = new HashMap<>();
                dataDeleteField.put("fieldsInfo", FieldValue.arrayRemove(fieldOldDTO));
                transaction.update(docOwner, dataDeleteField);

                Map<String, Object> dataUpdateField = new HashMap<>();
                dataUpdateField.put("fieldsInfo", FieldValue.arrayUnion(fieldDTO));
                transaction.update(docOwner, dataUpdateField);

                Map<String, Object> dataDeleteTimePicker = new HashMap<>();
                dataDeleteTimePicker.put("timePicker", FieldValue.delete());
                transaction.update(docField, dataDeleteTimePicker);

                Map<String, Object> dataUpdateTimePicker = new HashMap<>();
                dataUpdateTimePicker.put("timePicker", timePickerDTOList);
                transaction.set(docField, dataUpdateTimePicker, SetOptions.merge());
                return null;
            }
        });
    }

    public Task<QuerySnapshot> searchByLikeNameForUser(String name){
        return db.collection(COLLECTION_FOOTBALL_FIELD)
                .whereGreaterThanOrEqualTo("fieldInfo.name",name)
                .whereLessThanOrEqualTo("fieldInfo.name", name + "\uf8ff")
                .get();
    }

    public Task<QuerySnapshot> searchByTypeForUser(String type) {
        return db.collection(COLLECTION_FOOTBALL_FIELD)
                .whereEqualTo("fieldInfo.type", type)
                .whereEqualTo("fieldInfo.status", STATUS_ACTIVE)
                .get();
    }

    public Task<QuerySnapshot> getBookingByFieldAndDate(List<CartItemDTO> cart) {
        List<String> listFieldAndDate = new ArrayList<>();
        for (CartItemDTO cartItemDTO : cart) {
            listFieldAndDate.add(cartItemDTO.getFieldInfo().getFieldID() + cartItemDTO.getDate());
        }
        return db.collectionGroup(SUB_COLLECTION_BOOKING).whereIn("fieldAndDate", listFieldAndDate).get();
    }

    public Task<QuerySnapshot> getBookingOfAFieldByDate(String fieldID, String date) {
        return db.collection(COLLECTION_FOOTBALL_FIELD).document(fieldID)
                .collection(SUB_COLLECTION_BOOKING).whereEqualTo("date", date).get();
    }

    public void countRating(FootballFieldDTO fieldDTO, String ownerID) throws Exception {
        getRating(fieldDTO.getFieldID()).addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                float sumRating = 0f;
                int totalRating = 0;
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    float rating = doc.get("rating", Float.class);
                    sumRating += rating;
                    totalRating++;
                }
                float avgRating = sumRating / totalRating;
                Log.d("USER", "rating: " + avgRating);

                WriteBatch batch = db.batch();

                DocumentReference docField = db.collection(COLLECTION_FOOTBALL_FIELD).document(fieldDTO.getFieldID());
                DocumentReference docUser = db.collection(COLLECTION_USERS).document(ownerID);

                Map<String, Object> dataUserDelete = new HashMap<>();
                dataUserDelete.put("fieldsInfo", FieldValue.arrayRemove(fieldDTO));
                batch.update(docUser, dataUserDelete);

                fieldDTO.setRate(avgRating);
                Map<String, Object> dataUserUpdate = new HashMap<>();
                dataUserUpdate.put("fieldsInfo", FieldValue.arrayUnion(fieldDTO));
                batch.update(docUser, dataUserUpdate);

                Map<String, Object> dataField = new HashMap<>();
                dataField.put("fieldInfo.rate", avgRating);
                batch.update(docField, dataField);

                batch.commit();

            }
        });
    }

    public Task<QuerySnapshot> getRating(String fieldID) {
        return db.collection(COLLECTION_FOOTBALL_FIELD).document(fieldID)
                .collection(SUB_COLLECTION_RATING).orderBy("date", Query.Direction.DESCENDING).get();
    }

    public List<Task<QuerySnapshot>> searchNearMe (GeoLocation geoMe, double radiusInM) {
        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(geoMe, radiusInM);
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        for (GeoQueryBounds b : bounds) {
            Query q = db.collection(COLLECTION_FOOTBALL_FIELD)
                    .orderBy("fieldInfo.geoHash")
                    .startAt(b.startHash)
                    .endAt(b.endHash);

            tasks.add(q.get());
        }

        // Collect all the query results together into a single list
        return tasks;
    }

    public Task<QuerySnapshot> getListBookingByFieldID(String fieldID) {
        return db.collection(COLLECTION_FOOTBALL_FIELD).document(fieldID)
                .collection(SUB_COLLECTION_BOOKING)
                .orderBy("bookingAt",Query.Direction.DESCENDING)
                .get();
    }
}
