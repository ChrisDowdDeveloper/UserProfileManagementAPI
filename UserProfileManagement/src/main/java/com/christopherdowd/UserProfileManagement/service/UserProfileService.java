package com.christopherdowd.UserProfileManagement.service;

import java.util.List;

import com.christopherdowd.UserProfileManagement.dto.UserProfileRequestDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileResponseDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileUpdateRequestDto;

public interface UserProfileService {
    UserProfileResponseDto getById(String id);
    List<UserProfileResponseDto> getAll();
    UserProfileResponseDto create(UserProfileRequestDto dto);
    UserProfileResponseDto update(String id, UserProfileUpdateRequestDto dto);
    void delete(String id);
}
