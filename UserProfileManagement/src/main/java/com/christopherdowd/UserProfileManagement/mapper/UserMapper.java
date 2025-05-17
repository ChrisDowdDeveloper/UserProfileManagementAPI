package com.christopherdowd.UserProfileManagement.mapper;

import org.springframework.stereotype.Component;

import com.christopherdowd.UserProfileManagement.domain.User;
import com.christopherdowd.UserProfileManagement.dto.UserRequestDto;
import com.christopherdowd.UserProfileManagement.dto.UserResponseDto;
import com.christopherdowd.UserProfileManagement.dto.UserUpdateRequestDto;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponseDto toUserResponseDto(User user) {
        if (user == null) {
            return null;
        }
        UserResponseDto dto = new UserResponseDto();

        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        
        return dto;
    }

    public List<UserResponseDto> toUserResponseDtoList(List<User> users) {
        if (users == null) {
            return null;
        }
        return users.stream()
                .map(this::toUserResponseDto)
                .collect(Collectors.toList());
    }

    public User toUser(UserRequestDto dto) {
        if (dto == null) {
            return null;
        }
        
        User u = new User();

        u.setUsername(dto.getUsername());
        u.setEmail(dto.getEmail());

        return u;
    }

    public void updateUserFromDto(UserUpdateRequestDto userUpdateRequestDto, User user) {
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