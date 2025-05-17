package com.christopherdowd.UserProfileManagement.service;

import java.util.List;

import com.christopherdowd.UserProfileManagement.dto.UserRequestDto;
import com.christopherdowd.UserProfileManagement.dto.UserResponseDto;
import com.christopherdowd.UserProfileManagement.dto.UserUpdateRequestDto;

public interface UserService {
    UserResponseDto getById(String id);
    List<UserResponseDto> getAll();
    UserResponseDto create(UserRequestDto dto);
    UserResponseDto update(String id, UserUpdateRequestDto dto);
    void delete(String id);
}
