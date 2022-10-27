package com.reffians.c2.controller;

import com.reffians.c2.model.Beacon;
import com.reffians.c2.model.Command;
import com.reffians.c2.model.Command.Status;
import com.reffians.c2.model.CommandList;
import com.reffians.c2.model.User;
import com.reffians.c2.service.C2Service;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The REST Controller of the C2 Application, performs routing of all REST API
 * calls to this service. **/
@RestController
public class C2Controller {
  @Autowired
  private C2Service c2Service;
  

  private static final Logger logger = LoggerFactory.getLogger(C2Controller.class);

  /** GET commands for a beacon. Returns 200 OK and an array of command Command
  * objects on success, 400 Bad Request with an error message on failure.

  * @param beaconid A non-negative integer used to identify the beacon.

  * @param status An optional argument specifying the status of the command.
   Can be one of "pending", "sent", "executed", or "finished". If no status is supplied, 
   commands of any status are retrieved.

  * @return A list of command objects. A command object is contains integer
   identifier "id", integer "beaconid" of the corresponding beacon, user-defined
   string "content", and string "status" that is one of "pending", "sent",
   "executed", or "finished".
  **/
  @GetMapping("/beacon/command")
  public ResponseEntity<?> getCommandBeacon(@RequestParam Integer beaconid,
      @RequestParam Optional<String> status) {
    logger.info("GET commands from beacon with beaconid: {}, status: {}",
        beaconid, status.orElse("NULL"));

    if (beaconid < 0) {
      logger.info("GET commands from beacon with negative beaconid: {}", beaconid);
      return responseBadRequest("Invalid beaconid: supplied beaconid is negative.");
    }

    if (!status.isPresent()) {
      return responseOk(c2Service.getCommands(beaconid));
    }

    if (!Command.isValidStatus(status.get())) {
      logger.info("GET commands from beacon with invalid status: {}", status);
      return responseBadRequest("Invalid status.");
    }

    return responseOk(c2Service.getCommands(beaconid, Status.valueOf(status.get())));
  }


  /**
   * POST mapping for the create beacon endpoint.

   * @param username username of the user that 'owns' this beacon
   */
  @PostMapping("/beacon/create")
  public ResponseEntity<?> createBeacon(@RequestParam String username) {
    logger.info("POST create beacon for user with username: {}",
        username);
    List<User> thisUser = c2Service.getUsers(username);
    if (thisUser.size() == 0) {
      logger.info("POST create beacon for non-existent user: {}", username);
      return responseBadRequest("Invalid username: the user does not exist.");
    } else {
      c2Service.createBeacon(username);
      return new ResponseEntity<>("Beacon Created", HttpStatus.OK);
    }
  }

  private static <T> ResponseEntity<?> responseOk(@Nullable T body) {
    return new ResponseEntity<>(body, HttpStatus.OK);
  }

  private static <T> ResponseEntity<?> responseBadRequest(@Nullable T body) {
    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }

  private static <T> ResponseEntity<?> responseCreated(@Nullable T body) {
    return new ResponseEntity<>(body, HttpStatus.CREATED);
  }
  
  /** POST Register User. **/
  @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> registerUser(@RequestBody User user) {
    String username = user.username;
    String password = user.password;
    if (c2Service.checkUser(username)) {
      c2Service.addUser(username, password);
      //return new ResponseEntity<>("Registered", HttpStatus.OK);
      return responseCreated("User created");
    }
    logger.info("Attempted registration for existing user with username: {}", username);
    //return new ResponseEntity<>("User Already Exists", HttpStatus.OK);
    return responseBadRequest("");
  }   

  /** POST Login User. **/
  @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> login(@RequestBody User user) {
    String username = user.username;
    String password = user.password;
    if (c2Service.login(username, password)) {
      return responseOk("logged in");
    } 
    logger.info("Incorrect login information attempt for user: {}", username);
    return responseBadRequest("");
  }


  /**
   * POST Beacon Commands.
   *
   * @param commands : a list of Commands (containing beaconid and content)
   *
   * @return ResponseEntity with HttpStatus
   */
  @PostMapping(path = "/beacon/command", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> submitCommands(@RequestBody CommandList commands) {
    Integer beaconid = null;
    String content = "";

    if (commands.getCommands().isEmpty())  {
      return new ResponseEntity<>("invalid: empty command list", HttpStatus.OK);
    }

    for (Command c : commands.getCommands()) {
      beaconid = c.beaconid;
      content = c.content;
      c2Service.addCommand(beaconid, content, Status.pending.name());
    }
    return new ResponseEntity<>("added commands", HttpStatus.OK); 
  }
}
