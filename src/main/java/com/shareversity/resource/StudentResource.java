package com.shareversity.resource;

import com.lambdaworks.crypto.SCryptUtil;
import com.shareversity.dao.StudentDao;
import com.shareversity.dao.StudentLoginDao;
import com.shareversity.restModels.*;
import com.shareversity.utils.MailClient;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

@Path("/shareversity")
public class StudentResource {
    StudentDao studentDao = new StudentDao();
    StudentLoginDao studentLoginDao = new StudentLoginDao();

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String hello() {
        return "<body style=\"background-color:pink;\">" +
                "<H1>Welcome to Shareversity Website, this is Backend!</H1>" +
                "</body>";
    }

    @POST
    @Path("/registration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response sendRegistrationCode(Students students) {

        String studentEmail = students.getEmail();
        String firstName = students.getFirstName();

        // check if the email string is null or empty
        if (studentEmail == null || studentEmail.trim().length() == 0) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Email Id can't be null or empty").build();
        }

        // check if the email id is with edu domain
        // TODO: add method to verify email is valid and exists and also check for .edu

        if(!isValidEmailId(studentEmail)){
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Please enter a valid student email Id with edu domain").build();
        }

        Students studentObject = studentDao.findUserByEmailId(studentEmail);

        // check if the student is an already registered student
        // if student record exist but not registration is not confirmed then we allow to update
        // new information
        if (studentObject != null && studentObject.getIsCodeVerified()) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Student already exists").build();
        }

        //todo: add security code constraint to be valid only for 15 minutes
        String secretCode = createSecretCode();

        // Create TimeStamp for Key Expiration for 15 min
        Timestamp securityCodeExpiration = new Timestamp(System.currentTimeMillis()+ 15*60*1000);
        students.setSecurityCodeExpiration(securityCodeExpiration);
        students.setSecretCode(secretCode);

        students.setCreateDate(new Date());

        boolean success = false;
        Students newStudent;
        if (studentObject == null) {
            newStudent = studentDao.addNewStudent(students);
            success = true;
        } else {
            Students existingStudent = studentDao.findUserByEmailId(students.getEmail());
            existingStudent.setPassword(students.getPassword());
            existingStudent.setLastName(students.getLastName());
            existingStudent.setFirstName(students.getFirstName());
            existingStudent.setCreateDate(new Date());
            newStudent = studentDao.updateStudent(existingStudent);
            success = true;
        }

        if (newStudent != null && success == true) {
            MailClient.sendRegistrationEmail(firstName, studentEmail, secretCode);

            return Response.status(Response.Status.OK).
                    entity("Security Code is sent Successfully!").build();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                entity("Something Went Wrong" + studentEmail).build();
    }

    @POST
    @Path("/verification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response verifySecurityCode(EmailVerification emailVerification) {
        String securityCode = emailVerification.getSecurityCode();
        String email = emailVerification.getEmail();

        Students student = studentDao.findUserByEmailId(email);

        if (student == null) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Invalid User").build();
        }

        if (student.getIsCodeVerified()) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Security code is already verified").build();
        }

        String databaseSecurityCode = student.getSecretCode();
        Timestamp expirationTime = student.getSecurityCodeExpiration();

        System.out.println("databaseSecurityCode: " + databaseSecurityCode);
        System.out.println("securityCode: " + securityCode);

        if (!databaseSecurityCode.equals(securityCode)) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Please enter the correct security code").build();
        } else {

            Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());

            // check if the database time is after the current time
            if (expirationTime.after(currentTimestamp)) {
                student.setIsCodeVerified(true);

                //todo: change date to timestamp
                student.setCreateDate(new Date());

                // set code verified to true
                boolean codeVerifiedUpdated = studentDao.updateUserCodeVerified(student);

                if (!codeVerifiedUpdated) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                            entity("Something Went Wrong. Please retry.").build();
                }

                StudentLogin studentLogin = new StudentLogin();
                studentLogin.setEmail(student.getEmail());
                studentLogin.setStudentPassword(student.getPassword());

                Date date = new Date();
                Timestamp timeStamp = new Timestamp(date.getTime());
                studentLogin.setLoginTime(timeStamp);

                StudentLogin studentLogin1 = studentLoginDao.createStudentLogin(studentLogin);


                if (studentLogin1 == null) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                            entity("Something Went Wrong. Please retry.").build();
                }

                return Response.status(Response.Status.OK).
                        entity("You have successfully confirmed your account with the " +
                                "email " + email + ". You will use this email address to log in.!").build();

            } else {
                return Response.status(Response.Status.OK).
                        entity("Registration key expired, please request another security code.").build();

            }
        }
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response loginUser(LoginObject loginInput){
        Students students =
                studentDao.checkIfStudentIsRegistered(loginInput.getUserEmail());

        if(students == null){
            return Response.status(Response.Status.NOT_FOUND).
                    entity("Invalid User " + loginInput.getUserEmail()).build();
        }

        if(!students.getPassword().equals(loginInput.getPassword())){
            return Response.status(Response.Status.NOT_FOUND).
                    entity("Wrong password. Try again or click Forgot password to reset it.").build();
        }

        return Response.status(Response.Status.OK).
                entity("You have successfully logged in!").build();
    }

    @POST
    @Path("/password-change")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response changePassword(PasswordChangeObject passwordChangeObject) {

        // check if the student is a confirmed registered student
        Students student = studentDao.checkIfStudentIsRegistered(passwordChangeObject.getEmail());

        if (student == null) {
            return Response.status(Response.Status.NOT_FOUND).
                    entity("This Email doesn't exist: " + passwordChangeObject.getEmail()).build();
        }

        String enteredOldPassword = passwordChangeObject.getOldPassword();
        String enteredNewPassword = passwordChangeObject.getNewPassword();

        if (enteredOldPassword.equals(enteredNewPassword)) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).
                    entity("The New Password can't be same as Old password.").build();
        }

        if (!student.getPassword().equals(enteredOldPassword)) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Password Incorrect, please enter a correct old password").build();
        }

        student.setPassword(enteredNewPassword);
        studentDao.updateStudent(student);

        return Response.status(Response.Status.OK).
                entity("Password Changed Successfully!").build();
    }

    @POST
    @Path("/password-reset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendPasswordResetLink(PasswordResetObject passwordResetObject) {
        String studentEmail = passwordResetObject.getEmail();

        if (studentEmail == null) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Email Id can't be null").build();
        }

        if (studentEmail.trim().length() == 0) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Email can't be an Empty String, please enter a valid email").build();
        }else {

            Students students = studentDao.findUserByEmailId(studentEmail);

            // Check if student's record exists
            if (students == null) {
                return Response.status(Response.Status.NOT_FOUND).
                        entity("Email doesn't exist, please enter a valid email for password reset.").build();
            }

            // Check if student is a confirmed registered student
            if (students.getIsCodeVerified() == false) {

                return Response.status(Response.Status.NOT_FOUND).
                        entity("Password can't be reset, please register first.").build();
            }

            MailClient.sendPasswordResetEmail(studentEmail);
            return Response.status(Response.Status.OK).
                    entity("Password Reset link sent successfully!").build();
        }
    }

    @POST
    @Path("/password-create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPassword(PasswordCreateObject passwordCreateObject) {
        String email = passwordCreateObject.getEmail();
        String password = passwordCreateObject.getPassword();

        if (password == null || password.trim().length() == 0) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Password can't be null or empty").build();
        }

        // before create password, a student login should exist
        Students student = studentDao.findUserByEmailId(email);

        if (student == null) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Invalid Student details. Student does not exist").build();
        }

        if (!student.getIsCodeVerified()) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Student is not a registered student. Please register first.").build();
        }

        System.out.println(password);
        System.out.println(student.getPassword());
        if (password.equals(student.getPassword())) {
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("Password can't be same as old password.").build();
        }

        student.setPassword(password);
        studentDao.updateStudent(student);

        return Response.status(Response.Status.BAD_REQUEST).
                entity("Congratulations Password is reset successfully!").build();

    }
    private boolean isValidEmailId(String userEmail) {
        if(userEmail.contains(".edu")){
            return true;
        }
        return false;
    }

    private String createSecretCode() {
        return UUID.randomUUID().toString().substring(30);
    }
}