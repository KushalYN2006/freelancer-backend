package com.freelancerhub.repository;

import com.freelancerhub.model.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findFirstByContractContractIdOrderByPaymentDateDesc(Integer contractId);
}
