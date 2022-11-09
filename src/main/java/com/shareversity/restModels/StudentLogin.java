package com.shareversity.restModels;

import java.sql.Timestamp;

public class StudentLogin {
    private String email;
    private String studentPassword;
    private Timestamp loginTime;

    public StudentLogin(String email, String studentPassword, Timestamp loginTime) {
        this.email = email;
        this.studentPassword = studentPassword;
        this.loginTime = loginTime;
    }

    public StudentLogin(){

    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStudentPassword() {
        return studentPassword;
    }

    public void setStudentPassword(String studentPassword) {
        this.studentPassword = studentPassword;
    }

    public Timestamp getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Timestamp loginTime) {
        this.loginTime = loginTime;
    }


}
