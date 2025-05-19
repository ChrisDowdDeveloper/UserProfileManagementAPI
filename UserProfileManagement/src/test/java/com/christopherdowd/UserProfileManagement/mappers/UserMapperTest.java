package com.christopherdowd.UserProfileManagement.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.christopherdowd.UserProfileManagement.TestDataUtil;
import com.christopherdowd.UserProfileManagement.domain.UserProfile;
import com.christopherdowd.UserProfileManagement.dto.UserProfileRequestDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileResponseDto;
import com.christopherdowd.UserProfileManagement.dto.UserProfileUpdateRequestDto;
import com.christopherdowd.UserProfileManagement.mapper.UserProfileMapper;

public class UserMapperTest {
    private final UserProfileMapper mapper = new UserProfileMapper();

    @Test
    void toEntityFromRequestDto() {
        UserProfileRequestDto dto = TestDataUtil.createAliceRequestDto();
        UserProfile entity = mapper.toUser(dto);
        assertNull(entity.getId());
        assertEquals(dto.getUsername(), entity.getUsername());
        assertEquals(dto.getEmail(), entity.getEmail());
        assertNull(entity.getEncryptedSocialSecurityNumber());
    }

    @Test
    void toResponseDtoFromEntityExcludesSSN() {
        UserProfile entity = TestDataUtil.createAliceEntity();
        UserProfileResponseDto res = mapper.toUserResponseDto(entity);

        assertEquals(entity.getId(), res.getId());
        assertEquals(entity.getUsername(), res.getUsername());
        assertEquals(entity.getEmail(), res.getEmail());
    }

    @Test
    void updateEntityFromUpdateDtoOnlyUpdatesAllowedFields() {
        // given
        UserProfile original = TestDataUtil.createAliceEntity();
        // use the UpdateRequestDto, not the CreateRequestDto:
        UserProfileUpdateRequestDto updateDto = UserProfileUpdateRequestDto.builder()
            .username("NewName")
            .email("new@example.com")
            .build();

        // when
        mapper.updateUserFromDto(updateDto, original);

        // then only username & email changed; SSN stayed the same
        assertEquals("NewName",            original.getUsername());
        assertEquals("new@example.com",    original.getEmail());
        assertEquals(TestDataUtil.USER_1_SSN_ENCRYPTED,
                    original.getEncryptedSocialSecurityNumber());
    }

}
