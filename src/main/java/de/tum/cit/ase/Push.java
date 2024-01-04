package de.tum.cit.ase;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.List;

public record Push(String username, List<RevCommit> commits) {
}
