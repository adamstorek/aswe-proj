package com.reffians.c2.controller;

import com.reffians.c2.dto.UserRequestDTO;
import com.reffians.c2.model.Beacon;
import com.reffians.c2.model.Command;
import com.reffians.c2.model.Command.Status;
import com.reffians.c2.model.User;
import com.reffians.c2.service.BeaconService;
import com.reffians.c2.service.CommandService;
import com.reffians.c2.service.UserService;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
  private BeaconService beaconService;
  @Autowired
  private UserService userService;
  @Autowired
  private CommandService commandService;
  private static final Logger logger = LoggerFactory.getLogger(C2Controller.class);

  /** GET commands for a beacon. Returns 200 OK and an array of command Command
    * objects on success, 400 Bad Request with an error message on failure.
    *
    * @param beaconid A non-negative integer used to identify the beacon.
    * @param status An optional argument specifying the status of the command. Can
    *     be one of "pending", "sent", "executed", or "finished". If no status is
    *     supplied, commands of any status are retrieved.
    * @return A list of command objects. A command object contains integer identifier
    *     "id", integer "beaconid" of the corresponding beacon, user-defined string
    *      "content", and string "status" that is one of "pending", "sent", "executed",
    *      or "finished".
    */
  @GetMapping("/beacon/command")
  public ResponseEntity<?> getCommandBeacon(@RequestParam Integer beaconid,
      @RequestParam Optional<String> status) {
    logger.info("GET commands from beacon with beaconid: {}, status: {}",
        beaconid, status.orElse("NULL"));
    if (beaconid < 0) {
      logger.warn("GET commands from beacon with negative beaconid: {}", beaconid);
      return ResponseEntity.badRequest().body("Invalid beaconid: supplied beaconid is negative.");
    }
    if (status.isPresent() && !Status.isValid(status.get())) {
      logger.warn("GET commands from beacon with invalid status: {}", status);
      return ResponseEntity.badRequest().body("Invalid status.");
    }
    List<Command> commands;
    if (status.isPresent()) {
      commands = commandService.getCommands(beaconid, Status.valueOf(status.get()));
    } else {
      commands = commandService.getCommands(beaconid);
    }
    commandService.updateCommandStatus(commands, Status.sent);
    return ResponseEntity.ok(commands);
  }

  /**
   * POST mapping for the register beacon endpoint.
   */
  @PostMapping(path = "/beacon/register", consumes = MediaType.APPLICATION_JSON_VALUE, 
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> registerBeacon(@RequestBody Beacon beacon) {
    String username = beacon.username;
    logger.info("POST register beacon for user with username: {}",
        username);
    List<User> thisUser = userService.getUsers(username);
    if (thisUser.size() == 0) {
      logger.error("POST register beacon for non-existent user: {}", username);
      return ResponseEntity.badRequest().body("Invalid username: the user does not exist.");
    }
    beaconService.registerBeacon(username);
    Date date = new Date();
    Timestamp t = new Timestamp(date.getTime());
    logger.info("Beacon registered at " + t + " for user: {}",
        username);

    JSONObject obj = new JSONObject();
    obj.put("timestamp", t);
    obj.put("status", 200);
    obj.put("path", "/beacon/register");
    obj.put("username", username);
    obj.put("beacon_id", "beacon_id");
    String bodyToReturn = obj.toString();
    return ResponseEntity.ok(bodyToReturn);
    
  }

  /** 
   * POST mapping to register a new user.

   * @param user is contains two strings:
   *     username is a non null non empty string
   *     password is a non null non emtpry plaintext password
   */
  @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> registerUser(@RequestBody UserRequestDTO user) {
    if (user.getUsername() == null || user.getPassword() == null) {
      logger.warn("Registration request missing username or password field"); 
      return ResponseEntity.badRequest()
        .body("Registration request missing username or password field");
    } 

    if (user.getUsername().isEmpty() || user.getPassword().isEmpty()) {
      logger.warn("Attempted user creation with empty username or password");
      return ResponseEntity.badRequest()
        .body("Attempted user creation with empty username or password"); 
    }

    if (!userService.userExists(user.getUsername())) {
      userService.addUser(user.getUsername(), user.getPassword());
      logger.info("New user created: {}", user.getUsername());
      return ResponseEntity.ok("User created");
    }

    logger.warn("Attempted registration for existing user with username: {}", user.getUsername());
    return ResponseEntity.badRequest().body("Attempted registration for existing user"); //
  }   

  /** 
   * POST mapping for login. 

   * @param user is contains two strings:
   *     username is a non null non empty string
   *     password is a non null non emtpry plaintext password
   */
  @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> login(@RequestBody UserRequestDTO user) {
    if (user.getUsername() == null || user.getPassword() == null) {
      logger.warn("Login request missing username or password field");
      return ResponseEntity.badRequest().body("");
    }

    if (user.getUsername().isEmpty() || user.getPassword().isEmpty()) {
      logger.warn("Attempted user creation with empty username or password");
      return ResponseEntity.badRequest()
        .body("Attempted user creation with empty username or password"); 
    }

    if (userService.compareHash(user.getUsername(), user.getPassword())) {
      logger.info("User login for {}", user.getUsername());
      return ResponseEntity.ok("logged in");
    } 

    logger.warn("Incorrect login information attempt for user: {}", user.getUsername());
    return ResponseEntity.badRequest().body("Incorrect login");
  }

  /**
    * POST User Commands. Returns 300 Created on success, and 400 Bad Request
    * on failure.
    *
    * @param beaconid a non-negative integer representing the beaconid.
    * @param commandContents a list of strings representing the command contents.
    * @return ResponseEntity with HttpStatus
    */
  @PostMapping(path = "/user/command", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> submitCommands(@RequestParam Integer beaconid,
      @RequestBody List<String> commandContents) {
    logger.info("POST commands to beacon with beaconid: {}, commandContents: {}",
        beaconid, commandContents);
    if (beaconid < 0) {
      logger.info("POST commands to beacon with negative beaconid: {}", beaconid);
      return ResponseEntity.badRequest().body("Invalid beaconid: supplied beaconid is negative.");
    }

    if (commandContents.isEmpty())  {
      logger.info("POST commands to beacon with empty command contents list.");
      return ResponseEntity.badRequest().body("Invalid: empty command contents list.");
    }

    ArrayList<Command> addedCommands = new ArrayList<Command>();
    for (String content : commandContents) {
      addedCommands.add(commandService.addCommand(beaconid, content));
    }
    return ResponseEntity.ok(addedCommands); 
  }
}
