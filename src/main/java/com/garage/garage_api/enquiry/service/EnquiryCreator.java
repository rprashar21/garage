package com.garage.garage_api.enquiry.service;

import com.garage.garage_api.enquiry.model.Enquiry;
import com.garage.garage_api.enquiry.repository.EnquiryRepository;
import com.garage.garage_api.notification.model.NotificationTask;
import com.garage.garage_api.notification.repository.NotificationTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Separate bean so the @Transactional proxy applies. EnquiryService calls this from
// outside the transaction, so it can recover from duplicate-key failures.
@Component
@RequiredArgsConstructor
public class EnquiryCreator {

    private final EnquiryRepository enquiryRepository;
    private final NotificationTaskRepository notificationTaskRepository;

    // Enquiry and its outbox task commit together or not at all.
    @Transactional
    public Enquiry create(Enquiry enquiry) {
        Enquiry saved = enquiryRepository.save(enquiry);
        notificationTaskRepository.save(NotificationTask.pendingEmailFor(saved.getId()));
        return saved;
    }
}
