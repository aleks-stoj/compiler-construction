package ssw.mj;

import com.google.gson.Gson;
import ssw.mj.scanner.Token;

import java.util.*;

public class Recorder {
  /**
   * Result class for Gson serialization
   */
  public static class RecorderTraceEntry {
    public final String in;
    public final String name;
    public final Map<String, String> params;
    public final TraceEntryType type;
    public final TraceEntryOperation operation;
    public final Token laToken;
    public final Token token;
    public final transient RecorderTraceEntry parent; // transient <=> do not serialize

    public RecorderTraceEntry(TraceEntryType type, TraceEntryOperation operation, String in, String name, Map<String, String> params, Token token, Token laToken, RecorderTraceEntry parent) {
      this.type = type;
      this.operation = operation;
      this.in = in;
      this.name = name;
      this.params = params;
      this.token = token;
      this.laToken = laToken;
      this.parent = parent;
    }
  }

  public static final Gson gson = new Gson();

  // Type of operation executed
  public enum TraceEntryOperation {
    enter, exit
  }

  // Type of trace
  public enum TraceEntryType {
    check, error, scan, custom, recover
  }

  public Recorder() {
    this.trace = new ArrayList<>();
  }

  private final List<RecorderTraceEntry> trace;
  private RecorderTraceEntry currentEntry = null;

  private final Map<String, Integer> idIndex = new HashMap<>();

  /**
   * Record a check operation
   *
   * @param laToken  current token
   * @param expected expected token kind
   */
  public void checkEnter(Token token, Token laToken, Token.Kind expected) {
    enter(TraceEntryType.check, "check", token, laToken, Map.of("expected", expected.toString()));
  }

  /**
   * Record a scan operation
   *
   * @param laToken current token
   */
  public void scanEnter(Token token, Token laToken) {
    enter(TraceEntryType.scan, "scan", token, laToken, Map.of());
  }

  /**
   * Record an error operation
   *
   * @param laToken   current token
   * @param error     error message
   * @param msgParams parameters for error message
   */
  public void errorEnter(Token token, Token laToken, Errors.Message error, Object[] msgParams) {
    enter(TraceEntryType.error, "error", token, laToken, Map.of("type", error.toString(), "message", msgParams.length == 0 ? error.format() : error.format(msgParams)));
  }

  /**
   * Record a recover operation, i.e., a method repeatedly scanning to recover from an error
   *
   * @param laToken current token
   * @param name    name of function
   */
  public void recoverEnter(Token token, Token laToken, String name) {
    enter(TraceEntryType.recover, name, token, laToken, Map.of());
  }

  /**
   * Record an enter operation for a custom function, i.e., a grammar rule
   *
   * @param laToken current token
   * @param name    name of function
   */
  public void customEnter(Token token, Token laToken, String name) {
    enter(TraceEntryType.custom, name, token, laToken, Map.of());
  }

  /**
   * Record an enter trace for an arbitrary operation
   *
   * @param type    type of operation
   * @param name    name of function
   * @param laToken current token
   * @param params  parameters of function
   */
  private void enter(TraceEntryType type, String name, Token token, Token laToken, Map<String, String> params) {
    String in = currentEntry == null ? null : currentEntry.name;

    currentEntry = new RecorderTraceEntry(type, TraceEntryOperation.enter, in, getName(name), params, token, laToken, currentEntry);

    trace.add(currentEntry);
  }

  /**
   * Record an exit trace for the current operation
   *
   * @param laToken current token
   */
  public void exit(Token token, Token laToken) {
    if (currentEntry == null) {
      throw new IllegalStateException("Cannot exit trace, no trace is active");
    }

    trace.add(new RecorderTraceEntry(currentEntry.type, TraceEntryOperation.exit, currentEntry.in, currentEntry.name, currentEntry.params, token, laToken, null));

    currentEntry = currentEntry.parent;
  }

  /**
   * Reset the recorder
   */
  public void reset() {
    trace.clear();
    currentEntry = null;
    idIndex.clear();
  }

  public List<RecorderTraceEntry> getTrace() {
    return trace;
  }

  private String getName(String name) {
    if (idIndex.containsKey(name)) {
      idIndex.put(name, idIndex.get(name) + 1);
    } else {
      idIndex.put(name, 1);
    }

    return name + "$" + idIndex.get(name);
  }
}
