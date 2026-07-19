package com.chatbrain.repository;

import com.chatbrain.entity.Alias;
import com.chatbrain.entity.MemoryCategory;
import com.chatbrain.entity.MemoryConfidence;
import com.chatbrain.entity.MemorySource;
import com.chatbrain.entity.MemoryType;
import com.chatbrain.entity.PlatformIdentity;
import com.chatbrain.entity.User;
import com.chatbrain.memory.Memory;
import com.chatbrain.memory.MemoryPersistenceService;
import com.chatbrain.memory.MemoryService;
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
@Import({MemoryService.class, MemoryPersistenceService.class})
class RepositoryPersistenceTests {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PlatformIdentityRepository platformIdentityRepository;

	@Autowired
	private AliasRepository aliasRepository;

	@Autowired
	private MemoryRepository memoryRepository;

	@Autowired
	private MemoryPersistenceService memoryPersistenceService;

	@Autowired
	private MemoryService memoryService;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void repositoryBeansLoad() {
		assertThat(userRepository).isNotNull();
		assertThat(platformIdentityRepository).isNotNull();
		assertThat(aliasRepository).isNotNull();
		assertThat(memoryRepository).isNotNull();
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
	void memoryRepositoryKeepsMemoriesScopedToTheirOwner() {
		User firstUser = userRepository.saveAndFlush(User.create());
		User secondUser = userRepository.saveAndFlush(User.create());
		String firstContent = "first-user-memory-" + UUID.randomUUID();
		String secondContent = "second-user-memory-" + UUID.randomUUID();
		com.chatbrain.entity.Memory firstMemory = new com.chatbrain.entity.Memory(
				firstUser,
				MemoryType.ACHIEVEMENT,
				firstContent,
				MemoryConfidence.HIGH,
				MemorySource.USER_MESSAGE);
		com.chatbrain.entity.Memory secondMemory = new com.chatbrain.entity.Memory(
				secondUser,
				MemoryType.PREFERENCE,
				secondContent,
				MemoryConfidence.MEDIUM,
				MemorySource.MANUAL);

		memoryRepository.saveAndFlush(firstMemory);
		memoryRepository.saveAndFlush(secondMemory);
		entityManager.clear();

		assertThat(memoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(firstUser.getId()))
				.extracting(com.chatbrain.entity.Memory::getValue)
				.containsExactly(firstContent)
				.doesNotContain(secondContent);
		assertThat(memoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(secondUser.getId()))
				.extracting(com.chatbrain.entity.Memory::getValue)
				.containsExactly(secondContent)
				.doesNotContain(firstContent);
		assertThat(memoryRepository.findByIdAndUserId(
				firstMemory.getId(), secondUser.getId())).isEmpty();
	}

	@Test
	void memoryServiceOwnsCrudAndTypeQueries() {
		User owner = userRepository.saveAndFlush(User.create());
		com.chatbrain.entity.Memory created = memoryService.createMemory(
				owner,
				MemoryType.LANGUAGE,
				"Java",
				MemoryConfidence.HIGH,
				MemorySource.USER_MESSAGE);

		assertThat(memoryService.findMemoriesForUser(owner))
				.extracting(com.chatbrain.entity.Memory::getValue)
				.containsExactly("Java");
		assertThat(memoryService.findByType(owner, MemoryType.LANGUAGE))
				.extracting(com.chatbrain.entity.Memory::getId)
				.containsExactly(created.getId());

		com.chatbrain.entity.Memory updated = memoryService.updateMemory(
				owner,
				created.getId(),
				MemoryType.PROJECT,
				"CommunityBrain",
				MemoryConfidence.MEDIUM,
				MemorySource.MANUAL);
		assertThat(updated.getValue()).isEqualTo("CommunityBrain");

		memoryService.deleteMemory(owner, created.getId());
		assertThat(memoryService.findMemoriesForUser(owner)).isEmpty();
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
				MemorySource.USER_MESSAGE);

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
				MemorySource.USER_MESSAGE);
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
				MemorySource.USER_MESSAGE))
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
