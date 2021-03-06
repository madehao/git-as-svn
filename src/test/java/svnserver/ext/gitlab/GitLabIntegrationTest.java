/**
 * This file is part of git-as-svn. It is subject to the license terms
 * in the LICENSE file found in the top-level directory of this distribution
 * and at http://www.gnu.org/licenses/gpl-2.0.html. No part of git-as-svn,
 * including this file, may be copied, modified, propagated, or distributed
 * except according to the terms contained in the LICENSE file.
 */
package svnserver.ext.gitlab;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.Wait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;
import svnserver.SvnTestServer;
import svnserver.config.RepositoryMappingConfig;
import svnserver.ext.gitlab.auth.GitLabUserDBConfig;
import svnserver.ext.gitlab.config.GitLabConfig;
import svnserver.ext.gitlab.config.GitLabContext;
import svnserver.ext.gitlab.config.GitLabToken;
import svnserver.ext.gitlab.mapping.GitLabMappingConfig;
import svnserver.repository.git.GitCreateMode;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

/**
 * @author Marat Radchenko <marat@slonopotamus.org>
 */
public final class GitLabIntegrationTest {

  @NotNull
  private static final String root = "root";
  @NotNull
  private static final String rootPassword = "12345678";

  @NotNull
  private static final String user = "git-as-svn";
  @NotNull
  private static final String userPassword = "git-as-svn";

  private GenericContainer<?> gitlab;
  private String gitlabUrl;
  private GitLabToken rootToken;
  private GitlabProject gitlabProject;
  private GitlabProject gitlabPublicProject;

  @BeforeClass
  void before() throws Exception {
    String gitlabVersion = System.getenv("GITLAB_VERSION");
    if (gitlabVersion == null)
      gitlabVersion = "9.3.3-ce.0";

    final int gitlabPort = 80;

    gitlab = new GenericContainer<>("gitlab/gitlab-ce:" + gitlabVersion)
        .withEnv("GITLAB_ROOT_PASSWORD", rootPassword)
        .withExposedPorts(gitlabPort)
        .waitingFor(Wait.forHttp("/users/sign_in")
            .withStartupTimeout(Duration.of(10, ChronoUnit.MINUTES)));
    gitlab.start();
    gitlabUrl = "http://" + gitlab.getContainerIpAddress() + ":" + gitlab.getMappedPort(gitlabPort);

    rootToken = createToken(root, rootPassword, true);

    final GitlabAPI rootAPI = GitLabContext.connect(gitlabUrl, rootToken);

    final GitlabUser gitlabUser = rootAPI.createUser("git-as-svn@localhost", userPassword, user, user, null, null, null, null, null, null, null, null, false, null, true);
    Assert.assertNotNull(gitlabUser);

    final GitlabGroup group = rootAPI.createGroup("testGroup");
    Assert.assertNotNull(group);

    Assert.assertNotNull(rootAPI.addGroupMember(group.getId(), gitlabUser.getId(), GitlabAccessLevel.Developer));

    gitlabProject = createGitlabProject(rootAPI, group, "test", false);
    gitlabPublicProject = createGitlabProject(rootAPI, group, "publik", true);
  }

  @NotNull
  private GitLabToken createToken(@NotNull String username, @NotNull String password, boolean sudoScope) throws IOException {
    return GitLabContext.obtainAccessToken(gitlabUrl, username, password, sudoScope);
  }

  @NotNull
  private GitlabProject createGitlabProject(@NotNull GitlabAPI rootAPI, @NotNull GitlabGroup group, @NotNull String name, boolean publik) throws IOException {
    return rootAPI.createProject(name, group.getId(), null, null, null, null, null, null, publik, null, null);
  }

  @AfterClass
  void after() {
    if (gitlab != null) {
      gitlab.stop();
      gitlab = null;
    }
  }

  @Test
  void validUser() throws Exception {
    checkUser(root, rootPassword);
  }

  private void checkUser(@NotNull String login, @NotNull String password) throws Exception {
    try (SvnTestServer server = createServer(rootToken, null)) {
      server.openSvnRepository(login, password).getLatestRevision();
    }
  }

  @NotNull
  private SvnTestServer createServer(@NotNull GitLabToken token, @Nullable Function<File, RepositoryMappingConfig> mappingConfigCreator) throws Exception {
    final GitLabConfig gitLabConfig = new GitLabConfig(gitlabUrl, token);
    return SvnTestServer.createEmpty(new GitLabUserDBConfig(), mappingConfigCreator, false, gitLabConfig);
  }

  @Test(expectedExceptions = SVNAuthenticationException.class)
  void invalidPassword() throws Throwable {
    checkUser(root, "wrongpassword");
  }

  @Test(expectedExceptions = SVNAuthenticationException.class)
  void invalidUser() throws Throwable {
    checkUser("wronguser", rootPassword);
  }

  @Test
  void gitlabMappingAsRoot() throws Exception {
    try (SvnTestServer server = createServer(rootToken, dir -> new GitLabMappingConfig(dir, GitCreateMode.EMPTY))) {
      openSvnRepository(server, gitlabProject, user, userPassword).getLatestRevision();
    }
  }

  @NotNull
  private SVNRepository openSvnRepository(@NotNull SvnTestServer server, @NotNull GitlabProject gitlabProject, @NotNull String username, @NotNull String password) throws SVNException {
    return SvnTestServer.openSvnRepository(server.getUrl().appendPath(gitlabProject.getPathWithNamespace(), false), username, password);
  }

  @Test
  void gitlabMappingForAnonymous() throws Throwable {
    try (SvnTestServer server = createServer(rootToken, dir -> new GitLabMappingConfig(dir, GitCreateMode.EMPTY))) {
      openSvnRepository(server, gitlabPublicProject, "nobody", "nopassword").getLatestRevision();
    }
  }

  /**
   * Test for #119.
   */
  @Ignore
  @Test
  void gitlabMappingAsUser() throws Exception {
    final GitLabToken userToken = createToken(user, userPassword, false);

    try (SvnTestServer server = createServer(userToken, dir -> new GitLabMappingConfig(dir, GitCreateMode.EMPTY))) {
      openSvnRepository(server, gitlabProject, root, rootPassword).getLatestRevision();
    }
  }
}
