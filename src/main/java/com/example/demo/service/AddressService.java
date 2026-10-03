package com.example.demo.service;

import com.example.demo.dto.request.AddressRequest;
import com.example.demo.dto.response.AddressResponse;
import com.example.demo.entity.Address;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.AddressMapper;
import com.example.demo.repository.AddressRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    public List<AddressResponse> getMyAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.isDefault()) {
            clearDefaultFlag(userId);
        }

        Address address = Address.builder()
                .user(user)
                .recipientName(request.getRecipientName())
                .phone(request.getPhone())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .isDefault(request.isDefault())
                .build();

        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to access this address");
        }

        if (request.isDefault() && !address.getIsDefault()) {
            clearDefaultFlag(userId);
        }

        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setAddressLine(request.getAddressLine());
        address.setCity(request.getCity());
        address.setIsDefault(request.isDefault());

        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to access this address");
        }

        addressRepository.delete(address);
    }

    private void clearDefaultFlag(Long userId) {
        List<Address> existing = addressRepository.findByUserId(userId);
        for (Address address : existing) {
            if (Boolean.TRUE.equals(address.getIsDefault())) {
                address.setIsDefault(false);
                addressRepository.save(address);
            }
        }
    }
}
