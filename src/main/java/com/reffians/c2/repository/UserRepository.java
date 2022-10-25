package com.reffians.c2.repository;

<<<<<<< HEAD
import com.reffians.c2.model.Beacon;
import com.reffians.c2.model.Command;
=======
>>>>>>> fc7ad0f (refactored login and register code to follow best code practices. Return different status codes if login or register unsuccessful. Code is checkstyle CLEAN (w/o warns))
import com.reffians.c2.model.User;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


/** User Repository. Queries the User table in the database. **/
public interface UserRepository extends CrudRepository<User, String> {
  /** findByUsername
   * Find username and password given a username.
   **/
  @Query(value = "select * from users where username = :username", nativeQuery = true)
  List<User> findByUsername(@Param("username") String username);

  /** findByUnamePword
   * Check if username and password match an entry in the database.
   **/
  @Query(value = "select * from users where username = :username and password = :password", 
      nativeQuery = true)
  List<User> findByUnamePword(@Param("username") String username, 
      @Param("password") String password);

  /** insertUser
   * Add new user to the database with a given username and password.
   **/
  @Modifying
  @Transactional
  @Query(value = "insert into users (username, password) values (:username, :password)",
      nativeQuery = true)
  void insertUser(@Param("username") String username, @Param("password") String password);
}