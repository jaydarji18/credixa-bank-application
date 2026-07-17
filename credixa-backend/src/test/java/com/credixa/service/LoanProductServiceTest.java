package com.credixa.service;

import com.credixa.dto.request.LoanProductRequestDTO;
import com.credixa.dto.response.LoanProductResponseDTO;
import com.credixa.entity.LoanProduct;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.repository.LoanProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class LoanProductServiceTest {

    @Mock
    private LoanProductRepository loanProductRepository;

    @InjectMocks
    private LoanProductService loanProductService;

    @Nested
    @DisplayName("getAllLoanProducts() tests")
    class GetAllLoanProductsTests {

        @Test
        @DisplayName("Should return all loan products successfully")
        void shouldReturnAllLoanProducts() {
            LoanProduct product1 = LoanProduct.builder()
                    .id(1L)
                    .productCode("LP001")
                    .productName("Personal Loan")
                    .build();

            LoanProduct product2 = LoanProduct.builder()
                    .id(2L)
                    .productCode("LP002")
                    .productName("Home Loan")
                    .build();

            given(loanProductRepository.findAll()).willReturn(List.of(product1, product2));

            List<LoanProductResponseDTO> response = loanProductService.getAllLoanProducts();

            assertThat(response).hasSize(2);
            verify(loanProductRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no loan products exist")
        void shouldReturnEmptyListWhenNoProducts() {
            given(loanProductRepository.findAll()).willReturn(List.of());

            List<LoanProductResponseDTO> response = loanProductService.getAllLoanProducts();

            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("getLoanProductById() tests")
    class GetLoanProductByIdTests {

        @Test
        @DisplayName("Should return loan product successfully")
        void shouldReturnLoanProduct() {
            LoanProduct product = LoanProduct.builder()
                    .id(1L)
                    .productCode("LP001")
                    .productName("Personal Loan")
                    .interestRate(new BigDecimal("12.5"))
                    .minAmount(new BigDecimal("50000"))
                    .maxAmount(new BigDecimal("500000"))
                    .build();

            given(loanProductRepository.findById(1L)).willReturn(Optional.of(product));

            LoanProductResponseDTO response = loanProductService.getLoanProductById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getProductCode()).isEqualTo("LP001");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when loan product not found")
        void shouldThrowExceptionWhenProductNotFound() {
            given(loanProductRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> loanProductService.getLoanProductById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createLoanProduct() tests")
    class CreateLoanProductTests {

        @Test
        @DisplayName("Should create loan product successfully")
        void shouldCreateLoanProduct() {
            LoanProductRequestDTO request = LoanProductRequestDTO.builder()
                    .productCode("LP003")
                    .productName("Business Loan")
                    .loanType(LoanProduct.LoanType.BUSINESS_LOAN)
                    .interestRate(new BigDecimal("14.0"))
                    .minAmount(new BigDecimal("100000"))
                    .maxAmount(new BigDecimal("5000000"))
                    .minTenureMonths(12)
                    .maxTenureMonths(120)
                    .isActive(true)
                    .build();

            given(loanProductRepository.save(any(LoanProduct.class))).willAnswer(invocation -> {
                LoanProduct p = invocation.getArgument(0);
                p.setId(1L);
                return p;
            });

            LoanProductResponseDTO response = loanProductService.createLoanProduct(request);

            assertThat(response).isNotNull();
            verify(loanProductRepository).save(any(LoanProduct.class));
        }
    }

    @Nested
    @DisplayName("updateLoanProduct() tests")
    class UpdateLoanProductTests {

        @Test
        @DisplayName("Should update loan product successfully")
        void shouldUpdateLoanProduct() {
            LoanProduct existingProduct = LoanProduct.builder()
                    .id(1L)
                    .productCode("LP001")
                    .productName("Personal Loan")
                    .build();

            LoanProductRequestDTO request = LoanProductRequestDTO.builder()
                    .productCode("LP001")
                    .productName("Updated Personal Loan")
                    .build();

            given(loanProductRepository.findById(1L)).willReturn(Optional.of(existingProduct));
            given(loanProductRepository.save(any(LoanProduct.class))).willAnswer(invocation -> invocation.getArgument(0));

            LoanProductResponseDTO response = loanProductService.updateLoanProduct(1L, request);

            assertThat(response).isNotNull();
            verify(loanProductRepository).save(argThat(p -> 
                    p.getProductName().equals("Updated Personal Loan")
            ));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent product")
        void shouldThrowExceptionWhenUpdatingNonExistent() {
            LoanProductRequestDTO request = LoanProductRequestDTO.builder()
                    .productName("Updated")
                    .build();

            given(loanProductRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> loanProductService.updateLoanProduct(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteLoanProduct() tests")
    class DeleteLoanProductTests {

        @Test
        @DisplayName("Should delete loan product successfully")
        void shouldDeleteLoanProduct() {
            given(loanProductRepository.existsById(1L)).willReturn(true);

            assertThatCode(() -> loanProductService.deleteLoanProduct(1L)).doesNotThrowAnyException();

            verify(loanProductRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent product")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            given(loanProductRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> loanProductService.deleteLoanProduct(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}