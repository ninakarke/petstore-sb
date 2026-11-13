package com.petstore.customer.internal.persistence.jpa;

import com.petstore.customer.Account;
import com.petstore.customer.Address;
import com.petstore.customer.ContactInfo;
import com.petstore.customer.CreditCard;
import com.petstore.customer.Customer;
import com.petstore.customer.Profile;
import com.petstore.customer.internal.CustomerStore;
import com.petstore.shared.RelationalStore;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relational adapter of the customer port.
 *
 * <p>Replaces the six container-managed entity beans of the legacy customer component. The customer
 * is stored in six tables that share the customer's key, read and written here as one unit inside
 * one transaction. The rest of the application never sees the tables.
 */
@Repository
@RelationalStore
@Transactional(readOnly = true)
class JpaCustomerStore implements CustomerStore {

  private final EntityManager entityManager;

  JpaCustomerStore(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public Optional<Customer> find(String userId) {
    if (entityManager.find(CustomerEntity.class, userId) == null) {
      return Optional.empty();
    }
    AccountEntity account = entityManager.find(AccountEntity.class, userId);
    ContactInfoEntity contact = entityManager.find(ContactInfoEntity.class, userId);
    AddressEntity address = entityManager.find(AddressEntity.class, userId);
    CreditCardEntity card = entityManager.find(CreditCardEntity.class, userId);
    ProfileEntity profile = entityManager.find(ProfileEntity.class, userId);
    return Optional.of(
        new Customer(
            userId,
            new Account(
                account.status,
                new ContactInfo(
                    contact.givenName,
                    contact.familyName,
                    new Address(
                        address.streetName1,
                        address.streetName2,
                        address.city,
                        address.state,
                        address.zipCode,
                        address.country),
                    contact.email,
                    contact.telephone),
                new CreditCard(card.cardNumber, card.cardType, card.expiryDate)),
            new Profile(
                profile.preferredLanguage,
                profile.favoriteCategory,
                profile.myListPreference,
                profile.bannerPreference)));
  }

  /** Rows are written parent first, so the foreign keys always find their target. */
  @Override
  @Transactional
  public void save(Customer customer) {
    String userId = customer.userId();
    ContactInfo contactInfo = customer.account().contactInfo();
    Address addressValue = contactInfo.address();
    CreditCard creditCard = customer.account().creditCard();
    Profile profileValue = customer.profile();

    entityManager.merge(new CustomerEntity(userId));

    AccountEntity account = new AccountEntity(userId);
    account.status = customer.account().status();
    entityManager.merge(account);

    ContactInfoEntity contact = new ContactInfoEntity(userId);
    contact.givenName = contactInfo.givenName();
    contact.familyName = contactInfo.familyName();
    contact.telephone = contactInfo.telephone();
    contact.email = contactInfo.email();
    entityManager.merge(contact);

    AddressEntity address = new AddressEntity(userId);
    address.streetName1 = addressValue.streetName1();
    address.streetName2 = addressValue.streetName2();
    address.city = addressValue.city();
    address.state = addressValue.state();
    address.zipCode = addressValue.zipCode();
    address.country = addressValue.country();
    entityManager.merge(address);

    CreditCardEntity card = new CreditCardEntity(userId);
    card.cardNumber = creditCard.cardNumber();
    card.cardType = creditCard.cardType();
    card.expiryDate = creditCard.expiryDate();
    entityManager.merge(card);

    ProfileEntity profile = new ProfileEntity(userId);
    profile.preferredLanguage = profileValue.preferredLanguage();
    profile.favoriteCategory = profileValue.favoriteCategory();
    profile.myListPreference = profileValue.myListPreference();
    profile.bannerPreference = profileValue.bannerPreference();
    entityManager.merge(profile);

    // Write now, in this order, instead of leaving the order to the end of the transaction.
    entityManager.flush();
  }

  @Override
  public boolean isEmpty() {
    return entityManager
            .createQuery("select count(c) from CustomerEntity c", Long.class)
            .getSingleResult()
        == 0;
  }
}
