package com.fse1.movieapp.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.fse1.movieapp.models.Ticket;

import java.util.List;

public interface TicketRepository extends MongoRepository<Ticket,String> {
    @Query(value = "{'movieName' : ?0,'theatreName' : ?1}", fields = "{_id:0, seatNumber:1}")
    List<Ticket> findSeats(String movieName, String theatreName);

    List<Ticket> findByMovieName(String movieName);

    List<Ticket> findBy_id(ObjectId _id);
}
