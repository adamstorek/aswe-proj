package com.reffians.c2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reffians.c2.controller.C2Controller;
import com.reffians.c2.model.commands.Command;
import com.reffians.c2.service.BeaconService;
import com.reffians.c2.service.CommandService;
import com.reffians.c2.service.ResultService;
import com.reffians.c2.service.UserService;
import java.util.Collections;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest
@AutoConfigureMockMvc

@ContextConfiguration(classes = {IntegrationTestConfig.class})

class C2IntegrationTest {
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private BeaconService beaconService;
  @Autowired
  private UserService userService;
  @Autowired
  private CommandService commandService;
  @Autowired
  private ResultService resultService;
  @Autowired
  C2Controller c2Controller;

  @BeforeEach
  public void init()
  {
    mockMvc = MockMvcBuilders.standaloneSetup (c2Controller).build();
  }

  @Test
  void testReceiveCommandBeacon() throws Exception {
    Integer beaconid = 1;
    if(beaconService.beaconExists(beaconid, "token") == true){
        mockMvc.perform(post("/beacon/command")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"beacon\": {\"id\": 1, \"token\": \"token\"},\"status\": \"all\"}")
            .queryParam("beaconid", "5"))
            .andExpect(status().isOk());
    }
    else{
        mockMvc.perform(post("/beacon/command")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"beacon\": {\"id\": 1, \"token\": \"token\"}}"))
            .andExpect(status().isBadRequest());
    }
  }

  @Test
  void testLogin() throws Exception {
    JSONObject obj = new JSONObject();
    obj.put("username", "Nikhil4");
    obj.put("password", "pword");
    String testUser = obj.toString();
    mockMvc.perform(MockMvcRequestBuilders.post("/register")
        .contentType(MediaType.APPLICATION_JSON).content(testUser))
        .andExpect(status().isOk());
    mockMvc.perform(MockMvcRequestBuilders.post("/login")
        .contentType(MediaType.APPLICATION_JSON).content(testUser))
        .andExpect(status().isOk());
  }


}