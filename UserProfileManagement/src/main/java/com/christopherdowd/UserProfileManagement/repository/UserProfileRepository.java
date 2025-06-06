package com.christopherdowd.UserProfileManagement.repository;

import org.springframework.stereotype.Repository;

import com.christopherdowd.UserProfileManagement.domain.UserProfile;
import com.google.cloud.spring.data.datastore.repository.DatastoreRepository;

@Repository
public interface UserProfileRepository 
    extends DatastoreRepository<UserProfile, String> {
}
