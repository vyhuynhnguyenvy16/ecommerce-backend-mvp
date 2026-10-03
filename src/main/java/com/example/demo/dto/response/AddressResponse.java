package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private String recipientName;
    private String phone;
    private String addressLine;
    private String city;
    private boolean isDefault;
}
