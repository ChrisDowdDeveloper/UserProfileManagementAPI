package com.christopherdowd.UserProfileManagement.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.christopherdowd.UserProfileManagement.dto.UserProfileRequestDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileResponseDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileUpdateRequestDto;
import com.christopherdowd.UserProfileManagement.service.UserProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {
    
    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserProfileResponseDto> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public UserProfileResponseDto getById(@PathVariable String id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<UserProfileResponseDto> create(@RequestBody @Valid UserProfileRequestDto dto) {
        UserProfileResponseDto acceptedUserDto = service.create(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(acceptedUserDto.getId())
            .toUri();
        
        return ResponseEntity.created(location).body(acceptedUserDto);
    }

    @PutMapping("/{id}")
    public UserProfileResponseDto update(@PathVariable String id, @RequestBody @Valid UserProfileUpdateRequestDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
