package ssw.mj.test.support;

public class Configuration {
  /**
   * set to true to print expected and actual values of all testcases,
   * not only failing ones (prints expected errors, tokens, symbol table, code)
   */
  public static final boolean ALSO_PRINT_SUCCESSFUL_TESTCASES = Boolean.getBoolean("microjava.testcaseOutput");

  /**
   * Set to true to print debug information of the interpreter. Equal to
   * "-debug" on the command line. <br>
   * Remark:<br>
   * This is a lot of output, some test cases might time out, e.g.
   * CodeGenerationTest.fib
   */
  public static final boolean PRINT_INTERPRETER_DEBUG_OUTPUT = Boolean.getBoolean("microjava.interpreterOutput");

  /**
   * Determines the timeout after which a test case should fail automatically.
   * Default: 10 seconds. The default should work for all test cases
   * on most machines.<br>
   * <em>Attention</em>: For most computers it is likely that there is an
   * endless loop in the MicroJava compiler if a test fails for a timeout.
   */
  public static final long TIMEOUT = 10;
}
