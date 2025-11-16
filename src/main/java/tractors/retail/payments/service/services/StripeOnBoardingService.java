package tractors.retail.payments.service.services;

import tractors.retail.payments.service.models.Seller;
import tractors.retail.payments.service.repository.SellerRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeOnBoardingService {

    private final SellerRepository sellerRepository;

    @Value("${stripe.refresh-url}")
    private String refreshUrl;

    @Value("${stripe.return-url}")
    private String returnUrl;

    public StripeOnBoardingService(SellerRepository ownerRepository) {
        this.sellerRepository = ownerRepository;
    }

    public String createConnectedAccount(String ownerEmail, String ownerName) throws StripeException {
        Seller owner = sellerRepository.findByEmail(ownerEmail)
                .orElseGet(() -> sellerRepository.save(
                        Seller.builder().email(ownerEmail).name(ownerName).verified(false).build()
                ));

        if (owner.getStripeAccountId() != null) {
            return owner.getStripeAccountId();
        }

        AccountCreateParams params = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .setCountry("PT") // Change if needed
                .setEmail(ownerEmail)
                .build();

        Account account = Account.create(params);

        owner.setStripeAccountId(account.getId());
        sellerRepository.save(owner);

        return account.getId();
    }

    public String generateOnboardingLink(String accountId) throws StripeException {
        AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                .setAccount(accountId)
                .setRefreshUrl(refreshUrl)
                .setReturnUrl(returnUrl)
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();

        AccountLink link = AccountLink.create(linkParams);
        return link.getUrl();
    }

    public void markAccountVerified(String accountId) {
        sellerRepository.findAll().stream()
                .filter(o -> accountId.equals(o.getStripeAccountId()))
                .findFirst()
                .ifPresent(owner -> {
                    owner.setVerified(true);
                    sellerRepository.save(owner);
                });
    }
}
