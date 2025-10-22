package ssw.mj;

import java.io.Serial;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class Errors {
  public static class PanicMode extends Error {
    @Serial
    private static final long serialVersionUID = 1L;
    // Nothing to implement here.
  }

  public enum Message {
    // ----- error messages first used in ScannerTest
    EMPTY_CHARCONST("empty character constant"),
    UNDEFINED_ESCAPE("undefined escape character sequence ''\\{0}''"),
    MISSING_QUOTE("missing '' at end of character constant"),
    INVALID_CHAR("invalid character {0}"),
    BIG_NUM("{0} too big for integer constant"),
    EOF_IN_COMMENT("unexpected end of file in comment"),
    EOF_IN_CHAR("unexpected end of file in char"),
    ILLEGAL_LINE_END("illegal line end in character constant"),

    // ----- error messages first used in ParserTest
    INVALID_ADD_OP("unexpected token. + or - expected"), // cannot occur in current grammar version, but still must be placed correctly (in default case of respective switch)
    INVALID_ASSIGN_OP("unexpected token. =, +=, -=, *=, /=, %= expected"), // cannot occur in current grammar version, but still must be placed correctly (in default case of respective switch)
    INVALID_MUL_OP("unexpected token. *, /, % expected"), // cannot occur in current grammar version, but still must be placed correctly (in default case of respective switch)
    INVALID_METHOD_DECL("invalid start of method decl: type name or void expected"), // cannot occur in current grammar version, but still must be placed correctly (in default case of respective switch)
    INVALID_STATEMENT("unexpected token. identifier, if, while, break, return, read, print, '{' or ; expected"), // cannot occur in current grammar version, but still must be placed correctly (in default case of respective switch)
    INVALID_DESIGNATOR_STATEMENT("unexpected token. assignment token (=, +=, -=, *=, /=, %=), method call (\"(\"), increment (++) or decrement (--) expected"),
    INVALID_CONST_TYPE("number or character constant expected"),
    INVALID_FACTOR("unexpected token. identifier, number, character constant, new or \"(\" expected"),
    INVALID_REL_OP("unexpected token. ==, !=, >, >=, <, <= expected"),
    TOKEN_EXPECTED("{0} expected"),

    // ----- error messages first used in RecoverTest
    DECLARATION_RECOVERY("start or follow of declaration expected"),
    METHOD_DECL_RECOVERY("start or follow of method declaration expected"),
    STATEMENT_RECOVERY("start or follow of statement expected"),

    // ----- error messages first used in SymbolTableTest
    INCOMPATIBLE_TYPES("incompatible types"), // mainly used in SimpleCodeGenerationTest, but also in SymbolTableTest
    DUPLICATE_NAME_IN_SCOPE("{0} already declared in current scope"),
    MAIN_WITH_PARAMS("main method must not have any parameters"),
    MAIN_NOT_VOID("main method must return void"),
    FIELD_NOT_FOUND("{0} is not a field"),
    TYPE_EXPECTED("type expected"),
    NAME_NOT_FOUND("{0} not found"),
    TOO_MANY_FIELDS("too many fields"),
    TOO_MANY_GLOBALS("too many global variables"),
    TOO_MANY_LOCALS("too many local variables"),
    CLASS_TYPE_EXPECTED("can only instantiate new object for a class"),

    // ----- error messages first used in SimpleCodeGenerationTest
    ARRAY_INDEX_EXPECTS_INT("array index must be an integer"),
    ARRAY_SIZE_EXPECTS_INT("array size must be an integer"),
    CANNOT_STORE_TO_READONLY("cannot store to readonly operand of kind {0}"),
    VOID_CALL_IN_EXPRESSION("cannot use void method as part of expression"),
    MAIN_NOT_FOUND("mainPC is -1, main not found (did you forget to set code.mainPC? ;))"),
    INDEXED_ACCESS_TO_NON_ARRAY("indexed object is not an array"),
    FIELD_ACCESS_TO_NON_CLASS("accessed object is not of kind class"),
    ILLEGAL_OPERAND_KIND("cannot create operand symbol table object of type {0}"),
    CANNOT_LOAD_OPERAND("already loaded (stack) or loadable operand (const, local, static, field, array element) expected"),
    ILLEGAL_PRINT_ARGUMENT("can only print int or char values"),
    ILLEGAL_READ_ARGUMENT("can only read int or char values"),
    INC_DEC_EXPECTS_INT("increment and decrement only allowed for int"),
    UNARY_MINUS_EXPECTS_INT("unary minus only allowed for int"),

    // ----- error messages first used in CodeGenerationTest
    ILLEGAL_REFERENCE_COMPARISON("only equality and unequality checks are allowed for reference types"),
    ILLEGAL_METHOD_RETURN_TYPE("methods may only return int or char"),
    WRONG_ARGUMENT_COUNT("number of arguments and formal parameters does not match"),
    BREAK_OUTSIDE_LOOP("break is not within a loop"),
    CALL_TO_NON_METHOD("called object is not a method"),
    ARGUMENT_TYPE_MISMATCH("argument type does not match formal parameter type"),
    MISSING_RETURN_VALUE("return expression required in non-void method"),
    UNEXPECTED_RETURN_VALUE("no return expression allowed in void method"),
    RETURN_TYPE_MISMATCH("return type must match method type");

    private final String msg;

    Message(String msg) {
      this.msg = msg;
    }

    public String format(Object... params) {
      int expectedParams = 0;
      while (msg.contains("{" + expectedParams + "}")) {
        expectedParams++;
      }
      if (params.length != expectedParams) {
        throw new Error("incorrect number of error message parameters. Expected %d but got %d".formatted(expectedParams, params.length));
      }
      return MessageFormat.format(msg, params);
    }
  }

  /**
   * List of error messages.
   */
  private final List<String> errors;

  /**
   * Initialization (must be called before compilation).
   */
  public Errors() {
    errors = new ArrayList<>();
  }

  /**
   * Add a new error message to the list of errors.
   */
  public void error(int line, int col, Message msg, Object... msgParams) {
    errors.add("-- line " + line + " col " + col + ": " + msg.format(msgParams));
  }

  /**
   * Returns the number of errors.
   */
  public int numErrors() {
    return errors.size();
  }

  /**
   * String representation for JUnit test cases.
   */
  public String dump() {
    StringBuilder sb = new StringBuilder();
    for (String error : errors) {
      sb.append(error).append("\n");
    }
    return sb.toString();
  }

  public List<String> getErrors() {
    return errors;
  }
}
