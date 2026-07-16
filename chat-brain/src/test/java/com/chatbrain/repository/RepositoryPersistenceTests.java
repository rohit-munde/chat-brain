package com.chatbrain.repository;

import com.chatbrain.entity.Alias;
import com.chatbrain.entity.MemoryCategory;
import com.chatbrain.entity.MemorySource;
import com.chatbrain.entity.Platform;
import com.chatbrain.entity.User;
import com.chatbrain.entity.UserMemory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
		"spring.autoconfigure.exclude=",
		"spring.jpa.hibernate.ddl-auto=update"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryPersistenceTests {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AliasRepository aliasRepository;

	@Autowired
	private UserMemoryRepository userMemoryRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void repositoryBeansLoad() {
		assertThat(userRepository).isNotNull();
		assertThat(aliasRepository).isNotNull();
		assertThat(userMemoryRepository).isNotNull();
	}

	@Test
	void userRepositorySavesAndRetrievesUser() {
		User user = instantiate(User.class);
		String displayName = "repository-test-" + UUID.randomUUID();
		user.setDisplayName(displayName);

		userRepository.saveAndFlush(user);
		entityManager.clear();

		assertThat(userRepository.findByDisplayName(displayName))
				.isPresent()
				.get()
				.extracting(User::getRelationshipScore)
				.isEqualTo(0);
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
	void userMemoryRepositorySavesAndRetrievesMemory() {
		UserMemory memory = instantiate(UserMemory.class);
		String content = "repository-test-" + UUID.randomUUID();
		memory.setCategory(MemoryCategory.ACHIEVEMENT);
		memory.setContent(content);
		memory.setImportance(1);
		memory.setSource(MemorySource.USER);

		userMemoryRepository.saveAndFlush(memory);
		entityManager.clear();

		assertThat(userMemoryRepository.findAllByCategory(MemoryCategory.ACHIEVEMENT))
				.extracting(UserMemory::getContent)
				.contains(content);
		assertThat(userMemoryRepository.findAllByOrderByCreatedAtDesc())
				.extracting(UserMemory::getId)
				.contains(memory.getId());
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
