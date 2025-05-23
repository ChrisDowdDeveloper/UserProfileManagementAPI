package com.christopherdowd.UserProfileManagement.domain;

import org.springframework.data.annotation.Id;

import com.google.cloud.spring.data.datastore.core.mapping.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name="users")
public class UserProfile { 

    @Id
    private String id;

    private String username;

    private String email;

    private String encryptedSocialSecurityNumber;

}