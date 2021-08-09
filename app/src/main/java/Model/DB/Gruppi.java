package Model.DB;

import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Helper.AuthHelper;
import Helper.FirestoreHelper;
import Helper.StorageHelper;
import Model.Closures.ClosureBitmap;
import Model.Closures.ClosureBoolean;
import Model.Closures.ClosureList;
import Model.Closures.ClosureResult;
import Model.Pojo.Evento;
import Model.Pojo.Gruppo;
import Model.Pojo.Utente;

public class Gruppi extends ResultsConverter{

    private static final String GRUPPO_COLLECTION = "Gruppi";

    /**
     * Used to update the information of the group into the updateField method.
     */
    public static final String NOME_FIELD = "nome";
    public static final String DESCRIZIONE_FIELD = "descrizione";
    public static final String COMPONENTI_FIELD = "idComponenti";



    /** Update the information of the GROUP.
     *
     * @param groupId the id of the group
     * @param closureBool get called with true if the task is successful, false otherwise.
     * @param firstField the name of the first field to update
     * @param firstValue tha new value of the first field
     * @param otherFieldAndValues an array of object with other field and values.
     */
    public static final void updateFields(String groupId,ClosureBoolean closureBool, String firstField, Object firstValue, Object... otherFieldAndValues ){
        FirestoreHelper.db.collection(GRUPPO_COLLECTION).document(groupId).update(firstField,firstValue,otherFieldAndValues).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(closureBool != null) closureBool.closure(task.isSuccessful());
            }
        });
    }

    /**
     * Return the name of the group given as parameter.
     *
     * @param groupId id of the group whose name you want to know.
     * @param closureRes invoked with Pair <String, Boolean> with the group name and a flag indicating is the logged in user is the admin of the group if the task is successful, null otherwise.
     */
    public static final void getGroupName(String groupId, ClosureResult<Pair<String,Boolean>> closureRes){
        if(AuthHelper.isLoggedIn()){
            FirestoreHelper.db.collection(GRUPPO_COLLECTION).document(groupId).get().addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    if (closureRes != null){
                        Gruppo g = task.getResult().toObject(Gruppo.class);
                        if(g != null)
                            closureRes.closure(new Pair<>(g.getNome(), (g.getIdAmministratore().equals(AuthHelper.getUserId())) ? true : false ));
                        else
                            closureRes.closure(null);
                    }
                }else {
                    if(closureRes != null) closureRes.closure(null);
                }
            });
        }
    }

    /** Remove a user from a group.
     *
     * @param idUser    id of the user to remove from the group.
     * @param idGroup   group from which the user is to be removed.
     * @param closureBool   get called with true if the task is successful, false otherwise.
     */
    public static final void removeUserFromGroup(String idUser, String idGroup, ClosureBoolean closureBool){
        if(AuthHelper.isLoggedIn()){
            FirestoreHelper.db.collection(GRUPPO_COLLECTION).document(idGroup).update("idComponenti", FieldValue.arrayRemove(idUser)).addOnCompleteListener(task -> {
                if (closureBool != null) closureBool.closure(task.isSuccessful());
            });
        }
    }

    /** Delete group.
     *
     * @param idGroup   id of the group to delete.
     * @param closureBool   get called with true if the task is successful, false otherwise.
     */
    public static final void deleteGroup(String idGroup, ClosureBoolean closureBool){
        if(AuthHelper.isLoggedIn()){
            FirestoreHelper.db.collection(GRUPPO_COLLECTION).document(idGroup).delete().addOnCompleteListener(task -> {
                if (closureBool != null) closureBool.closure(task.isSuccessful());
            });
        }
    }

    /** Add a new user to a group.
     *
     * @param idNewUser id of the new user to insert into the group
     * @param idGroup   group in which the new user is to be entered
     * @param closureBool   get called with true if the task is successful, false otherwise.
     */
    public static final void addUserToGroup(String idNewUser, String idGroup, ClosureBoolean closureBool){
        if(AuthHelper.isLoggedIn()){
            FirestoreHelper.db.collection(GRUPPO_COLLECTION).document(idGroup).update("idComponenti", FieldValue.arrayUnion(idNewUser)).addOnCompleteListener(task -> {
                if (closureBool != null) closureBool.closure(task.isSuccessful());
            });
        }
    }

    /** Return a list of Gruppo of which the user is the admin or a member.
     *
     * @param closureList get called with a List of Gruppo if the task is successful, null otherwise.
     */
    public static final void getAllMyGroups(ClosureList<Gruppo> closureList){
        if(AuthHelper.isLoggedIn()){
            //Group of which the user is the administrator
            Task secondTask = FirestoreHelper.db.collection(GRUPPO_COLLECTION).whereArrayContains("idComponenti",AuthHelper.getUserId()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if(task.isSuccessful()){
                        if(closureList != null) closureList.closure(convertResults(task,Gruppo.class));
                    }else{
                        if(closureList != null) closureList.closure(null);
                    }
                }
            });
        }
    }

    /** Return the list of Gruppo of which the user is the admin.
     *
     * @param closureList get called with a List of Gruppo if the task is successful, null otherwise.
     */
    public static final void getAdministrationGroups(ClosureList<Gruppo> closureList){
        if(AuthHelper.isLoggedIn()){
            FirestoreHelper.db.collection(GRUPPO_COLLECTION).whereEqualTo("idAmministratore",AuthHelper.getUserId()).get().addOnCompleteListener(task -> {
                if (closureList != null){
                    if(task.isSuccessful() ){
                        closureList.closure(convertResults(task,Gruppo.class));
                    }else closureList.closure(null);
                }
            });
        }
    }

    /** Add a new group to Firestore. The idGroup is randomly picked and the id inside the pojo object is avoided.
     *
     * @param g group to add to Firestore
     * @param closureRes invoked with the id of the created group if the creation is successful, with null otherwise.
     */
    public static final void addNewGroup(Gruppo g, ClosureResult<String> closureRes){

        FirestoreHelper.db.collection(GRUPPO_COLLECTION).add(g).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                String groupId = task.getResult().getId();
                if(g.getImmagine()!= null) {
                    uploadGroupImage(g.getImmagine(),groupId,isSuccessful -> {
                        if(isSuccessful){
                            if (closureRes != null) closureRes.closure(groupId);
                        }else{
                            if (closureRes != null) closureRes.closure(null);
                        }

                    });
                }else{
                    if (closureRes != null) closureRes.closure(groupId);
                }
            }else{
                if (closureRes != null) closureRes.closure(null);
            }

        });
    }

    /** Return a list of all group on Firestore.
     *
     * @param closureList ClosureList of Gruppo type.
     */
    public static final void getAllGroups(ClosureList<Gruppo> closureList){
        FirestoreHelper.db.collection(GRUPPO_COLLECTION).get().addOnCompleteListener(task -> {
            if(closureList != null){
                if(task.isSuccessful()){
                    closureList.closure(convertResults(task,Gruppo.class));
                }else closureList.closure(null);
            }
        });
    }

    /** Return the group with the specified id
     *
     * @param closureRes the closure invoked with the group.
     */
    public static final void getGroup(String idGruppo, ClosureResult<Gruppo> closureRes){
        FirestoreHelper.db.collection(GRUPPO_COLLECTION).document(idGruppo).get().addOnCompleteListener(task -> {
            if(closureRes!= null){
                if(task.isSuccessful()){
                    closureRes.closure(task.getResult().toObject(Gruppo.class));
                }else closureRes.closure(null);
            }
        });
    }

    /** Upload the group image to the Firestore Storage. It is placed inside a directory named after the group id.
     * It is replaced if already present.
     * The image is placed inside the following path: Gruppi/\idGruppo\/immagineProfilo
     *
     * User must be logged-in.
     *
     * @param file file to upload
     * @param idGruppo  group whose image you want to change
     * @param closureBool   get called with true if the task is successful, false otherwise.
     */
    public static final void uploadGroupImage(Uri file, String idGruppo, ClosureBoolean closureBool){
        if (!AuthHelper.isLoggedIn()){
            if (closureBool != null) closureBool.closure(false);
            return;
        }

        String finalChildName = "Gruppi/" + idGruppo + "/immagineProfilo";
        StorageHelper.uploadImage(file, finalChildName, new ClosureBoolean() {
            @Override
            public void closure(boolean isSuccess) {
                if(!isSuccess){
                    if(closureBool != null) closureBool.closure(false);
                } else {
                    FirestoreHelper.db.collection(GRUPPO_COLLECTION).document(idGruppo).update("isImmagineUploaded", true).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(closureBool != null) closureBool.closure(task.isSuccessful());
                        }
                    });
                }
            }
        });
    }

    /** Download group image from the Firebase Firestore.
     *
     *  User must be logged-in.
     *
     * @param idGruppo  group whose image you want to download.
     * @param closureBitmap get called with the Bitmap if the task is successful, null otherwise.
     */
    public static final void downloadGroupImage(String idGruppo, ClosureBitmap closureBitmap){
        if (!AuthHelper.isLoggedIn()){
            if (closureBitmap != null) closureBitmap.closure(null);
            return;
        }

        String finalChildName = GRUPPO_COLLECTION + "/" + idGruppo + "/immagineProfilo";
        StorageHelper.downloadImage(finalChildName,closureBitmap);
    }

}
