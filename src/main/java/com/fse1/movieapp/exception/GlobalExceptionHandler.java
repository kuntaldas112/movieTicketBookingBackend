package com.fse1.movieapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fse1.movieapp.payload.response.MessageResponse;

@ControllerAdvice
public class GlobalExceptionHandler extends Exception {

    @ExceptionHandler(MoviesNotFound.class)
    public ResponseEntity<?> incaseOfMoviesNotFound(Exception e){
        return new ResponseEntity<>(new MessageResponse(e.getMessage(),"Error"), HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(UserNotFound.class)
    public ResponseEntity<?> incaseOfUserNotFound(Exception e){
    	return new ResponseEntity<>(new MessageResponse(e.getMessage(),"Error"), HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(SeatAlreadyBooked.class)
    public ResponseEntity<?> incaseOfSeatsAlreadyBooked(Exception e){
        return new ResponseEntity<>(new MessageResponse(e.getMessage(),"Error"), HttpStatus.BAD_REQUEST);
    }
}
