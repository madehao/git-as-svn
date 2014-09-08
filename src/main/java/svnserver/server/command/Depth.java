package svnserver.server.command;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * @author Marat Radchenko <marat@slonopotamus.org>
 */
public enum Depth {
  Empty {
    @NotNull
    @Override
    public Action determineAction(@NotNull Depth requestedDepth, boolean directory) {
      if (requestedDepth == Immediates || requestedDepth == Infinity)
        return Action.Upgrade;

      return Action.Skip;
    }
  },

  Files {
    @NotNull
    @Override
    public Action determineAction(@NotNull Depth requestedDepth, boolean directory) {
      if (directory)
        return requestedDepth == Immediates || requestedDepth == Infinity ? Action.Upgrade : Action.Skip;

      return requestedDepth == Empty ? Action.Skip : Action.Normal;
    }
  },

  Immediates {
    @NotNull
    @Override
    public Action determineAction(@NotNull Depth requestedDepth, boolean directory) {
      if (directory)
        return requestedDepth == Empty || requestedDepth == Files ? Action.Skip : Action.Normal;

      return requestedDepth == Empty ? Action.Skip : Action.Normal;
    }
  },

  Infinity {
    @NotNull
    @Override
    public Action determineAction(@NotNull Depth requestedDepth, boolean directory) {
      if (directory)
        return requestedDepth == Empty || requestedDepth == Files ? Action.Skip : Action.Normal;

      return requestedDepth == Empty ? Action.Skip : Action.Normal;
    }
  };

  @NotNull
  private final String value = name().toLowerCase(Locale.ENGLISH);

  @NotNull
  public static Depth parse(@NotNull String value) {
    for (Depth depth : values())
      if (depth.value.equals(value))
        return depth;

    return Infinity;
  }

  @NotNull
  public static Depth parse(@NotNull String value, boolean recurse, @NotNull Depth nonRecurse) {
    if (value.isEmpty())
      return recurse ? Infinity : nonRecurse;

    return parse(value);
  }

  @NotNull
  public abstract Action determineAction(@NotNull Depth requestedDepth, boolean directory);

  public enum Action {
    Skip,
    Upgrade,
    Normal;
  }

  @NotNull
  public final Depth deepen() {
    return this == Immediates ? Empty : this;
  }
}
