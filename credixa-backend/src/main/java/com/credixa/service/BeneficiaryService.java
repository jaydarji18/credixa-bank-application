package com.credixa.service;

import com.credixa.dto.request.AddBeneficiaryRequestDTO;
import com.credixa.dto.response.BeneficiaryResponseDTO;
import com.credixa.entity.Beneficiary;
import com.credixa.entity.User;
import com.credixa.exception.ConflictException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.exception.UnauthorizedException;
import com.credixa.repository.BeneficiaryRepository;
import com.credixa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;

    @Transactional
    public BeneficiaryResponseDTO addBeneficiary(String userCode, AddBeneficiaryRequestDTO request) {
        User user = findUserByCode(userCode);

        // Check duplicate: same user + accountNumber + ifscCode
        beneficiaryRepository.findByUserAndAccountNumberAndIfscCode(user, request.getAccountNumber(), request.getIfscCode())
                .ifPresent(b -> {
                    throw new ConflictException("Beneficiary already exists");
                });

        Beneficiary beneficiary = Beneficiary.builder()
                .user(user)
                .beneficiaryName(request.getBeneficiaryName())
                .accountNumber(request.getAccountNumber())
                .ifscCode(request.getIfscCode())
                .bankName(request.getBankName())
                .nickname(request.getNickname())
                .isVerified(request.getIfscCode().startsWith("CRDX")) // True in demo for CRDX IFSC
                .status(Beneficiary.BeneficiaryStatus.ACTIVE)
                .build();

        Beneficiary savedBeneficiary = beneficiaryRepository.save(beneficiary);

        log.info("Beneficiary added for user {}: {}", userCode, savedBeneficiary.getId());
        return mapToDTO(savedBeneficiary);
    }

    public List<BeneficiaryResponseDTO> getBeneficiaries(String userCode) {
        User user = findUserByCode(userCode);
        return beneficiaryRepository.findByUserAndStatus(user, Beneficiary.BeneficiaryStatus.ACTIVE)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public BeneficiaryResponseDTO getBeneficiaryById(Long id, String userCode) {
        User user = findUserByCode(userCode);
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found or access denied"));

        return mapToDTO(beneficiary);
    }

    @Transactional
    public void deleteBeneficiary(Long id, String userCode) {
        User user = findUserByCode(userCode);
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found or access denied"));

        beneficiary.setStatus(Beneficiary.BeneficiaryStatus.INACTIVE);
        beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary {} soft deleted for user {}", id, userCode);
    }

    private User findUserByCode(String userCode) {
        return userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private BeneficiaryResponseDTO mapToDTO(Beneficiary b) {
        return BeneficiaryResponseDTO.builder()
                .id(b.getId())
                .beneficiaryName(b.getBeneficiaryName())
                .accountNumber(b.getAccountNumber())
                .ifscCode(b.getIfscCode())
                .bankName(b.getBankName())
                .nickname(b.getNickname())
                .isVerified(b.isVerified())
                .status(b.getStatus())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
