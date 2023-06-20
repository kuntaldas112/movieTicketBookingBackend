package com.fse1.movieapp.exception;

public class UserNotFound extends RuntimeException {
    public UserNotFound(String noUserAvailable) {
        super(noUserAvailable);
    }
}
