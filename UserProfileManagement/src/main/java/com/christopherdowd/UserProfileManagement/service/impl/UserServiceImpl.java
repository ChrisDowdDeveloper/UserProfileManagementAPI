package com.christopherdowd.UserProfileManagement.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.christopherdowd.UserProfileManagement.dto.UserRequestDto;
import com.christopherdowd.UserProfileManagement.dto.UserResponseDto;
import com.christopherdowd.UserProfileManagement.dto.UserUpdateRequestDto;
import com.christopherdowd.UserProfileManagement.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    @Override
    public UserResponseDto getById(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getById'");
    }

    @Override
    public List<UserResponseDto> getAll() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAll'");
    }

    @Override
    public UserResponseDto create(UserRequestDto dto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'create'");
    }

    @Override
    public void delete(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public UserResponseDto update(String id, UserUpdateRequestDto dto) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'update'");
    }

}
