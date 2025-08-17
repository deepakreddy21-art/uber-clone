package com.uberclone.backend.repository;

import com.uberclone.backend.model.PaymentTransaction;
import com.uberclone.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByUser(User user);
} 