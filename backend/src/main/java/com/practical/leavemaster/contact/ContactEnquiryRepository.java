package com.practical.leavemaster.contact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactEnquiryRepository extends JpaRepository<ContactEnquiry, String> {
    List<ContactEnquiry> findAllByOrderByCreatedAtDesc();
    List<ContactEnquiry> findByStatusOrderByCreatedAtDesc(ContactEnquiryStatus status);
}
