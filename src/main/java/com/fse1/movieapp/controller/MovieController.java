package com.fse1.movieapp.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.fse1.movieapp.exception.MoviesNotFound;
import com.fse1.movieapp.exception.SeatAlreadyBooked;
import com.fse1.movieapp.models.Movie;
import com.fse1.movieapp.models.Ticket;
import com.fse1.movieapp.models.User;
import com.fse1.movieapp.payload.request.LoginRequest;
import com.fse1.movieapp.payload.response.MessageResponse;
import com.fse1.movieapp.repository.MovieRepository;
import com.fse1.movieapp.repository.TicketRepository;
import com.fse1.movieapp.repository.UserRepository;
import com.fse1.movieapp.security.services.MovieService;
import com.fse1.movieapp.security.services.UserDetailsImpl;
import com.fse1.movieapp.security.services.UserDetailsServiceImpl;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1.0/moviebooking")
@OpenAPIDefinition(
        info = @Info(
                title = "Movie Application API",
                description = "This API provides endpoints for managing movies."
        )
)
@Slf4j
@CrossOrigin("*")
public class MovieController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    private MovieService movieService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private NewTopic topic;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private MovieRepository movieRepository;


    @PutMapping("/{loginId}/forgot")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "reset password")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> changePassword(@Valid @RequestBody LoginRequest loginRequest, @PathVariable String loginId){
        log.debug("forgot password endopoint accessed by "+loginRequest.getLoginId());
        Optional<User> user1 = userRepository.findByLoginId(loginId);
            User availableUser = user1.get();
            User updatedUser = new User(
                            loginId,
                    availableUser.getFirstName(),
                    availableUser.getLastName(),
                    availableUser.getEmail(),
                    availableUser.getContactNumber(),
                    passwordEncoder.encode(loginRequest.getPassword())
                    );
            updatedUser.set_id(availableUser.get_id());
            updatedUser.setRoles(availableUser.getRoles());
            userRepository.save(updatedUser);
            log.debug(loginRequest.getLoginId()+" has password changed successfully");
            return new ResponseEntity<>(new MessageResponse("Users password changed successfully", "Success"),HttpStatus.OK);
    }
    @PostMapping("/addmovie")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Add new Movie (Admin Only)")
    public ResponseEntity<?> addMovie(@Valid @RequestBody Movie movie){
    	Movie savedMovie = movieService.saveMovie(movie);
    	return new ResponseEntity<>(new MessageResponse("Movie-"+savedMovie.getMovieName()+" has been added successfully","Success"),HttpStatus.OK);
    }

    @GetMapping("/all")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "search all movies")
    public ResponseEntity<List<Movie>> getAllMovies(){
        log.debug("here u can access all the available movies");
        List<Movie> movieList = movieService.getAllMovies();
        if(movieList.isEmpty()){
            log.debug("currently no movies are available");
            throw new MoviesNotFound("No Movies are available");
        }
        else{
            log.debug("listed the available movies");
            return new ResponseEntity<>(movieList, HttpStatus.FOUND);
        }
    }

    @GetMapping("/movies/search/{movieName}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "search movies by movie name")
    public ResponseEntity<List<Movie>> getMovieByName(@PathVariable String movieName){
        log.debug("here search a movie by its name");
        List<Movie> movieList = movieService.getMovieByName(movieName);
        if(movieList.isEmpty()){
            log.debug("currently no movies are available");
//            throw new MoviesNotFound("Movies Not Found");
            return new ResponseEntity<>(movieList,HttpStatus.OK);
        }
        else
            log.debug("listed the available movies with title:"+movieName);
            return new ResponseEntity<>(movieList,HttpStatus.OK);
    }

    @PostMapping("/{movieName}/book")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "book ticket")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> bookTickets(@RequestBody Ticket ticket, @PathVariable String movieName) {
        log.debug(ticket.getLoginId()+" entered to book tickets");
        List<Ticket> allTickets = movieService.findSeats(movieName,ticket.getTheatreName());
        for(Ticket each : allTickets){
            for(int i = 0; i < ticket.getNoOfTickets(); i++){
                if(each.getSeatNumber().contains(ticket.getSeatNumber().get(i))){
                    log.debug("seat is already booked");
                    throw new SeatAlreadyBooked("Seat number "+ticket.getSeatNumber().get(i)+" is already booked");
                }
            }
        }

        if(movieService.findAvailableTickets(movieName,ticket.getTheatreName()).get(0).getNoOfTicketsAvailable() >=
                ticket.getNoOfTickets()){

            log.info("available tickets "
                    +movieService.findAvailableTickets(movieName,ticket.getTheatreName()).get(0).getNoOfTicketsAvailable());
            movieService.saveTicket(ticket);
            log.debug(ticket.getLoginId()+" booked "+ticket.getNoOfTickets()+" tickets");
            kafkaTemplate.send(topic.name(),"Movie ticket booked. " +
                    "Booking Details are: "+
            ticket);
          updateAvailableTickectsInMovie(movieName,ticket.getTheatreName(),ticket.getNoOfTickets());
          
            return new ResponseEntity<>(new MessageResponse("Tickets Booked Successfully with seat numbers"+ticket.getSeatNumber(),"Success"),HttpStatus.OK);
        }
        else{
            log.debug("tickets sold out");
            return new ResponseEntity<>(new MessageResponse("All tickets sold out","Error"),HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    @GetMapping("/getallbookedtickets/{movieName}")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "get all booked tickets(Admin Only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Ticket>> getAllBookedTickets(@PathVariable String movieName){
        return new ResponseEntity<>(movieService.getAllBookedTickets(movieName),HttpStatus.OK);
    }

    @PutMapping("/{movieName}/update")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "get all booked tickets(Admin Only)")
    public ResponseEntity<String> updateTicketStatus(@PathVariable String movieName) {
        List<Movie> movie = movieRepository.findByMovieName(movieName);
        
        if (movie == null) {
            throw new MoviesNotFound("Movie not found: " + movieName);
        }

       
        for (Movie movies : movie) {
            if (movies.getNoOfTicketsAvailable() == 0) {
                movies.setTicketsStatus("SOLD OUT");
            } else {
                movies.setTicketsStatus("BOOK ASAP");
            }
            movieService.saveMovie(movies);
        }
        return new ResponseEntity<>("Ticket status updated successfully", HttpStatus.OK);

    }


    @DeleteMapping("/{movieName}/delete")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "delete a movie(Admin Only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteMovie(@PathVariable String movieName){
        List<Movie> availableMovies = movieService.findByMovieName(movieName);
        if(availableMovies.isEmpty()){
            throw new MoviesNotFound("No movies Available with moviename "+ movieName);
        }
        else {
            movieService.deleteByMovieName(movieName);
            kafkaTemplate.send(topic.name(),"Movie Deleted by the Admin. "+movieName+" is now not available");
            return new ResponseEntity<>(new MessageResponse("Movie "+movieName+"Movie deleted successfully", "Success") ,HttpStatus.OK);
        }

    }


    private void updateAvailableTickectsInMovie(String moviename,String theatreName,Integer noOfTickets) {
        ObjectId objectId = movieService.findAvailableTickets(moviename,theatreName).get(0).get_id();
        Movie movie = new Movie(
                objectId,
                moviename,
                theatreName,
                movieService.findAvailableTickets(moviename,theatreName).get(0).getNoOfTicketsAvailable() - noOfTickets
        );
        movieService.saveMovie(movie);
    }

}
