package com.christopherdowd.UserProfileManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreationMessageDto {
    private String id;
    private String username;
    private String email;
    private String encryptedSocialSecurityNumber;
}
