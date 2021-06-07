package Helper;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import Model.Closures.ClosureBoolean;
import Model.Closures.ClosureResult;
import Model.DB.Enti;
import Model.DB.Utenti;


/**
 * Helper class for the autentication on Firebase.
 */
public class AuthHelper {

    private static final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    /**
     * All the possible type of the user logged in
     */
    public enum UserType{
        Utente,
        Ente,
        None
    }


    /**
     * Return the type of the user logged-in
     * @param closureRes
     */
    public static final void getLoggedUserType(ClosureResult<UserType> closureRes){
        if(!isLoggedIn()){
            if(closureRes != null) closureRes.closure(UserType.None);
            return;
        }

       Utenti.isValidUser(getUserId(), isSuccess -> {
           if(isSuccess){
               if(closureRes != null) closureRes.closure(UserType.Utente);
           }else{

               //Check if is an Ente
               Enti.isValidEnte(getUserId(), isSuccess1 -> {
                   if(isSuccess1){
                       if(closureRes != null) closureRes.closure(UserType.Ente);
                   }else{
                       if(closureRes != null) closureRes.closure(UserType.None);
                   }
               });
           }
       });
    }

    /** Check if the the user is already logged in
     * @return true if the user is already logged in, false otherwise
     */
    public static final boolean isLoggedIn(){
        FirebaseUser cU = mAuth.getCurrentUser();
        return cU != null;
    }

    /**
     *
     * @return The id of the logged-in user if there is, an empty string otherwise
     */
    public static final String getUserId(){
        return isLoggedIn() ? mAuth.getCurrentUser().getUid() : null;
    }

    /**
     * Function used to create a new user
     *
     * @param email
     * @param psw
     * @param closureResult return the Uid of the created user, null otherwise.
     */
    public static final void createNewAccount(String email, String psw, ClosureResult<String> closureResult){
        mAuth.createUserWithEmailAndPassword(email,psw).addOnCompleteListener(task -> {
            if (closureResult == null) return;

            if (task.isSuccessful()) closureResult.closure(task.getResult().getUser().getUid());
            else closureResult.closure(null);
        });
    }

    /** Function used to login.
     *
     * @param email
     * @param psw
     * @param closureBool Listener to manage the success of failure of the login.
     */
    public static final void singIn(String email,String psw,ClosureBoolean closureBool){
        mAuth.signInWithEmailAndPassword(email,psw).addOnCompleteListener(task -> {
            if (closureBool == null) return;

            if (task.isSuccessful()) closureBool.closure(true);
            else closureBool.closure(false);
        });
    }

    public static final void updatePsw(String newPsw, ClosureBoolean closureBool){
        if(isLoggedIn()){
            mAuth.getCurrentUser().updatePassword(newPsw).addOnCompleteListener(task -> {
                if (closureBool == null) return;

                if (task.isSuccessful()) closureBool.closure(true);
                else closureBool.closure(false);
            });
        }
    }

    public static final void updateEmail(String newEmail, ClosureBoolean closureBool){
        if(isLoggedIn()){
            mAuth.getCurrentUser().updateEmail(newEmail).addOnCompleteListener(task -> {
                if (closureBool == null) return;

                if (task.isSuccessful()) closureBool.closure(true);
                else closureBool.closure(false);
            });
        }
    }

    public static final void sendPswResetEmail(String email,ClosureBoolean closureBool){
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (closureBool == null) return;

            if (task.isSuccessful()) closureBool.closure(true);
            else closureBool.closure(false);
        });
    }

    /**
     * Double autentication for some secutiry sensitive action
     * @param email
     * @param psw
     * @param closureBool Completition handler to handle the success or the insuccess
     */
    public static final void reAuthenticate(String email, String psw, ClosureBoolean closureBool){
        if(isLoggedIn()){
            FirebaseUser currentUser = mAuth.getCurrentUser();
            AuthCredential cred = EmailAuthProvider.getCredential(email,psw);

            currentUser.reauthenticate(cred).addOnCompleteListener(task -> {
                if (closureBool == null) return;

                if (task.isSuccessful()) closureBool.closure(true);
                else closureBool.closure(false);
            });
        }
    }

    /**
     * Function used to Log-out
     */
    public static final void logOut(){
        mAuth.signOut();
    }
}