package com.sustbus.driver.util;

import java.util.HashMap;
import java.util.Map;

public class UserInfo {
    public static final int STUDENT_PERMITTED = 1;
    public static final int STUDENT_NOT_PERMITTED = 0;
    public static final int PERMISSION_PENDING = -1;
    public static boolean downNeeded = false;
    public static Builder builder = new Builder();
    private static UserInfo instance = new UserInfo();
    private String email;
    private long isStudentPermitted;
    private String userName;
    private boolean driver;
    private Double lat;
    private Double lang;
    private String uId;
    private String regiNo;
    private String url;

    public UserInfo() {
    }

    public static UserInfo getInstance() {
        return instance;
    }

    public static void setInstance(UserInfo i) {
        instance = i;
    }

    /**
     * Private method needed for the builder
     * class to store them on the main userinfo
     * object after getting all the data
     */

    static UserInfo userInfo(Builder builder) {
        instance.email = builder.email;
        instance.isStudentPermitted = builder.isStudentPermitted;
        instance.userName = builder.userName;
        instance.driver = builder.isDriver;
        instance.lat = builder.lat;
        instance.lang = builder.lang;
        instance.uId = builder.uId;
        return instance;
    }

    /**
     * This method creates a object of the inner Builder class
     */

    public static Builder getBuilder() {
        return builder;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("isStudentPermitted", isStudentPermitted);
        map.put("userName", userName);
        map.put("driver", driver);
        map.put("uId", uId);
        map.put("regiNo", regiNo);
        map.put("url", url);
        return map;
    }

    public String toString() {
        return "userInfo new datas"
                + "\nisDriver " + instance.isDriver()
                + "\nuid " + instance.getuId()
                + "\nispermitted " + instance.getIsStudentPermitted()
                + "\nemail " + instance.getEmail()
                + "\nurl " + instance.getUrl()
                + "\nregiNO " + instance.regiNo
                + "\nuserName " + instance.userName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    /**
     * Builder pattern to build a Userinfo
     */

    public static class Builder {
        String email;
        long isStudentPermitted;
        String userName;
        boolean isDriver;
        Double lat;
        Double lang;
        String uId;
        String regiNo;
        String url;

        Builder() {
        }

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

        public Builder setRegiNo(String regiNo) {
            this.regiNo = regiNo;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public UserInfo build() {
            return userInfo(builder);
        }
    }
}