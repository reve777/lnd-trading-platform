package com.lightning.trading.repo;

import com.lightning.trading.entity.RoleApplication;
import com.lightning.trading.entity.UserAccount;
import com.lightning.trading.util.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleApplicationRepository extends JpaRepository<RoleApplication, Long> {

    List<RoleApplication> findAllByOrderByCreatedAtDesc();

    List<RoleApplication> findByStatusOrderByCreatedAtDesc(ApplicationStatus status);

    List<RoleApplication> findByApplicantOrderByCreatedAtDesc(UserAccount applicant);
}
