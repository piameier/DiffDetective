package org.variantsync.diffdetective.datasets;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.tinylog.Logger;

/**
 * A functional interface for listing the commits in a {@link Repository}.
 * Apart from listing a fixed set of commits, this is mainly useful to configure a {@link org.eclipse.jgit.api.Git#log() Git log} call.
 *<p>
 * This is mainly intended to be used as an argument for the {@code listCommits} argument of
 * {@link Repository#Repository(RepositoryLocationType, Path, URI, String, Function<Repository, Iterator<RevCommit>>, PatchDiffParseOptions, DiffFilter)}.
 */
@FunctionalInterface
public interface CommitLister {
    Iterator<RevCommit> listCommits(Repository repository);

    /**
     * List all commits reachable by the current {@code HEAD} of the repository.
     */
    public static final CommitLister TraverseHEAD =
        (Repository repository) -> {
            try {
                return repository.getGitRepo().log().call().iterator();
            } catch (GitAPIException e) {
                Logger.warn("Could not get log for git repository {}", repository.getRepositoryName());
                throw new RuntimeException(e);
            }
        };

    /**
     * List all commits reachable from all branches of the repository.
     */
    public static final CommitLister AllCommits =
        (Repository repository) -> {
            try {
                return repository.getGitRepo().log().all().call().iterator();
            } catch (GitAPIException | IOException e) {
                Logger.warn("Could not get log for git repository {}", repository.getRepositoryName());
                throw new RuntimeException(e);
            }
        };
}
