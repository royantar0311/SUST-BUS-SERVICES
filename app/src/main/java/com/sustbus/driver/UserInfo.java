package com.sustbus.driver;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.auth.User;

import java.util.HashMap;
import java.util.Map;

public class UserInfo {
    public static final int STUDENT_PERMITTED= 1;
    public static final int STUDENT_NOT_PERMITTED= 0;
    public static final int PERMISSION_PENDING=-1;
    private static UserInfo instance=new UserInfo();

    private String email;
    private long isStudentPermitted;
    private String userName;
    private boolean driver;
    private Double lat;
    private Double lang;
    private DatabaseReference databaseReference;
    private DatabaseReference userDatabaseReference;
    private DatabaseReference userLocationData;
    private FirebaseAuth mAuth;
    private String uId;

    private String regiNo;

    public static UserInfo getInstance(){
        return instance;
    }

    public static void setInstance(UserInfo i){
        instance=i;
    }

    public UserInfo() {
    }



    /**Private method needed for the builder
     * class to store them on the main userinfo
     * object after getting all the data*/

    UserInfo userInfo(Builder builder) {
        instance.email = builder.email;
        instance.isStudentPermitted = builder.isStudentPermitted;
        instance.userName = builder.userName;
        instance.driver = builder.isDriver;
        instance.lat = builder.lat;
        instance.lang = builder.lang;
        instance.uId = builder.uId;
        return this;
    }

    /**This method creates a object of the inner Builder class */

    public  Builder getBuilder(){
        return new Builder();
    }

    /**Builder pattern to build a Userinfo*/

    public class Builder{
        String email;
        long isStudentPermitted;
        String userName;
        boolean isDriver;
        Double lat;
        Double lang;
        String uId;

        Builder(){}

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setIsStudentPermitted(long isStudentPermitted) {
            this.isStudentPermitted = isStudentPermitted;
            return this;
        }

        public Builder setUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder setDriver(boolean driver) {
            isDriver = driver;
            return this;
        }

        public Builder setLat(Double lat) {
            this.lat = lat;
            return this;
        }

        public Builder setLang(Double lang) {
            this.lang = lang;
            return this;
        }
        public Builder setuId(String uId) {
            this.uId = uId;
            return this;
        }

        public UserInfo build(){
            return userInfo(this);
        }
    }

    public Map<String, Object> toMap(){
        Map<String, Object>map = new HashMap<>();
        map.put("email", email);
        map.put("isStudentPermitted",isStudentPermitted);
        map.put("userName", userName);
        map.put("driver", driver);
        map.put("lat",lat);
        map.put("lang",lang);
        map.put("uId",uId);
        map.put("regiNo",regiNo);
        return map;
    }

    public String getRegiNo() {
        return regiNo;
    }

    public void setRegiNo(String regiNo) {
        this.regiNo = regiNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getIsStudentPermitted() {
        return isStudentPermitted;
    }

    public void setIsStudentPermitted(long isStudentPermitted) {
        this.isStudentPermitted = isStudentPermitted;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isDriver() {
        return driver;
    }

    public void setDriver(boolean driver) {
        this.driver = driver;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLang() {
        return lang;
    }

    public void setLang(Double lang) {
        this.lang = lang;
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }
}
