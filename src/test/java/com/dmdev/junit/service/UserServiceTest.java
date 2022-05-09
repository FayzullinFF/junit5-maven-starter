package com.dmdev.junit.service;

import com.dmdev.junit.TestBase;
import com.dmdev.junit.dao.UserDao;
import com.dmdev.junit.dto.User;
import com.dmdev.junit.extension.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("fast")
@Tag("user")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class)
@ExtendWith({
        UserServiceParamResolver.class,
        PostProcessingExtension.class,
        ConditionalExtension.class,
        MockitoExtension.class
        //ThrowableExtension.class
        //GlobalExtension.class
})
public class UserServiceTest extends TestBase {

    @Captor
    private ArgumentCaptor<Integer> argumentCaptor;
    @Mock(lenient = true)
    private UserDao userDao;
    @InjectMocks
    private UserService userService;

    UserServiceTest(TestInfo testInfo) {
        System.out.println();
    }

    private static final User IVAN = User.of(1, "Ivan", "123");
    private static final User PETR = User.of(2, "Petr", "456");

    @BeforeAll
    void init() {
        System.out.println("Before all: " + this);
    }

    @BeforeEach
    void prepare() {
        System.out.println("Before each: " + this);
        //lenient().when(userDao.delete(IVAN.getId())).thenReturn(true);
        doReturn(true).when(userDao).delete(IVAN.getId());
        //Mockito.mock(UserDao.class, withSettings().lenient());
        /*this.userDao = Mockito.spy(new UserDao());
        this.userService = new UserService(userDao);*/
    }

    @Test
    void throwExceptionIfDatabaseIsNotAvailable() {
        doThrow(RuntimeException.class).when(userDao).delete(IVAN.getId());
        assertThrows(RuntimeException.class, () -> userService.delete(IVAN.getId()));
    }

    @Test
    void shouldDeleteExistedUser() {
        userService.add(IVAN);
        //Mockito.doReturn(true).when(userDao).delete(IVAN.getId());
        //Mockito.doReturn(true).when(userDao).delete(Mockito.any());
        /*Mockito.when(userDao.delete(IVAN.getId()))
                .thenReturn(true)
                .thenReturn(false);*/
        //BDDMockito.given(userDao.delete(IVAN.getId())).willReturn(true);

        var deleteResult = userService.delete(IVAN.getId());
        System.out.println(userService.delete(IVAN.getId()));
        System.out.println(userService.delete(IVAN.getId()));

        //var argumentCaptor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(userDao, Mockito.times(3)).delete(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue()).isEqualTo(IVAN.getId());

        //Mockito.reset();

        assertThat(deleteResult).isTrue();
    }

    @Test
    @Order(1)
    @DisplayName("users will be empty if no users added")
    void usersEmptyIfNoUsersAdded(UserService userService) throws IOException {
        if (userService.getAll().isEmpty()) {
            throw new RuntimeException();
        }
        System.out.println("Test 1: " + this);
        var users = userService.getAll();

        // Hamcrest
        MatcherAssert.assertThat(users, IsEmptyCollection.empty());
        // AssertJ
        assertTrue(users.isEmpty());
    }

    @Test
    @Order(2)
    void usersSizeIfUserAdded() {
        // given
        System.out.println("Test 2: " + this);
        userService.add(IVAN);
        userService.add(PETR);

        // when
        var users = userService.getAll();

        // then
        assertThat(users).hasSize(2);
        //assertEquals(2, users.size());
    }

    @Test
    void usersConvertedToMapById() {
        userService.add(PETR);

        Map<Integer, User> users = userService.getAllConvertedById();
        // Hamcrest
        //MatcherAssert.assertThat(users, IsMapContaining.hasKey(IVAN.getId()));
        // AssertJ
        assertAll(
                () -> assertThat(users).containsKeys(IVAN.getId(), PETR.getId()),
                () -> assertThat(users).containsValues(IVAN, PETR)
        );
    }

    @AfterEach
    void deleteDataFromDatabase() {
        System.out.println("After each: " + this);
    }

    @AfterAll
    void closeConnectionPool() {
        System.out.println("After all: " + this);
    }

    @Nested
    @DisplayName("test user login functionality")
    @Tag("login")
    class LoginTest {
        @Test
        void loginSuccessIfUserExists() {
            userService.add(IVAN);
            Optional<User> maybeUser = userService.login(IVAN.getUsername(), IVAN.getPassword());

            assertThat(maybeUser).isPresent();
            //assertTrue(maybeUser.isPresent());
            maybeUser.ifPresent(user -> assertThat(user).isEqualTo(IVAN));
            //maybeUser.ifPresent(user -> assertEquals(IVAN, user));
        }

        @Test
        void throwExceptionIfUsernameOrPasswordIsNull() {
            assertAll(
                    () -> {
                        var exception = assertThrows(IllegalArgumentException.class,
                                () -> userService.login(null, "dummy"));
                        assertThat(exception.getMessage()).isEqualTo("username or password is null");
                    },
                    () -> assertThrows(IllegalArgumentException.class, () -> userService.login("dummy", null))
            );
        }

        @Test
        @Disabled("flaky, need to see")
        void loginFailIfPasswordIsNotCorrect() {
            userService.add(IVAN);

            var maybeUser = userService.login(IVAN.getUsername(), "dummy");

            assertTrue(maybeUser.isEmpty());
        }

        //@Test
        @RepeatedTest(value = 5, name = RepeatedTest.LONG_DISPLAY_NAME)
        void loginFailIfUserDoesNotExists(RepetitionInfo repetitionInfo) {
            userService.add(IVAN);

            var maybeUser = userService.login("dummy", IVAN.getPassword());

            assertTrue(maybeUser.isEmpty());
        }

        @ParameterizedTest(name = "{arguments} test")
        //@ArgumentsSource()
        //@NullSource
        //@EmptySource
        //@NullAndEmptySource
        //@ValueSource(strings = {
        //"Ivan", "Petr"
        //})
        //@EnumSource
        @MethodSource("com.dmdev.junit.service.UserServiceTest#getArgumentsForLoginTest")
        //@CsvFileSource(resources = "/login-test-data.csv", delimiter = ',', numLinesToSkip = 1)
        /*@CsvSource({
                "Ivan,123",
                "Petr,456"

        })*/
        @DisplayName("login param test")
        void loginParametrizedTest(String username, String password, Optional<User> user) {
            userService.add(IVAN, PETR);
            var maybeUser = userService.login(username, password);
            assertThat(maybeUser).isEqualTo(user);
        }

        @Test
        @Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
        void checkLoginFunctionalityPerformance() {
            System.out.println(Thread.currentThread().getName());
            var result = assertTimeoutPreemptively(Duration.ofMillis(200), () -> {
                Thread.sleep(100L);
                System.out.println(Thread.currentThread().getName());
                return userService.login("dummy", IVAN.getPassword());
            });
        }


    }
    static Stream<Arguments> getArgumentsForLoginTest() {
        return Stream.of(
                Arguments.of("Ivan", "123", Optional.of(IVAN)),
                Arguments.of("Petr", "456", Optional.of(PETR)),
                Arguments.of("Petr", "dummy", Optional.empty()),
                Arguments.of("dummy", "456", Optional.empty())
        );
    }
}
