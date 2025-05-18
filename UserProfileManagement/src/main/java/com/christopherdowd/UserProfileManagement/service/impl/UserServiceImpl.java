package com.christopherdowd.UserProfileManagement.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.christopherdowd.UserProfileManagement.domain.User;
import com.christopherdowd.UserProfileManagement.dto.UserRequestDto;
import com.christopherdowd.UserProfileManagement.dto.UserResponseDto;
import com.christopherdowd.UserProfileManagement.dto.UserUpdateRequestDto;
import com.christopherdowd.UserProfileManagement.mapper.UserMapper;
import com.christopherdowd.UserProfileManagement.repository.UserRepository;
import com.christopherdowd.UserProfileManagement.service.EncryptionService;
import com.christopherdowd.UserProfileManagement.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final EncryptionService crypto;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper mapper,
            EncryptionService crypto) {
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.crypto = crypto;
    }

    @Override
    @Cacheable("profiles")
    public UserResponseDto getById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + id
                    )
                );
        return mapper.toUserResponseDto(user);
    }

    @Override
    public List<UserResponseDto> getAll() {
        Iterable<User> iterable = userRepository.findAll();
        List<User> users = new ArrayList<>();
        iterable.forEach(users::add);

        return mapper.toUserResponseDtoList(users);
    }

    @Override
    public UserResponseDto create(UserRequestDto dto) {
        String encrypted = crypto.encrypt(dto.getSocialSecurityNumber());

        User user = mapper.toUser(dto);
        user.setId(UUID.randomUUID().toString());
        user.setEncryptedSocialSecurityNumber(encrypted);

        userRepository.save(user);

        return mapper.toUserResponseDto(user);
    }
    @Override
    @CacheEvict(value = "profiles", key = "#id")
    public UserResponseDto update(String id, UserUpdateRequestDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found: " + id
                    )
                );

        mapper.updateUserFromDto(dto, user);
        userRepository.save(user);

        return mapper.toUserResponseDto(user);
    }

    @Override
    @CacheEvict(value = "profiles", key = "#id")
    public void delete(String id) {
        userRepository.deleteById(id);
    }
}
