package svnserver.server;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;
import org.yaml.snakeyaml.Yaml;
import svnserver.config.Config;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Entry point.
 *
 * @author a.navrotskiy
 */
public class Main {
  @NotNull
  private static final Logger log = LoggerFactory.getLogger(SvnServer.class);

  public static void main(@NotNull String[] args) throws IOException, SVNException {
    final CmdArgs cmd = new CmdArgs();
    final JCommander jc = new JCommander(cmd);
    jc.parse(args);
    if (cmd.help) {
      jc.usage();
      return;
    }
    // Load config
    Yaml yaml = new Yaml();
    Config config;
    try (
        InputStream stream = new FileInputStream(cmd.configuration);
        Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)
    ) {
      config = yaml.loadAs(reader, Config.class);
    }
    if (cmd.showConfig) {
      log.info("Actual config:\n{}", yaml.dump(config));
    }
    new SvnServer(config).start();
  }

  public static class CmdArgs {
    @Parameter(names = {"-c", "--config"}, description = "Configuration file name", required = true)
    @NotNull
    private File configuration;

    @Parameter(names = {"--show-config"}, description = "Show actual configuration on start")
    private boolean showConfig = false;

    @Parameter(names = {"-h", "--help"}, description = "Show help", help = true)
    private boolean help = false;
  }

}
