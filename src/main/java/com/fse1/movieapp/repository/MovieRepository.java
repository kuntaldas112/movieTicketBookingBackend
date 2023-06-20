package com.fse1.movieapp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.fse1.movieapp.models.Movie;

import java.util.List;
public interface MovieRepository extends MongoRepository<Movie,String> {
    @Query("{movieName:{$regex:'^?0', $options:'i'}}")
    List<Movie> findByMovieName(String movieName);

    @Query("{'movieName' : ?0,'theatreName' : ?1}")
    List<Movie> findAvailableTickets(String moviename,String theatreName);

    void deleteByMovieName(String movieName);
}
