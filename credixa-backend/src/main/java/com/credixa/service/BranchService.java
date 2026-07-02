package com.credixa.service;

import com.credixa.dto.request.BranchRequestDTO;
import com.credixa.dto.response.BranchResponseDTO;
import com.credixa.entity.Bank;
import com.credixa.entity.Branch;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.repository.BankRepository;
import com.credixa.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final BankRepository bankRepository;

    public List<BranchResponseDTO> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public BranchResponseDTO getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
        return mapToDTO(branch);
    }

    @Transactional
    public BranchResponseDTO createBranch(BranchRequestDTO request) {
        Bank bank = bankRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No bank found. Create a bank first."));

        Branch branch = Branch.builder()
                .bank(bank)
                .branchName(request.getBranchName())
                .branchCode(request.getBranchCode())
                .ifscCode(request.getIfscCode())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .phone(request.getPhone())
                .build();
        
        return mapToDTO(branchRepository.save(branch));
    }

    @Transactional
    public BranchResponseDTO updateBranch(Long id, BranchRequestDTO request) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
        
        branch.setBranchName(request.getBranchName());
        branch.setBranchCode(request.getBranchCode());
        branch.setIfscCode(request.getIfscCode());
        branch.setAddress(request.getAddress());
        branch.setCity(request.getCity());
        branch.setState(request.getState());
        branch.setPincode(request.getPincode());
        branch.setPhone(request.getPhone());
        
        return mapToDTO(branchRepository.save(branch));
    }

    @Transactional
    public void deleteBranch(Long id) {
        if (!branchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Branch not found with id: " + id);
        }
        branchRepository.deleteById(id);
    }

    private BranchResponseDTO mapToDTO(Branch branch) {
        return BranchResponseDTO.builder()
                .id(branch.getId())
                .branchName(branch.getBranchName())
                .branchCode(branch.getBranchCode())
                .ifscCode(branch.getIfscCode())
                .address(branch.getAddress())
                .city(branch.getCity())
                .state(branch.getState())
                .pincode(branch.getPincode())
                .phone(branch.getPhone())
                .build();
    }
}
