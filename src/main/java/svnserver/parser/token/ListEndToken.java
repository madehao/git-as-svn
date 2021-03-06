/**
 * This file is part of git-as-svn. It is subject to the license terms
 * in the LICENSE file found in the top-level directory of this distribution
 * and at http://www.gnu.org/licenses/gpl-2.0.html. No part of git-as-svn,
 * including this file, may be copied, modified, propagated, or distributed
 * except according to the terms contained in the LICENSE file.
 */
package svnserver.parser.token;

import org.jetbrains.annotations.NotNull;
import svnserver.parser.SvnServerToken;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Конец списка.
 *
 * @author Artem V. Navrotskiy <bozaro@users.noreply.github.com>
 */
public final class ListEndToken implements SvnServerToken {
  @NotNull
  public static final ListEndToken instance = new ListEndToken();
  @NotNull
  private static final byte[] TOKEN = {')', ' '};

  private ListEndToken() {
  }

  @Override
  public void write(OutputStream stream) throws IOException {
    stream.write(TOKEN);
  }

  @Override
  public String toString() {
    return "ListEnd";
  }
}
