package com.chatbrain.repository;

import com.chatbrain.entity.Alias;
import com.chatbrain.entity.MemoryCategory;
import com.chatbrain.entity.MemorySource;
import com.chatbrain.entity.PlatformIdentity;
import com.chatbrain.entity.User;
import com.chatbrain.entity.UserMemory;
import com.chatbrain.memory.Memory;
import com.chatbrain.memory.MemoryPersistenceService;
import com.chatbrain.platform.Platform;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
		"spring.autoconfigure.exclude=",
		"spring.jpa.hibernate.ddl-auto=update"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MemoryPersistenceService.class)
class RepositoryPersistenceTests {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PlatformIdentityRepository platformIdentityRepository;

	@Autowired
	private AliasRepository aliasRepository;

	@Autowired
	private UserMemoryRepository userMemoryRepository;

	@Autowired
	private MemoryPersistenceService memoryPersistenceService;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void repositoryBeansLoad() {
		assertThat(userRepository).isNotNull();
		assertThat(platformIdentityRepository).isNotNull();
		assertThat(aliasRepository).isNotNull();
		assertThat(userMemoryRepository).isNotNull();
	}

	@Test
	void userAndPlatformIdentityRepositoriesSaveAndRetrieveIdentity() {
		User user = User.create();
		user.setRelationshipScore(1);
		User savedUser = userRepository.saveAndFlush(user);
		String platformUserId = "repository-test-" + UUID.randomUUID();
		PlatformIdentity identity = new PlatformIdentity(
				Platform.YOUTUBE,
				platformUserId,
				"@test-viewer",
				"Test Viewer",
				savedUser);
		platformIdentityRepository.saveAndFlush(identity);
		entityManager.clear();

		assertThat(platformIdentityRepository.findByPlatformAndPlatformUserId(
				Platform.YOUTUBE,
				platformUserId))
				.isPresent()
				.get()
				.extracting(PlatformIdentity::getDisplayName)
				.isEqualTo("Test Viewer");
		assertThat(userRepository.findById(savedUser.getId())).isPresent();
	}

	@Test
	void aliasRepositorySavesAndRetrievesAlias() {
		Alias alias = instantiate(Alias.class);
		String username = "repository-test-" + UUID.randomUUID();
		alias.setPlatform(Platform.YOUTUBE);
		alias.setUsername(username);

		aliasRepository.saveAndFlush(alias);
		entityManager.clear();

		assertThat(aliasRepository.findByPlatformAndUsername(Platform.YOUTUBE, username))
				.isPresent();
	}

	@Test
	void userMemoryRepositoryKeepsMemoriesScopedToTheirOwner() {
		User firstUser = userRepository.saveAndFlush(User.create());
		User secondUser = userRepository.saveAndFlush(User.create());
		String firstContent = "first-user-memory-" + UUID.randomUUID();
		String secondContent = "second-user-memory-" + UUID.randomUUID();
		UserMemory firstMemory = new UserMemory(
				firstUser,
				MemoryCategory.ACHIEVEMENT,
				firstContent,
				1,
				MemorySource.USER);
		UserMemory secondMemory = new UserMemory(
				secondUser,
				MemoryCategory.PREFERENCE,
				secondContent,
				1,
				MemorySource.USER);

		userMemoryRepository.saveAndFlush(firstMemory);
		userMemoryRepository.saveAndFlush(secondMemory);
		entityManager.clear();

		assertThat(userMemoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(firstUser.getId()))
				.extracting(UserMemory::getContent)
				.containsExactly(firstContent)
				.doesNotContain(secondContent);
		assertThat(userMemoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(secondUser.getId()))
				.extracting(UserMemory::getContent)
				.containsExactly(secondContent)
				.doesNotContain(firstContent);
		assertThat(userMemoryRepository.findByIdAndUserId(
				firstMemory.getId(), secondUser.getId())).isEmpty();
	}

	@Test
	void memoryPersistenceServicePersistsRetrievesAndUpdatesOwnedMemory() {
		User owner = userRepository.saveAndFlush(User.create());
		User differentUser = userRepository.saveAndFlush(User.create());

		Memory persisted = memoryPersistenceService.persist(
				owner,
				MemoryCategory.INTEREST,
				"Uses Spring Boot",
				2,
				MemorySource.USER);

		assertThat(persisted.id()).isNotNull();
		assertThat(memoryPersistenceService.exists(
				owner, MemoryCategory.INTEREST, "uses spring boot")).isTrue();
		assertThat(memoryPersistenceService.retrieveRecent(owner))
				.extracting(Memory::content)
				.contains("Uses Spring Boot");
		assertThat(memoryPersistenceService.retrieveRecent(differentUser)).isEmpty();

		Memory updated = memoryPersistenceService.update(
				owner,
				persisted.id(),
				MemoryCategory.PREFERENCE,
				"Prefers Spring Boot",
				3,
				MemorySource.USER);
		assertThat(updated.content()).isEqualTo("Prefers Spring Boot");
		assertThat(memoryPersistenceService.retrieveRecent(owner))
				.extracting(Memory::content)
				.containsExactly("Prefers Spring Boot");
		assertThatThrownBy(() -> memoryPersistenceService.update(
				differentUser,
				persisted.id(),
				MemoryCategory.OTHER,
				"Not allowed",
				1,
				MemorySource.USER))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static <T> T instantiate(Class<T> entityType) {
		try {
			var constructor = entityType.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException
				 | InvocationTargetException exception) {
			throw new AssertionError("Unable to instantiate entity " + entityType.getSimpleName(), exception);
		}
	}
}
