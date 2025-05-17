package com.christopherdowd.UserProfileManagement.domain;

import org.springframework.data.annotation.Id;

import com.google.auto.value.AutoValue.Builder;
import com.google.cloud.spring.data.datastore.core.mapping.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name="users")
public class User { 

    @Id
    private Long id;

    private String username;

    private String email;
    
    private String encryptedSocialSecurityNumber;

}