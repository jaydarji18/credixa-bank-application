package com.credixa.service;

import com.credixa.dto.request.BranchRequestDTO;
import com.credixa.dto.response.BranchResponseDTO;
import com.credixa.entity.Bank;
import com.credixa.entity.Branch;
import com.credixa.exception.ResourceNotFoundException;
import com.credixa.repository.BankRepository;
import com.credixa.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BankRepository bankRepository;

    @InjectMocks
    private BranchService branchService;

    private Bank testBank;

    @BeforeEach
    void setUp() {
        testBank = Bank.builder()
                .id(1L)
                .bankName("Credixa Bank")
                .build();
    }

    @Nested
    @DisplayName("getAllBranches() tests")
    class GetAllBranchesTests {

        @Test
        @DisplayName("Should return all branches successfully")
        void shouldReturnAllBranches() {
            Branch branch1 = Branch.builder()
                    .id(1L)
                    .branchName("Branch 1")
                    .build();

            Branch branch2 = Branch.builder()
                    .id(2L)
                    .branchName("Branch 2")
                    .build();

            given(branchRepository.findAll()).willReturn(List.of(branch1, branch2));

            List<BranchResponseDTO> response = branchService.getAllBranches();

            assertThat(response).hasSize(2);
            verify(branchRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no branches exist")
        void shouldReturnEmptyListWhenNoBranches() {
            given(branchRepository.findAll()).willReturn(List.of());

            List<BranchResponseDTO> response = branchService.getAllBranches();

            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBranchById() tests")
    class GetBranchByIdTests {

        @Test
        @DisplayName("Should return branch successfully")
        void shouldReturnBranch() {
            Branch branch = Branch.builder()
                    .id(1L)
                    .branchName("Main Branch")
                    .build();

            given(branchRepository.findById(1L)).willReturn(Optional.of(branch));

            BranchResponseDTO response = branchService.getBranchById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getBranchName()).isEqualTo("Main Branch");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when branch not found")
        void shouldThrowExceptionWhenBranchNotFound() {
            given(branchRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> branchService.getBranchById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Branch not found");
        }
    }

    @Nested
    @DisplayName("createBranch() tests")
    class CreateBranchTests {

        @Test
        @DisplayName("Should create branch successfully")
        void shouldCreateBranch() {
            BranchRequestDTO request = BranchRequestDTO.builder()
                    .branchName("New Branch")
                    .branchCode("BR001")
                    .ifscCode("CRDX0005")
                    .address("123 Street")
                    .city("Mumbai")
                    .state("Maharashtra")
                    .pincode("400001")
                    .phone("9876543210")
                    .build();

            given(bankRepository.findAll()).willReturn(List.of(testBank));
            given(branchRepository.save(any(Branch.class))).willAnswer(invocation -> {
                Branch b = invocation.getArgument(0);
                b.setId(1L);
                return b;
            });

            BranchResponseDTO response = branchService.createBranch(request);

            assertThat(response).isNotNull();
            verify(branchRepository).save(any(Branch.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when bank not found")
        void shouldThrowExceptionWhenBankNotFound() {
            BranchRequestDTO request = BranchRequestDTO.builder()
                    .branchName("New Branch")
                    .build();

            given(bankRepository.findAll()).willReturn(List.of());

            assertThatThrownBy(() -> branchService.createBranch(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No bank found");
        }
    }

    @Nested
    @DisplayName("updateBranch() tests")
    class UpdateBranchTests {

        @Test
        @DisplayName("Should update branch successfully")
        void shouldUpdateBranch() {
            Branch existingBranch = Branch.builder()
                    .id(1L)
                    .branchName("Old Name")
                    .build();

            BranchRequestDTO request = BranchRequestDTO.builder()
                    .branchName("Updated Name")
                    .branchCode("BR002")
                    .ifscCode("CRDX0006")
                    .address("456 Street")
                    .city("Delhi")
                    .state("Delhi")
                    .pincode("110001")
                    .phone("9876543211")
                    .build();

            given(branchRepository.findById(1L)).willReturn(Optional.of(existingBranch));
            given(branchRepository.save(any(Branch.class))).willAnswer(invocation -> invocation.getArgument(0));

            BranchResponseDTO response = branchService.updateBranch(1L, request);

            assertThat(response).isNotNull();
            verify(branchRepository).save(argThat(branch -> 
                    branch.getBranchName().equals("Updated Name")
            ));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent branch")
        void shouldThrowExceptionWhenUpdatingNonExistent() {
            BranchRequestDTO request = BranchRequestDTO.builder()
                    .branchName("Updated Name")
                    .build();

            given(branchRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> branchService.updateBranch(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteBranch() tests")
    class DeleteBranchTests {

        @Test
        @DisplayName("Should delete branch successfully")
        void shouldDeleteBranch() {
            given(branchRepository.existsById(1L)).willReturn(true);

            assertThatCode(() -> branchService.deleteBranch(1L)).doesNotThrowAnyException();

            verify(branchRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent branch")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            given(branchRepository.existsById(999L)).willReturn(false);

            assertThatThrownBy(() -> branchService.deleteBranch(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}