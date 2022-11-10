package com.shareversity.restModels;

public class PasswordCreateObject {
    private String email;
    private String password;

    public PasswordCreateObject(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public PasswordCreateObject(){
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
