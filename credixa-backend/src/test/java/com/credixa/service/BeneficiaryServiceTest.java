package com.credixa.service;

import com.credixa.dto.request.AddBeneficiaryRequestDTO;
import com.credixa.dto.response.BeneficiaryResponseDTO;
import com.credixa.entity.Beneficiary;
import com.credixa.entity.User;
import com.credixa.exception.ConflictException;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.repository.BeneficiaryRepository;
import com.credixa.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryRepository beneficiaryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BeneficiaryService beneficiaryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userCode("USR00001")
                .firstName("John")
                .build();
    }

    @Nested
    @DisplayName("addBeneficiary() tests")
    class AddBeneficiaryTests {

        @Test
        @DisplayName("Should add beneficiary successfully for internal IFSC")
        void shouldAddBeneficiaryForInternalIfsc() {
            AddBeneficiaryRequestDTO request = AddBeneficiaryRequestDTO.builder()
                    .beneficiaryName("Jane Doe")
                    .accountNumber("0987654321")
                    .ifscCode("CRDX0002")
                    .bankName("Credixa Bank")
                    .nickname("Jane")
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(beneficiaryRepository.findByUserAndAccountNumberAndIfscCode(any(), any(), any()))
                    .willReturn(Optional.empty());
            given(beneficiaryRepository.save(any(Beneficiary.class))).willAnswer(invocation -> {
                Beneficiary b = invocation.getArgument(0);
                b.setId(1L);
                b.setCreatedAt(LocalDateTime.now());
                return b;
            });

            BeneficiaryResponseDTO response = beneficiaryService.addBeneficiary("USR00001", request);

            assertThat(response).isNotNull();
            verify(beneficiaryRepository).save(argThat(b -> 
                    b.isVerified() == true &&
                    b.getStatus() == Beneficiary.BeneficiaryStatus.ACTIVE
            ));
        }

        @Test
        @DisplayName("Should add beneficiary successfully for external IFSC")
        void shouldAddBeneficiaryForExternalIfsc() {
            AddBeneficiaryRequestDTO request = AddBeneficiaryRequestDTO.builder()
                    .beneficiaryName("Jane Doe")
                    .accountNumber("0987654321")
                    .ifscCode("HDFC0001234")
                    .bankName("HDFC Bank")
                    .nickname("Jane")
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(beneficiaryRepository.findByUserAndAccountNumberAndIfscCode(any(), any(), any()))
                    .willReturn(Optional.empty());
            given(beneficiaryRepository.save(any(Beneficiary.class))).willAnswer(invocation -> {
                Beneficiary b = invocation.getArgument(0);
                b.setId(1L);
                return b;
            });

            BeneficiaryResponseDTO response = beneficiaryService.addBeneficiary("USR00001", request);

            assertThat(response).isNotNull();
            verify(beneficiaryRepository).save(argThat(b -> 
                    b.isVerified() == false && // External IFSC not verified in demo
                    b.getStatus() == Beneficiary.BeneficiaryStatus.ACTIVE
            ));
        }

        @Test
        @DisplayName("Should throw ConflictException when beneficiary already exists")
        void shouldThrowExceptionWhenBeneficiaryExists() {
            Beneficiary existingBeneficiary = Beneficiary.builder()
                    .id(1L)
                    .user(testUser)
                    .accountNumber("0987654321")
                    .ifscCode("CRDX0002")
                    .build();

            AddBeneficiaryRequestDTO request = AddBeneficiaryRequestDTO.builder()
                    .beneficiaryName("Jane Doe")
                    .accountNumber("0987654321")
                    .ifscCode("CRDX0002")
                    .bankName("Credixa Bank")
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(beneficiaryRepository.findByUserAndAccountNumberAndIfscCode(any(), any(), any()))
                    .willReturn(Optional.of(existingBeneficiary));

            assertThatThrownBy(() -> beneficiaryService.addBeneficiary("USR00001", request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Beneficiary already exists");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            AddBeneficiaryRequestDTO request = AddBeneficiaryRequestDTO.builder()
                    .beneficiaryName("Jane Doe")
                    .accountNumber("0987654321")
                    .ifscCode("CRDX0002")
                    .build();

            given(userRepository.findByUserCode("USR99999")).willReturn(Optional.empty());

            assertThatThrownBy(() -> beneficiaryService.addBeneficiary("USR99999", request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getBeneficiaries() tests")
    class GetBeneficiariesTests {

        @Test
        @DisplayName("Should return active beneficiaries only")
        void shouldReturnActiveBeneficiaries() {
            Beneficiary activeBeneficiary = Beneficiary.builder()
                    .id(1L)
                    .user(testUser)
                    .status(Beneficiary.BeneficiaryStatus.ACTIVE)
                    .build();

            Beneficiary inactiveBeneficiary = Beneficiary.builder()
                    .id(2L)
                    .user(testUser)
                    .status(Beneficiary.BeneficiaryStatus.INACTIVE)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(beneficiaryRepository.findByUserAndStatus(any(), eq(Beneficiary.BeneficiaryStatus.ACTIVE)))
                    .willReturn(List.of(activeBeneficiary));

            List<BeneficiaryResponseDTO> response = beneficiaryService.getBeneficiaries("USR00001");

            assertThat(response).hasSize(1);
            assertThat(response.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should return empty list when no beneficiaries")
        void shouldReturnEmptyListWhenNoBeneficiaries() {
            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(beneficiaryRepository.findByUserAndStatus(any(), any()))
                    .willReturn(List.of());

            List<BeneficiaryResponseDTO> response = beneficiaryService.getBeneficiaries("USR00001");

            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBeneficiaryById() tests")
    class GetBeneficiaryByIdTests {

        @Test
        @DisplayName("Should return beneficiary successfully for owner")
        void shouldReturnBeneficiaryForOwner() {
            Beneficiary beneficiary = Beneficiary.builder()
                    .id(1L)
                    .user(testUser)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(beneficiaryRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.of(beneficiary));

            BeneficiaryResponseDTO response = beneficiaryService.getBeneficiaryById(1L, "USR00001");

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when beneficiary not found")
        void shouldThrowExceptionWhenBeneficiaryNotFound() {
            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(beneficiaryRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.empty());

            assertThatThrownBy(() -> beneficiaryService.getBeneficiaryById(1L, "USR00001"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("access denied");
        }
    }

    @Nested
    @DisplayName("deleteBeneficiary() tests")
    class DeleteBeneficiaryTests {

        @Test
        @DisplayName("Should soft delete beneficiary successfully")
        void shouldSoftDeleteBeneficiary() {
            Beneficiary beneficiary = Beneficiary.builder()
                    .id(1L)
                    .user(testUser)
                    .status(Beneficiary.BeneficiaryStatus.ACTIVE)
                    .build();

            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(beneficiaryRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.of(beneficiary));

            beneficiaryService.deleteBeneficiary(1L, "USR00001");

            verify(beneficiaryRepository).save(argThat(b -> 
                    b.getStatus() == Beneficiary.BeneficiaryStatus.INACTIVE
            ));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent beneficiary")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            given(userRepository.findByUserCode("USR00001")).willReturn(Optional.of(testUser));
            given(beneficiaryRepository.findByIdAndUser(1L, testUser)).willReturn(Optional.empty());

            assertThatThrownBy(() -> beneficiaryService.deleteBeneficiary(1L, "USR00001"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}