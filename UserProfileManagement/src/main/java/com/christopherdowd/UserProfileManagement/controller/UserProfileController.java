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

/**
 * REST Controller for managing user profiles
 */
@RestController
@RequestMapping("/api/users")
public class UserProfileController {
    
    private final UserProfileService service;

    /*
     * Constructor for UserProfile Controller
     * @param service for UserProfileService to be injected
     */
    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    /*!
     * Retrieves a list of all user profiles
     * @return a list of UserProfileResponseDto
     */
    @GetMapping
    public List<UserProfileResponseDto> getAll() {
        return service.getAll();
    }

    /* 
     * Retrieves a single user profile by its ID
     * @param {id} the ID of the user profile to retrieve
     * @return the UserProfileResponseDto if found, if not, throw exception
    */
    @GetMapping("/{id}")
    public UserProfileResponseDto getById(@PathVariable String id) {
        return service.getById(id);
    }

    /* 
     * Creates a new user profile
     * Request body is validated. When succesfully created,
     * the user SSN is encrypted and creation event is published to Pub/Sub for async persistance
     * Returns HTTP 201 with location of resource and user DTO.
     * @param dto: UserProfileRequestDto containing data for new user
     * @return a response entity containing the created UserProfileResponseDto
    */
    @PostMapping
    public ResponseEntity<UserProfileResponseDto> create(@RequestBody @Valid UserProfileRequestDto dto) {
        UserProfileResponseDto acceptedUserDto = service.create(dto);

        // Builds the location URI for the new created resource
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(acceptedUserDto.getId())
            .toUri();
        
        return ResponseEntity.created(location).body(acceptedUserDto);
    }

    /* 
     * Updates existing user profile by its ID
     * @param {id} the id of the profile to update
     * @param dto: the UserProfileUpdateRequestDto with the updated data
     * #return the updated UserProfileResponseDto
    */
    @PutMapping("/{id}")
    public UserProfileResponseDto update(@PathVariable String id, @RequestBody @Valid UserProfileUpdateRequestDto dto) {
        return service.update(id, dto);
    }

    /*
     * Deletes user by ID
     * @param {id} the ID of user profile to delete
     * @return a response entity with no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
