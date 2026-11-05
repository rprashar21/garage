package com.garage.garage_api.notification.repository;

import com.garage.garage_api.notification.model.NotificationTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, UUID> {
}
