package com.credixa.service;

import com.credixa.dto.request.LoanProductRequestDTO;
import com.credixa.dto.response.LoanProductResponseDTO;
import com.credixa.entity.LoanProduct;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanProductService {

    private final LoanProductRepository loanProductRepository;

    public List<LoanProductResponseDTO> getAllLoanProducts() {
        return loanProductRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public LoanProductResponseDTO getLoanProductById(Long id) {
        LoanProduct loanProduct = loanProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan Product not found with id: " + id));
        return mapToDTO(loanProduct);
    }

    @Transactional
    public LoanProductResponseDTO createLoanProduct(LoanProductRequestDTO request) {
        LoanProduct loanProduct = LoanProduct.builder()
                .productCode(request.getProductCode())
                .productName(request.getProductName())
                .loanType(request.getLoanType())
                .interestRate(request.getInterestRate())
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .minTenureMonths(request.getMinTenureMonths())
                .maxTenureMonths(request.getMaxTenureMonths())
                .isActive(request.isActive())
                .build();
        
        return mapToDTO(loanProductRepository.save(loanProduct));
    }

    @Transactional
    public LoanProductResponseDTO updateLoanProduct(Long id, LoanProductRequestDTO request) {
        LoanProduct loanProduct = loanProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan Product not found with id: " + id));
        
        loanProduct.setProductCode(request.getProductCode());
        loanProduct.setProductName(request.getProductName());
        loanProduct.setLoanType(request.getLoanType());
        loanProduct.setInterestRate(request.getInterestRate());
        loanProduct.setMinAmount(request.getMinAmount());
        loanProduct.setMaxAmount(request.getMaxAmount());
        loanProduct.setMinTenureMonths(request.getMinTenureMonths());
        loanProduct.setMaxTenureMonths(request.getMaxTenureMonths());
        loanProduct.setActive(request.isActive());
        
        return mapToDTO(loanProductRepository.save(loanProduct));
    }

    @Transactional
    public void deleteLoanProduct(Long id) {
        if (!loanProductRepository.existsById(id)) {
            throw new ResourceNotFoundException("Loan Product not found with id: " + id);
        }
        loanProductRepository.deleteById(id);
    }

    private LoanProductResponseDTO mapToDTO(LoanProduct lp) {
        return LoanProductResponseDTO.builder()
                .id(lp.getId())
                .productCode(lp.getProductCode())
                .productName(lp.getProductName())
                .loanType(lp.getLoanType())
                .interestRate(lp.getInterestRate())
                .minAmount(lp.getMinAmount())
                .maxAmount(lp.getMaxAmount())
                .minTenureMonths(lp.getMinTenureMonths())
                .maxTenureMonths(lp.getMaxTenureMonths())
                .isActive(lp.isActive())
                .build();
    }
}
