package com.practical.leavemaster.customerenquiry;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerEnquiryRepository extends JpaRepository<CustomerEnquiry, String> {
    @EntityGraph(attributePaths = "replies")
    List<CustomerEnquiry> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "replies")
    List<CustomerEnquiry> findByStatusOrderByCreatedAtDesc(CustomerEnquiryStatus status);

    @EntityGraph(attributePaths = "replies")
    Optional<CustomerEnquiry> findWithRepliesById(String id);
}
