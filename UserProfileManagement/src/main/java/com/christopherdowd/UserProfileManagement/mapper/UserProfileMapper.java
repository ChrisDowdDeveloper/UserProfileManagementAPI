package com.christopherdowd.UserProfileManagement.mapper;

import org.springframework.stereotype.Component;

import com.christopherdowd.UserProfileManagement.domain.UserProfile;
import com.christopherdowd.UserProfileManagement.dto.UserProfileRequestDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileResponseDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileUpdateRequestDto;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserProfileMapper {

    public UserProfileResponseDto toUserResponseDto(UserProfile user) {
        if (user == null) {
            return null;
        }
        UserProfileResponseDto dto = new UserProfileResponseDto();

        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        
        return dto;
    }

    public List<UserProfileResponseDto> toUserResponseDtoList(List<UserProfile> users) {
        if (users == null) {
            return null;
        }
        return users.stream()
                .map(this::toUserResponseDto)
                .collect(Collectors.toList());
    }

    public UserProfile toUser(UserProfileRequestDto dto) {
        if (dto == null) {
            return null;
        }
        
        UserProfile u = new UserProfile();

        u.setUsername(dto.getUsername());
        u.setEmail(dto.getEmail());

        return u;
    }

    public void updateUserFromDto(UserProfileUpdateRequestDto userUpdateRequestDto, UserProfile user) {
        if (userUpdateRequestDto == null || user == null) {
            return;
        }
        if (userUpdateRequestDto.getUsername() != null) {
            user.setUsername(userUpdateRequestDto.getUsername());
        }
        if (userUpdateRequestDto.getEmail() != null) {
            user.setEmail(userUpdateRequestDto.getEmail());
        }
    }
}