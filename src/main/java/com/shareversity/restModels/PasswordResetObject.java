package com.shareversity.restModels;

public class PasswordResetObject {
    private String email;

    public PasswordResetObject(String email) {
        super();
        this.email = email;
    }

    public PasswordResetObject() {
        super();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
