package de.tum.cit.ase;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static de.tum.cit.ase.Main.*;

public class ArtemisUser {
    private static final Logger log = LoggerFactory.getLogger(ArtemisUser.class);
    private final String username;
    private final String password;
    private CloneCommand cloneCommand;
    private PushCommand pushCommand;
    private Git git;
    private Iterable<RevCommit> unpushedCommits;

    public ArtemisUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void prepareClone() throws IOException {
        var localPath = Path.of("repos", username);
        FileUtils.deleteDirectory(localPath.toFile());

        cloneCommand = Git
                .cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(localPath.toFile())
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
    }

    public void executeClone() throws GitAPIException {
        long time = System.currentTimeMillis();
        cloneCommand.call().close();
        log.info("{} - Clone. Timestamp: {}", username, time);
    }

    public void preparePush(int counter) throws IOException, GitAPIException {
        var localPath = Path.of("repos", username);
        git = Git.open(localPath.toFile());
        var changedFile = changeFiles(counter);
        git.add().addFilepattern("src").call();
        git.commit().setMessage("Commit " + counter + " for " + username + " to " + changedFile).setAllowEmpty(true).setSign(false).call();
        pushCommand = git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
        unpushedCommits = git.log().not(git.getRepository().resolve("origin/main")).call();
    }

    public void executePush(int counter) throws GitAPIException, IOException {
        long time = System.currentTimeMillis();
        pushCommand.call()
                .forEach(result -> result.getRemoteUpdates()
                        .forEach(update -> {
                            log.info("{} - Push {}: {} - {}. Timestamp: {}", username, counter, update.getStatus(), update.getMessage(), time);
                            if (update.getStatus() == RemoteRefUpdate.Status.OK) {
                                List<RevCommit> commits = new LinkedList<>();
                                unpushedCommits.forEach(commits::add);
                                successfulPushes.add(new Push(username, commits));
                            }
                        })
                );
        git.close();
    }

    public void updateRepository() throws IOException, GitAPIException {
        var localPath = Path.of("repos", username);
        git = Git.open(localPath.toFile());
        git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).call();
        git.close();
    }

    public String getUsername() {
        return username;
    }

    private String changeFiles(int counter) throws IOException {
        var bubbleSort = Path.of("repos", username, "src", exerciseShortName, "BubbleSort.java");
        var client = Path.of("repos", username, "src", exerciseShortName, "Client.java");
        var mergeSort = Path.of("repos", username, "src", exerciseShortName, "MergeSort.java");
        var fileToChange = Math.random() < 0.5 ? bubbleSort : Math.random() < 0.5 ? client : mergeSort;
        FileUtils.writeLines(fileToChange.toFile(), List.of("//Commit " + counter + " for " + username), true);
        return fileToChange.toString();
    }
}
