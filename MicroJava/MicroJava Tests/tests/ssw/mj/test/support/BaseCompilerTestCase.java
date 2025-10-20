package ssw.mj.test.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import ssw.mj.Errors;
import ssw.mj.Interpreter;
import ssw.mj.Visualizer;
import ssw.mj.codegen.Decoder;
import ssw.mj.impl.Parser;
import ssw.mj.impl.Scanner;
import ssw.mj.scanner.Token;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base class for test cases with utility methods used by all tests.
 */

@Timeout(value = Configuration.TIMEOUT, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
public abstract class BaseCompilerTestCase {

  public static final String CR = "\r";
  public static final String LF = "\n";
  private List<String> expectedErrors;
  private List<String> expectedTokens;
  private List<Token> expectedTokensFull;
  private List<String> expectedSymTab;
  private List<String> expectedRuntimeErrors;
  private String source;
  private Scanner scanner;
  protected Parser parser;
  private String callingClassAndMethod;
  private final List<String> runInputs = new ArrayList<>();
  private final List<String> expectedOutputs = new ArrayList<>();

  @BeforeEach
  public void setUp() {
    // initialize expected compiler output
    expectedErrors = new ArrayList<>();
    expectedTokens = new ArrayList<>();
    expectedTokensFull = new ArrayList<>();
    expectedSymTab = new ArrayList<>();
    expectedRuntimeErrors = new ArrayList<>();

    if (Configuration.ALSO_PRINT_SUCCESSFUL_TESTCASES) {
      // print header for console output
      System.out.println("--------------------------------------------------");
    }
  }

  protected void initCode(String code) {
    initScannerCode(code);
    parser = new Parser(scanner);
  }

  protected void initFile(String filename) {
    initScannerFile(filename);
    parser = new Parser(scanner);
  }

  protected void initScannerCode(String code) {
    source = code;
    scanner = new Scanner(new StringReader(code));
  }

  protected void initScannerFile(String filename) {
    try {
      ClassLoader classLoader = getClass().getClassLoader();
      URL resource = classLoader.getResource(filename);
      if (resource == null) {
        throw new RuntimeException("resource %s not found".formatted(filename));
      }
      String urlAsStr = resource.getFile();
      // replaces %20 Urlencoding with " " (blank space), as e.g. Linux cannot handle url paths
      String path = URLDecoder.decode(urlAsStr, StandardCharsets.UTF_8);
      File file = new File(path);
      scanner = new Scanner(new FileReader(file));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  private List<String> splitString(String s) {
    StringTokenizer st = new StringTokenizer(s, "\n");
    List<String> result = new ArrayList<>();
    while (st.hasMoreTokens()) {
      result.add(st.nextToken());
    }
    return result;
  }

  private void print(String title, List<String> expected, List<String> actual) {
    if (expected.isEmpty() && actual.isEmpty()) {
      return;
    }
    System.out.format("%s - %s\n", callingClassAndMethod, title);
    if (Configuration.ALSO_PRINT_SUCCESSFUL_TESTCASES || !expected.equals(actual)) {
      System.out.format("  %-60s %s\n", "expected", "actual");
      int lines = Math.max(expected.size(), actual.size());
      for (int i = 0; i < lines; i++) {
        String expectedLine = (i < expected.size() ? expected.get(i) : "");
        String actualLine = (i < actual.size() ? actual.get(i) : "");
        System.out.format("%s %-60s %s\n", (expectedLine.equals(actualLine) ? " " : "x"), expectedLine,
                actualLine);
      }
    } else {
      if (expected.equals(actual)) {
        System.out.println("  correct (exact comparison hidden, enable via Configuration.ALSO_PRINT_SUCCESSFUL_TESTCASES)");
      }
    }
  }

  private void addRun(String input, String output, String error) {
    runInputs.add(input);
    expectedOutputs.add(output);
    expectedRuntimeErrors.add(error);
  }

  protected void addExpectedRun(String output) {
    addExpectedRun("", output);
  }

  protected void addExpectedRun(String input, String output) {
    addRun(input, output, "");
  }

  protected void addFailingRun(String error) {
    addFailingRun("", error);
  }

  protected void addFailingRun(String input, String error) {
    addRun(input, "", error);
  }

  /**
   * Scans the given code and checks the scanned tokens against the expected ones.
   * Also checks that expected errors occur.
   * Finally, the method creates a visualization of the scanned tokens if the test was run
   * with @link ssw.mj.TracingClassLoader as system classloader.
   */
  protected void scanVerifyVisualize() {
    callingClassAndMethod = getCallingClassAndMethod(1);

    List<Token> actualTokens = new ArrayList<>();

    // scan only the expected number of tokens to prevent endless loops
    for (int i = 0; i < getExpectedTokens().size(); i++) {
      actualTokens.add(scanner.next());
    }

    List<String> actualTokenStrings = actualTokens.stream().map(Token::toString).toList();

    Visualizer.createScannerVisualization(source, actualTokens, getExpectedTokensFull(), false);

    printErrors();
    printTokens(actualTokenStrings);

    verifyErrors();
    verifyTokens(actualTokenStrings);
  }

  /**
   * Parses the given code and checks it for expected errors, matching sym tab and matching byte code.
   * Then it executed the interpreter for all given inputs.
   * Finally, the method creates a visualization of the parse tree if the test was run
   * with @link ssw.mj.TracingClassLoader as system classloader.
   */
  protected void parseVerifyVisualize() {
    callingClassAndMethod = getCallingClassAndMethod(1);

    try {
      parser.parse();
      assertEquals(Token.Kind.eof, scanner.next().kind, "Complete input should be scanned");
    } catch (Errors.PanicMode error) {
      // Ignore, nothing to do
    }

    printErrors();
    printSymTab();

    verifyErrors();
    verifySymTab();

    if (ByteCodeTestSupport.GENERATE_REFERENCE_BYTE_CODE && expectedErrors.isEmpty()) {
      ByteCodeTestSupport.generateReferenceByteCode(callingClassAndMethod, parser);
    } else {
      printAndVerifyByteCode(callingClassAndMethod);
    }

    for (int i = 0; i < runInputs.size(); i++) {
      run(i);
    }

    Visualizer.createParserVisualization(source, false);
  }

  private static String getCallingClassAndMethod(int up) {
    // [0] getStackTrace -> [1] getCallingMethodName -> [2] caller of getCallingMethodName -> [3] ...
    StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
    StackTraceElement e = stacktrace[2 + up];
    String fullyQualifiedClassName = e.getClassName();
    String className = fullyQualifiedClassName.substring(Math.max(fullyQualifiedClassName.lastIndexOf(".") + 1, 0));
    return className + "." + e.getMethodName() + "()";
  }

  private void run(int i) {
    Interpreter.BufferIO io = new Interpreter.BufferIO(runInputs.get(i));
    Interpreter interpreter = new Interpreter(
            parser.code.buf,
            parser.code.mainpc,
            parser.code.dataSize,
            io,
            Configuration.PRINT_INTERPRETER_DEBUG_OUTPUT);
    try {
      interpreter.run();
    } catch (IllegalStateException e) {
      verifyRuntimeError(i, e);
    }
    String output = io.getOutput();
    verifyOutput(i, output);
  }


  private void printErrors() {
    print("Errors", expectedErrors, getActualErrors());
  }

  private void printTokens(List<String> actualTokens) {
    print("Tokens", getExpectedTokens(), actualTokens);
  }

  private void printSymTab() {
    if (!expectedSymTab.isEmpty()) {
      print("Symbol Table", getExpectedSymTab(), getActualSymTab());
    }
  }

  private void verifyErrors() {
    assertEquals(expectedErrors, getActualErrors(), "Errors");
  }

  private void verifyTokens(List<String> actualTokens) {
    assertEquals(getExpectedTokens(), actualTokens, "Tokens");
    assertTrue(scanner.next().toString().contains("end of file"), "Complete Input Scanned");
  }

  private void verifySymTab() {
    if (!expectedSymTab.isEmpty()) {
      assertEquals(getExpectedSymTab(), getActualSymTab(), "Symbol Table");
    }
  }

  private void printAndVerifyByteCode(String callingClassAndMethod) {
    if (ByteCodeTestSupport.BYTE_CODES.containsKey(callingClassAndMethod)) {
      List<String> possibleByteCodes = ByteCodeTestSupport.BYTE_CODES.get(callingClassAndMethod);
      if (possibleByteCodes.size() == 1) {
        List<String> expected = getExpectedByteCodeLines(possibleByteCodes.get(0));
        print("Bytecode", expected, getActualByteCodeLines());
        // Verify that the bytecode is correct
        assertEquals(expected, getActualByteCodeLines(), "Byte Code");
      } else {
        int matchIdx = -1;
        for (int i = 0; i < possibleByteCodes.size(); i++) {
          List<String> expected = getExpectedByteCodeLines(possibleByteCodes.get(i));
          if (expected.equals(getActualByteCodeLines())) {
            matchIdx = i;
            break;
          }
        }
        if (matchIdx < 0) {
          // No bytecode matched
          // print all
          for (int i = 0; i < possibleByteCodes.size(); i++) {
            List<String> expected = getExpectedByteCodeLines(possibleByteCodes.get(i));
            print("Possible Bytecode %d".formatted(i + 1), expected, getActualByteCodeLines());
          }
          // fail assert on first
          assertEquals(getExpectedByteCodeLines(possibleByteCodes.get(0)), getActualByteCodeLines(), "Byte Code");
        } else {
          // bytecode at idx matchIdx correctly generated
          // print working bytecode
          print("Bytecode", getExpectedByteCodeLines(possibleByteCodes.get(matchIdx)), getActualByteCodeLines());
          // assert not really necessary since we already know we matched successfully
          assertEquals(getExpectedByteCodeLines(possibleByteCodes.get(matchIdx)), getActualByteCodeLines(), "Byte Code");
        }
      }
    }
  }

  private void verifyOutput(int runIdx, String actualOutput) {
    assertEquals(expectedOutputs.get(runIdx), actualOutput, "Unexpected result when input is \"" + runInputs.get(runIdx) + "\": ");
  }

  private void verifyRuntimeError(int runIdx, IllegalStateException e) {
    assertEquals(expectedRuntimeErrors.get(runIdx), e.getMessage(), "Unexpected runtime error message when input is \"" + runInputs.get(runIdx) + "\": ");
  }

  private List<String> getExpectedByteCodeLines(String bytecode) {
    return Arrays.stream(bytecode.split("\n")).toList();
  }

  private List<String> getActualByteCodeLines() {
    return Arrays.stream(new Decoder().decode(parser.code).split("\n")).toList();
  }

  private List<String> getActualErrors() {
    return splitString(scanner.errors.dump());
  }

  private List<String> getExpectedTokens() {
    return expectedTokens;
  }

  private List<Token> getExpectedTokensFull() {
    return expectedTokensFull;
  }

  private List<String> getExpectedSymTab() {
    return expectedSymTab;
  }

  private List<String> getActualSymTab() {
    return splitString(SymTabDumper.dump(parser.tab));
  }

  protected void expectError(int line, int col, Errors.Message msg, Object... msgParams) {
    expectedErrors.add("-- line " + line + " col " + col + ": " + msg.format(msgParams));
  }

  protected void expectToken(Token.Kind kind, int line, int col) {
    expectedTokens.add("line " + line + ", col " + col + ", kind " + kind);
    expectedTokensFull.add(new Token(kind, line, col));
  }

  protected void expectToken(Token.Kind kind, int line, int col, String val) {
    expectedTokens.add("line " + line + ", col " + col + ", kind " + kind + ", val " + val);

    Token token = new Token(kind, line, col);
    token.val = val;
    expectedTokensFull.add(token);
  }

  protected void expectToken(Token.Kind kind, int line, int col, int val) {
    expectedTokens.add("line " + line + ", col " + col + ", kind " + kind + ", val " + val + ", numVal " + val);

    Token token = new Token(kind, line, col);
    token.val = String.valueOf(val);
    token.numVal = val;
    expectedTokensFull.add(token);
  }

  protected void expectToken(Token.Kind kind, int line, int col, char ch) {
    expectedTokens.add("line " + line + ", col " + col + ", kind " + kind + ", val " + ch + ", numVal " + (int) ch);

    Token token = new Token(kind, line, col);
    token.val = String.valueOf(ch);
    token.numVal = ch;
    expectedTokensFull.add(token);
  }

  protected void expectInvalidToken(Token.Kind kind, int line, int col) {
    expectedTokens.add("line " + line + ", col " + col + ", kind " + kind + ", val null, numVal 0");

    Token token = new Token(kind, line, col);
    token.val = null;
    token.numVal = 0;
    expectedTokensFull.add(token);
  }

  protected void expectSymTab(String line) {
    expectedSymTab.add(line);
  }

  protected void expectSymTabUniverse() {
    // first part of the symbol table (universe) that is equal for all
    // programs
    expectSymTab("-- begin scope (0 variables) --");
    expectSymTab("Type int: int");
    expectSymTab("Type char: char");
    expectSymTab("Constant: class (0 fields) null = 0");
    expectSymTab("Method: char chr (1 locals, 1 parameters)");
    expectSymTab("  Local Variable 0: int i");
    expectSymTab("Method: int ord (1 locals, 1 parameters)");
    expectSymTab("  Local Variable 0: char ch");
    expectSymTab("Method: int len (1 locals, 1 parameters)");
    expectSymTab("  Local Variable 0: void[] arr");
  }
}
