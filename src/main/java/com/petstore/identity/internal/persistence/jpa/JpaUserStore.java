package com.petstore.identity.internal.persistence.jpa;

import com.petstore.identity.internal.UserStore;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Relational adapter of the identity port. Replaces the container-managed {@code UserEJB}. */
@Repository
@Transactional(readOnly = true)
class JpaUserStore implements UserStore {

  private final EntityManager entityManager;

  JpaUserStore(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public Optional<StoredUser> find(String username) {
    return Optional.ofNullable(entityManager.find(UserEntity.class, username))
        .map(user -> new StoredUser(user.username(), user.passwordHash()));
  }

  /**
   * The existence check and the insert run in one transaction. If two requests create the same name
   * at the same moment, the primary key rejects the second insert and it is reported as "already
   * exists" like any other duplicate.
   */
  @Override
  @Transactional(
      noRollbackFor = {PersistenceException.class, DataIntegrityViolationException.class})
  public boolean create(String username, String passwordHash) {
    if (entityManager.find(UserEntity.class, username) != null) {
      return false;
    }
    try {
      entityManager.persist(new UserEntity(username, passwordHash));
      entityManager.flush();
      return true;
    } catch (PersistenceException | DataIntegrityViolationException duplicate) {
      entityManager.clear();
      return false;
    }
  }

  @Override
  public boolean isEmpty() {
    return entityManager
            .createQuery("select count(u) from UserEntity u", Long.class)
            .getSingleResult()
        == 0;
  }
}
