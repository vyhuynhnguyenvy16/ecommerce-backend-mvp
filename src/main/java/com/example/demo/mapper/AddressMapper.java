package com.example.demo.mapper;

import com.example.demo.dto.response.AddressResponse;
import com.example.demo.entity.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .addressLine(address.getAddressLine())
                .city(address.getCity())
                .isDefault(address.getIsDefault())
                .build();
    }
}
