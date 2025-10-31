package br.com.flowlinkerAPI.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.flowlinkerAPI.model.SocialMediaAccount;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SocialMediaAccountRepository extends JpaRepository<SocialMediaAccount, Long> {

    List<SocialMediaAccount> findAllByPlatform(SocialMediaAccount.SocialMediaPlatform platform);

    List<SocialMediaAccount> findAllByPlatformAndStatusNot(
        SocialMediaAccount.SocialMediaPlatform platform,
        SocialMediaAccount.SocialMediaAccountStatus status
    );

    List<SocialMediaAccount> findAllByCustomerIdAndPlatformAndStatusNot(
        Long customerId,
        SocialMediaAccount.SocialMediaPlatform platform,
        SocialMediaAccount.SocialMediaAccountStatus status
    );
}