package com.garage.garage_api.enquiry.repository;

import com.garage.garage_api.enquiry.model.Enquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnquiryRepository extends JpaRepository<Enquiry, UUID> {

    Optional<Enquiry> findBySubmissionKey(UUID submissionKey);
}
