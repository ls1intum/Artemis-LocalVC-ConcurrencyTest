package de.tum.cit.ase;

import de.tum.cit.ase.artemisModel.Course;
import de.tum.cit.ase.artemisModel.ProgrammingExercise;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpSslContextSpec;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static String repositoryUrl;
    public static String exerciseShortName;
    public static final List<Push> successfulPushes = Collections.synchronizedList(new LinkedList<>());
    private static final List<ArtemisUser> users = new LinkedList<>();
    private static WebClient webClient;

    private static String artemisUrl;
    private static String usernameAdmin;
    private static String passwordAdmin;
    private static String usernamePattern;
    private static String passwordPattern;
    private static int numberOfCommits;
    private static int numberOfUsers;

    public static void main(String[] args) {
        init();

        performActionWithAllUsers(i -> {
            try {
                users.get(i).prepareClone();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        performActionWithAllUsers(i -> {
            try {
                users.get(i).executeClone();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        });
        log.info("Cloning done.");

        for (int i = 1; i <= numberOfCommits; i++) {
            log.info("Committing round {}", i);
            int counter = i;
            performActionWithAllUsers(j -> {
                try {
                    users.get(j).updateRepository();
                } catch (IOException | GitAPIException e) {
                    e.printStackTrace();
                }
            });
            performActionWithAllUsers(j -> {
                try {
                    users.get(j).preparePush(counter);
                } catch (IOException | GitAPIException e) {
                    e.printStackTrace();
                }
            });
            performActionWithAllUsers(j -> {
                try {
                    users.get(j).executePush(counter);
                } catch (GitAPIException | IOException e) {
                    e.printStackTrace();
                }
            });
            log.info("Committing round {} done.", i);
        }
        System.out.println("Successful pushes:");
        successfulPushes.forEach(push -> {
            System.out.println(push.username() + ": " + push.commits().size() + " commits");
            push.commits().forEach(commit -> System.out.println("\t" + commit.getFullMessage()));
        });
    }

    private static void performActionWithAllUsers(Consumer<Integer> action) {
        ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(users.size());
        Scheduler scheduler = Schedulers.from(threadPoolExecutor);
        CountDownLatch latch = new CountDownLatch(1);
        try {
            ScheduledExecutorService countdownExecutor = Executors.newSingleThreadScheduledExecutor();
            countdownExecutor.schedule(latch::countDown, 500, TimeUnit.MILLISECONDS);
            countdownExecutor.shutdown();

            Flowable
                    .range(0, users.size())
                    .parallel(users.size())
                    .runOn(scheduler)
                    .doOnNext(i -> {
                        try {
                            latch.await();
                            action.accept(i);
                        } catch (Exception e) {
                            log.warn("Error while performing action for user {{}}: {{}}", i, e.getMessage());
                        }
                    })
                    .sequential()
                    .blockingSubscribe();
        } finally {
            threadPoolExecutor.shutdownNow();
            scheduler.shutdown();
        }
    }

    public static void init() {
        readUserInput();

        for (int i = 1; i <= numberOfUsers; i++) {
            users.add(new ArtemisUser(usernamePattern.replace("{i}", String.valueOf(i)), passwordPattern.replace("{i}", String.valueOf(i))));
        }
        loginAdmin();
        var course = createCourse();
        registerInstructorsForCourse(course.getId());
        var exercise = createProgrammingExercise(course);
        repositoryUrl = exercise.getTemplateParticipation().getRepositoryUrl();
        log.info("Repository URL: {}", repositoryUrl);
        exerciseShortName = exercise.getShortName();
        log.info("Created course {}", course.getTitle());
    }

    public static void readUserInput() {
        var defaultUrl = "https://artemis-staging-localci.artemis.cit.tum.de/";
        var defaultUsernameAdmin = "artemis_test_user_42";
        var defaultPasswordAdmin = System.getenv("DEFAULT_ADMIN_PASSWORD");
        var defaultUsernamePattern = "artemis_test_user_{i}";
        var defaultPasswordPattern = System.getenv("DEFAULT_PASSWORD_PATTERN");
        var defaultNumberOfCommits = 5;
        var defaultNumberOfUsers = 5;

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter Artemis URL (" + defaultUrl +"):");
        artemisUrl = scanner.nextLine();
        if (artemisUrl.isBlank()) {
            artemisUrl = defaultUrl;
        }
        System.out.println("Enter admin username (" + defaultUsernameAdmin +"): ");
        usernameAdmin = scanner.nextLine();
        if (usernameAdmin.isBlank()) {
            usernameAdmin = defaultUsernameAdmin;
        }
        System.out.println("Enter admin password (" + defaultPasswordAdmin +"): ");
        passwordAdmin = scanner.nextLine();
        if (passwordAdmin.isBlank()) {
            passwordAdmin = defaultPasswordAdmin;
        }
        System.out.println("Enter username pattern (" + defaultUsernamePattern +"): ");
        usernamePattern = scanner.nextLine();
        if (usernamePattern.isBlank()) {
            usernamePattern = defaultUsernamePattern;
        }
        System.out.println("Enter password pattern (" + defaultPasswordPattern + "): ");
        passwordPattern = scanner.nextLine();
        if (passwordPattern.isBlank()) {
            passwordPattern = defaultPasswordPattern;
        }
        System.out.println("Enter number of commits (" + defaultNumberOfCommits + "): ");
        var input = scanner.nextLine();
        numberOfCommits = input.isBlank() ? defaultNumberOfCommits : Integer.parseInt(input);
        System.out.println("Enter number of users ("  + defaultNumberOfUsers + "): ");
        input = scanner.nextLine();
        numberOfUsers = input.isBlank() ? defaultNumberOfUsers : Integer.parseInt(input);
        scanner.close();

        log.info("Artemis URL: {}", artemisUrl);
        log.info("Admin username: {}", usernameAdmin);
        log.info("Username pattern: {}", usernamePattern);
        log.info("Number of commits: {}", numberOfCommits);
        log.info("Number of users: {}", numberOfUsers);
    }

    public static void loginAdmin() {
        log.info("Logging in admin");
        WebClient webClient = WebClient
                .builder()
                .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                .baseUrl(artemisUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        var payload = Map.of("username", usernameAdmin, "password", passwordAdmin, "rememberMe", true);
        var response = webClient.post().uri("api/public/authenticate").bodyValue(payload).retrieve().toBodilessEntity().block();

        if (response == null) {
            throw new RuntimeException("Login failed - No response received");
        }
        var header = response.getHeaders().get("Set-Cookie");
        if (header == null) {
            throw new RuntimeException("Login failed - No cookie received");
        }
        var cookieHeader = header.get(0);
        var authToken = AuthToken.fromResponseHeaderString(cookieHeader);
        Main.webClient =
                WebClient
                        .builder()
                        .clientConnector(new ReactorClientHttpConnector(createHttpClient()))
                        .baseUrl(artemisUrl)
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader("Cookie", authToken.jwtToken())
                        .build();
        log.debug("Logged in as admin");
    }

    public static Course createCourse() {
        var randomInt = (int) (Math.random() * 10_0000);
        var course = new Course("Concurrency Test Course " + randomInt, "concurrency" + randomInt);

        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.pathSegment("api", "admin", "courses").build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("course", course))
                .retrieve()
                .bodyToMono(Course.class)
                .block();
    }

    public static void registerInstructorsForCourse(long courseId) {
        for (int i = 0; i < users.size(); i++) {
            int finalI = i;
            webClient
                    .post()
                    .uri(uriBuilder ->
                            uriBuilder.pathSegment("api", "courses", String.valueOf(courseId), "instructors", users.get(finalI).getUsername()).build()
                    )
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        }
    }

    public static ProgrammingExercise createProgrammingExercise(Course course) {
        var programmingExercise = new ProgrammingExercise();
        programmingExercise.setTitle("Programming Exercise for Concurrency Test");
        programmingExercise.setMaxPoints(1.0);
        programmingExercise.setShortName("progForConc");
        programmingExercise.setPackageName("progforconc");
        programmingExercise.setCourse(course);

        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.pathSegment("api", "programming-exercises", "setup").build())
                .bodyValue(programmingExercise)
                .retrieve()
                .bodyToMono(ProgrammingExercise.class)
                .block();
    }

    private static HttpClient createHttpClient() {
        return HttpClient
                .create()
                .secure(spec -> {
                    try {
                        spec
                                .sslContext(TcpSslContextSpec.forClient().sslContext())
                                .handshakeTimeout(Duration.ofSeconds(30))
                                .closeNotifyFlushTimeout(Duration.ofSeconds(30))
                                .closeNotifyReadTimeout(Duration.ofSeconds(30));
                    } catch (SSLException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
