package com.christopherdowd.UserProfileManagement.controllers;

import com.christopherdowd.UserProfileManagement.controller.UserProfileController;
import com.christopherdowd.UserProfileManagement.TestDataUtil;
import com.christopherdowd.UserProfileManagement.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerStandaloneTest {

    @Mock
    private UserProfileService userService;

    @InjectMocks
    private UserProfileController controller;

    private MockMvc mvc;
    private ObjectMapper om;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
        om = new ObjectMapper();
    }

    @Test
    void testThatCreateUserSuccessfullyReturnsHttp201AndBody() throws Exception {
        var req = TestDataUtil.createAliceRequestDto();
        var res = TestDataUtil.createAliceResponseDto();

        when(userService.create(any())).thenReturn(res);

        mvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").value(TestDataUtil.USER_1_ID))
           .andExpect(jsonPath("$.username").value(TestDataUtil.USER_1_USERNAME))
           .andExpect(jsonPath("$.email").value(TestDataUtil.USER_1_EMAIL))
           .andExpect(jsonPath("$.encryptedSocialSecurityNumber").doesNotExist());
    }

    @Test
    void createUser_missingUsername_returns400() throws Exception {
        var invalid = TestDataUtil.createAliceRequestDto();
        invalid.setUsername(null);

        mvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(invalid)))
           .andExpect(status().isBadRequest());
    }
}
