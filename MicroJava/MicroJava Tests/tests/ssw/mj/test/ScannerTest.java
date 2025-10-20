package ssw.mj.test;

import org.junit.jupiter.api.Test;
import ssw.mj.test.support.BaseCompilerTestCase;

import static ssw.mj.Errors.Message.*;
import static ssw.mj.scanner.Token.Kind.*;

public class ScannerTest extends BaseCompilerTestCase {
  private static final char invalidChar = (char) 65533;

  @Test
  public void oneToken() {
    initScannerCode(";");

    expectToken(semicolon, 1, 1);
    expectToken(eof, 1, 2);

    scanVerifyVisualize();
  }

  @Test
  public void twoTokens() {
    initScannerCode(";;");

    expectToken(semicolon, 1, 1);
    expectToken(semicolon, 1, 2);
    expectToken(eof, 1, 3);

    scanVerifyVisualize();
  }

  @Test
  public void space() {
    initScannerCode(";  ;");

    expectToken(semicolon, 1, 1);
    expectToken(semicolon, 1, 4);
    expectToken(eof, 1, 5);

    scanVerifyVisualize();
  }

  @Test
  public void tabulator() {
    initScannerCode(";\t\t;");

    expectToken(semicolon, 1, 1);
    expectToken(semicolon, 1, 4);
    expectToken(eof, 1, 5);

    scanVerifyVisualize();
  }

  @Test
  public void noToken() {
    initScannerCode("");

    expectToken(eof, 1, 1);

    scanVerifyVisualize();
  }

  @Test
  public void crLfLineSeparators() {
    initScannerCode(";" + CR + LF + " ;" + CR + LF + "  ; ");

    expectToken(semicolon, 1, 1);
    expectToken(semicolon, 2, 2);
    expectToken(semicolon, 3, 3);
    expectToken(eof, 3, 5);

    scanVerifyVisualize();
  }

  @Test
  public void lFLineSeparators() {
    initScannerCode(";" + LF + " ;" + LF + "  ; ");

    expectToken(semicolon, 1, 1);
    expectToken(semicolon, 2, 2);
    expectToken(semicolon, 3, 3);
    expectToken(eof, 3, 5);

    scanVerifyVisualize();
  }

  @Test
  public void invalidChar1() {
    initScannerCode(" {" + invalidChar + "} ");

    expectToken(lbrace, 1, 2);
    expectToken(none, 1, 3);
    expectError(1, 3, INVALID_CHAR, invalidChar);
    expectToken(rbrace, 1, 4);
    expectToken(eof, 1, 6);

    scanVerifyVisualize();
  }

  @Test
  public void invalidChar2() {
    initScannerCode(" {\0} ");

    expectToken(lbrace, 1, 2);
    expectToken(none, 1, 3);
    expectError(1, 3, INVALID_CHAR, '\0');
    expectToken(rbrace, 1, 4);
    expectToken(eof, 1, 6);

    scanVerifyVisualize();
  }

  @Test
  public void invalidChar3() {
    initScannerCode(" {&} ");

    expectToken(lbrace, 1, 2);
    expectToken(none, 1, 3);
    expectError(1, 3, INVALID_CHAR, '&');
    expectToken(rbrace, 1, 4);
    expectToken(eof, 1, 6);

    scanVerifyVisualize();
  }

  @Test
  public void invalidChar4() {
    initScannerCode(" {|} ");

    expectToken(lbrace, 1, 2);
    expectToken(none, 1, 3);
    expectError(1, 3, INVALID_CHAR, '|');
    expectToken(rbrace, 1, 4);
    expectToken(eof, 1, 6);

    scanVerifyVisualize();
  }

  @Test
  public void invalidChar5() {
    initScannerCode(" {!} ");

    expectToken(lbrace, 1, 2);
    expectToken(none, 1, 3);
    expectError(1, 3, INVALID_CHAR, '!');
    expectToken(rbrace, 1, 4);
    expectToken(eof, 1, 6);

    scanVerifyVisualize();
  }

  @Test
  public void invalidChar6() {
    initScannerCode(" {ident" + invalidChar + "} ");

    expectToken(lbrace, 1, 2);
    expectToken(ident, 1, 3, "ident");
    expectToken(none, 1, 8);
    expectError(1, 8, INVALID_CHAR, invalidChar);
    expectToken(rbrace, 1, 9);
    expectToken(eof, 1, 11);

    scanVerifyVisualize();
  }

  @Test
  public void ident() {
    initScannerCode(" {i I i1 i_ i1I_i} ");

    expectToken(lbrace, 1, 2);
    expectToken(ident, 1, 3, "i");
    expectToken(ident, 1, 5, "I");
    expectToken(ident, 1, 7, "i1");
    expectToken(ident, 1, 10, "i_");
    expectToken(ident, 1, 13, "i1I_i");
    expectToken(rbrace, 1, 18);
    expectToken(eof, 1, 20);

    scanVerifyVisualize();
  }

  @Test
  public void indentSepararator() {
    initScannerCode(" {i[i<i0i_i>i]i} ");

    expectToken(lbrace, 1, 2);
    expectToken(ident, 1, 3, "i");
    expectToken(lbrack, 1, 4);
    expectToken(ident, 1, 5, "i");
    expectToken(lss, 1, 6);
    expectToken(ident, 1, 7, "i0i_i");
    expectToken(gtr, 1, 12);
    expectToken(ident, 1, 13, "i");
    expectToken(rbrack, 1, 14);
    expectToken(ident, 1, 15, "i");
    expectToken(rbrace, 1, 16);
    expectToken(eof, 1, 18);

    scanVerifyVisualize();
  }

  @Test
  public void singleIdent() {
    initScannerCode("i");

    expectToken(ident, 1, 1, "i");
    expectToken(eof, 1, 2);

    scanVerifyVisualize();
  }

  @Test
  public void number() {
    initScannerCode(" {123 2147483647} ");

    expectToken(lbrace, 1, 2);
    expectToken(number, 1, 3, 123);
    expectToken(number, 1, 7, 2147483647);
    expectToken(rbrace, 1, 17);
    expectToken(eof, 1, 19);

    scanVerifyVisualize();
  }

  @Test
  public void singleNumber() {
    initScannerCode("123");

    expectToken(number, 1, 1, 123);
    expectToken(eof, 1, 4);

    scanVerifyVisualize();
  }

  @Test
  public void negativeNumber() {
    initScannerCode(" {-123} ");

    expectToken(lbrace, 1, 2);
    expectToken(minus, 1, 3);
    expectToken(number, 1, 4, 123);
    expectToken(rbrace, 1, 7);
    expectToken(eof, 1, 9);

    scanVerifyVisualize();
  }

  @Test
  public void bigNumber() {
    initScannerCode(" {2147483648} ");

    expectToken(lbrace, 1, 2);
    expectInvalidToken(number, 1, 3);
    expectError(1, 3, BIG_NUM, "2147483648");
    expectToken(rbrace, 1, 13);
    expectToken(eof, 1, 15);

    scanVerifyVisualize();
  }

  @Test
  public void negativeBigNumber() {
    initScannerCode(" {-2147483648} ");

    expectToken(lbrace, 1, 2);
    expectToken(minus, 1, 3);
    expectInvalidToken(number, 1, 4);
    expectError(1, 4, BIG_NUM, "2147483648");
    expectToken(rbrace, 1, 14);
    expectToken(eof, 1, 16);

    scanVerifyVisualize();
  }

  @Test
  public void reallyBigNumber() {
    initScannerCode(" {1234567890123456789012345678901234567890} ");

    expectToken(lbrace, 1, 2);
    expectInvalidToken(number, 1, 3);
    expectError(1, 3, BIG_NUM, "1234567890123456789012345678901234567890");
    expectToken(rbrace, 1, 43);
    expectToken(eof, 1, 45);

    scanVerifyVisualize();
  }

  @Test
  public void numberIdent() {
    initScannerCode(" {123abc123 123break} ");

    expectToken(lbrace, 1, 2);
    expectToken(number, 1, 3, 123);
    expectToken(ident, 1, 6, "abc123");
    expectToken(number, 1, 13, 123);
    expectToken(break_, 1, 16);
    expectToken(rbrace, 1, 21);
    expectToken(eof, 1, 23);

    scanVerifyVisualize();
  }

  @Test
  public void numbersSeparated() {
    initScannerCode("123.456,789");

    expectToken(number, 1, 1, 123);
    expectToken(period, 1, 4);
    expectToken(number, 1, 5, 456);
    expectToken(comma, 1, 8);
    expectToken(number, 1, 9, 789);
    expectToken(eof, 1, 12);

    scanVerifyVisualize();
  }

  @Test
  public void identsSeparated() {
    initScannerCode("abc.def,ghi\njkl");

    expectToken(ident, 1, 1, "abc");
    expectToken(period, 1, 4);
    expectToken(ident, 1, 5, "def");
    expectToken(comma, 1, 8);
    expectToken(ident, 1, 9, "ghi");
    expectToken(ident, 2, 1, "jkl");
    expectToken(eof, 2, 4);

    scanVerifyVisualize();
  }

  @Test
  public void newlineBetweenIdentifiersAndTokens() {
    initScannerCode("anIdentifier" + LF + "class" + LF + "anotherIdentifier");

    expectToken(ident, 1, 1, "anIdentifier");
    expectToken(class_, 2, 1);
    expectToken(ident, 3, 1, "anotherIdentifier");

    scanVerifyVisualize();
  }

  @Test
  public void newlineAndSpacesBetweenIdentifiersAndTokens() {
    initScannerCode(" anIdentifier" + LF + "  class" + LF + "   anotherIdentifier");

    expectToken(ident, 1, 2, "anIdentifier");
    expectToken(class_, 2, 3);
    expectToken(ident, 3, 4, "anotherIdentifier");

    scanVerifyVisualize();
  }

  @Test
  public void charConst() {
    initScannerCode(" {' ' 'A' 'z' '0' '!' '\"' '" + invalidChar + "' '\0'} ");

    expectToken(lbrace, 1, 2);
    expectToken(charConst, 1, 3, ' ');
    expectToken(charConst, 1, 7, 'A');
    expectToken(charConst, 1, 11, 'z');
    expectToken(charConst, 1, 15, '0');
    expectToken(charConst, 1, 19, '!');
    expectToken(charConst, 1, 23, '"');
    expectToken(charConst, 1, 27, invalidChar);
    expectToken(charConst, 1, 31, '\0');
    expectToken(rbrace, 1, 34);
    expectToken(eof, 1, 36);

    scanVerifyVisualize();
  }

  @Test
  public void singleCharConst() {
    initScannerCode("'x'");

    expectToken(charConst, 1, 1, 'x');
    expectToken(eof, 1, 4);

    scanVerifyVisualize();
  }

  @Test
  public void escapeCharConst() {
    initScannerCode(" {'\\n' '\\r' '\\\\' '\\''} ");

    expectToken(lbrace, 1, 2);
    expectToken(charConst, 1, 3, '\n');
    expectToken(charConst, 1, 8, '\r');
    expectToken(charConst, 1, 13, '\\');
    expectToken(charConst, 1, 18, '\'');
    expectToken(rbrace, 1, 22);
    expectToken(eof, 1, 24);

    scanVerifyVisualize();
  }

  @Test
  public void singleEscapeCharConst() {
    initScannerCode("'\\n'");

    expectToken(charConst, 1, 1, '\n');
    expectToken(eof, 1, 5);

    scanVerifyVisualize();
  }

  @Test
  public void emptyCharConst() {
    initScannerCode(" {''} ");

    expectToken(lbrace, 1, 2);
    expectToken(charConst, 1, 3, '\0');
    expectError(1, 3, EMPTY_CHARCONST);
    expectToken(rbrace, 1, 5);
    expectToken(eof, 1, 7);

    scanVerifyVisualize();
  }

  @Test
  public void unclosedCharConst() {
    initScannerCode(" {'a} ");

    expectToken(lbrace, 1, 2);
    expectToken(charConst, 1, 3, '\0');
    expectError(1, 3, MISSING_QUOTE);
    expectToken(rbrace, 1, 5);
    expectToken(eof, 1, 7);

    scanVerifyVisualize();
  }

  @Test
  public void emptyAndUnclosedCharConst() {
    initScannerCode(" ''' ");

    expectToken(charConst, 1, 2, '\0');
    expectError(1, 2, EMPTY_CHARCONST);
    expectToken(charConst, 1, 4, '\0');
    expectError(1, 4, MISSING_QUOTE);
    expectToken(eof, 1, 6);

    scanVerifyVisualize();
  }

  @Test
  public void unclosedEscapeCharConst() {
    initScannerCode(" {'\\r} ");

    expectToken(lbrace, 1, 2);
    expectToken(charConst, 1, 3, '\0');
    expectError(1, 3, MISSING_QUOTE);
    expectToken(rbrace, 1, 6);
    expectToken(eof, 1, 8);

    scanVerifyVisualize();
  }

  @Test
  public void unclosedBackslashCharConst() {
    initScannerCode(" {'\\'} ");

    expectToken(lbrace, 1, 2);
    expectToken(charConst, 1, 3, '\0');
    expectError(1, 3, MISSING_QUOTE);
    expectToken(rbrace, 1, 6);
    expectToken(eof, 1, 8);

    scanVerifyVisualize();
  }

  @Test
  public void invalidEscapeCharConst() {
    initScannerCode(" {'\\a'} ");

    expectToken(lbrace, 1, 2);
    expectToken(charConst, 1, 3, '\0');
    expectError(1, 3, UNDEFINED_ESCAPE, 'a');
    expectToken(rbrace, 1, 7);
    expectToken(eof, 1, 9);

    scanVerifyVisualize();
  }

  @Test
  public void invalidEscapeCharMissingQuote() {
    initScannerCode(" '\\a ");

    expectToken(charConst, 1, 2, '\0');
    expectError(1, 2, UNDEFINED_ESCAPE, 'a');
    expectError(1, 2, MISSING_QUOTE);
    expectToken(eof, 1, 6);

    scanVerifyVisualize();
  }

  @Test
  public void fileEndCharConst() {
    initScannerCode(" {'");

    expectToken(lbrace, 1, 2);
    expectToken(charConst, 1, 3, '\0');
    expectError(1, 3, EOF_IN_CHAR);
    expectToken(eof, 1, 4);

    scanVerifyVisualize();
  }

  @Test
  public void lineEndCharConst() {
    initScannerCode(" {'" + LF + "'a'} ");

    expectToken(lbrace, 1, 2);
    expectToken(charConst, 1, 3, '\0');
    expectError(1, 3, ILLEGAL_LINE_END);
    expectToken(charConst, 2, 1, 'a');
    expectToken(rbrace, 2, 4);
    expectToken(eof, 2, 6);

    scanVerifyVisualize();
  }

  @Test
  public void lineEndWithCRCharConst() {
    initScannerCode(" {'" + CR + LF + "'a'} ");

    expectToken(lbrace, 1, 2);
    expectToken(charConst, 1, 3, '\0');
    expectError(1, 3, ILLEGAL_LINE_END);
    expectToken(charConst, 2, 1, 'a');
    expectToken(rbrace, 2, 4);
    expectToken(eof, 2, 6);

    scanVerifyVisualize();
  }

  @Test
  public void keyword1() {
    initScannerCode(" { if } ");

    expectToken(lbrace, 1, 2);
    expectToken(if_, 1, 4);
    expectToken(rbrace, 1, 7);
    expectToken(eof, 1, 9);

    scanVerifyVisualize();
  }

  @Test
  public void keyword2() {
    initScannerCode(" {if} ");

    expectToken(lbrace, 1, 2);
    expectToken(if_, 1, 3);
    expectToken(rbrace, 1, 5);
    expectToken(eof, 1, 7);

    scanVerifyVisualize();
  }

  @Test
  public void singleKeyword() {
    initScannerCode("if");

    expectToken(if_, 1, 1);
    expectToken(eof, 1, 3);

    scanVerifyVisualize();
  }

  @Test
  public void keyword3() {
    initScannerCode(" {for_} ");

    expectToken(lbrace, 1, 2);
    expectToken(ident, 1, 3, "for_");
    expectToken(rbrace, 1, 7);
    expectToken(eof, 1, 9);

    scanVerifyVisualize();
  }

  @Test
  public void keyword4() {
    initScannerCode(" {&if} ");

    expectToken(lbrace, 1, 2);
    expectToken(none, 1, 3);
    expectError(1, 3, INVALID_CHAR, '&');
    expectToken(if_, 1, 4);
    expectToken(rbrace, 1, 6);
    expectToken(eof, 1, 8);

    scanVerifyVisualize();
  }

  @Test
  public void caseSensitive1() {
    initScannerCode(" {For} ");

    expectToken(lbrace, 1, 2);
    expectToken(ident, 1, 3, "For");
    expectToken(rbrace, 1, 6);
    expectToken(eof, 1, 8);

    scanVerifyVisualize();
  }

  @Test
  public void caseSensitive2() {
    initScannerCode(" {FOR} ");

    expectToken(lbrace, 1, 2);
    expectToken(ident, 1, 3, "FOR");
    expectToken(rbrace, 1, 6);
    expectToken(eof, 1, 8);

    scanVerifyVisualize();
  }

  @Test
  public void simpleSingleLineComment() {
    initScannerCode(" {/* Simple / single * line comment. */} ");

    expectToken(lbrace, 1, 2);
    expectToken(rbrace, 1, 40);
    expectToken(eof, 1, 42);

    scanVerifyVisualize();
  }

  @Test
  public void simpleMultiLineComment() {
    initScannerCode(" {" + LF + "  /* Simple " + LF + "     / multi * line " + LF //
            + "     comment. */ " + LF + " } ");

    expectToken(lbrace, 1, 2);
    expectToken(rbrace, 5, 2);
    expectToken(eof, 5, 4);

    scanVerifyVisualize();
  }

  @Test
  public void nestedSingleLineComment2() {
    initScannerCode(" {/*//*///****/**/*/} ");

    expectToken(lbrace, 1, 2);
    expectToken(rbrace, 1, 21);
    expectToken(eof, 1, 23);

    scanVerifyVisualize();
  }

  @Test
  public void nestedSingleLineComment() {
    initScannerCode(" {/* This / is * a /* nested  /* single line */ comment. */*/} ");

    expectToken(lbrace, 1, 2);
    expectToken(rbrace, 1, 62);
    expectToken(eof, 1, 64);

    scanVerifyVisualize();
  }

  @Test
  public void nestedMultiLineComment() {
    initScannerCode(" {" + LF + "  /* This / is * a " + LF + "   /* nested  " + LF //
            + "    /* multi line */" + LF + "    comment. " + LF + "   */" + LF //
            + "  */ " + LF + " } ");

    expectToken(lbrace, 1, 2);
    expectToken(rbrace, 8, 2);
    expectToken(eof, 8, 4);

    scanVerifyVisualize();
  }

  @Test
  public void nestedMultiLineComment2() {
    initScannerCode(" {" + LF + "  /* This / is * a " + LF + "   /* nested  " + LF //
            + "    /* multi /*/* double nestet */*/ line */" + LF + "    comment. " + LF + "   */" + LF //
            + "  */ " + LF + " } ");

    expectToken(lbrace, 1, 2);
    expectToken(rbrace, 8, 2);
    expectToken(eof, 8, 4);

    scanVerifyVisualize();
  }

  @Test
  public void commentAtEnd1() {
    initScannerCode(" {/* This / is * a /* nested  /* single line */ comment. */*/ ");

    expectToken(lbrace, 1, 2);
    expectToken(eof, 1, 63);

    scanVerifyVisualize();
  }

  @Test
  public void commentAtEnd2() {
    initScannerCode(" {/* This / is * a /* nested  /* single line */ comment. */*/");

    expectToken(lbrace, 1, 2);
    expectToken(eof, 1, 62);

    scanVerifyVisualize();
  }

  @Test
  public void unclosedComment() {
    initScannerCode(" {/* This / is * a nested unclosed comment. } ");

    expectToken(lbrace, 1, 2);
    expectError(1, 3, EOF_IN_COMMENT);
    expectToken(eof, 1, 47);

    scanVerifyVisualize();
  }

  @Test
  public void unclosedComment2() {
    initScannerCode(" {/*/");

    expectToken(lbrace, 1, 2);
    expectError(1, 3, EOF_IN_COMMENT);
    expectToken(eof, 1, 6);

    scanVerifyVisualize();
  }

  @Test
  public void nestedUnclosedComment() {
    initScannerCode(" {/* This / is * a /* nested /* unclosed comment. */} ");

    expectToken(lbrace, 1, 2);
    expectError(1, 3, EOF_IN_COMMENT);
    expectToken(eof, 1, 55);

    scanVerifyVisualize();
  }

  @Test
  public void nestedUnclosedComment2() {
    initScannerCode(" {/* This / is * a nested unclosed /* comment. } */");

    expectToken(lbrace, 1, 2);
    expectError(1, 3, EOF_IN_COMMENT);
    expectToken(eof, 1, 52);

    scanVerifyVisualize();
  }

  @Test
  public void noLineComment() {
    initScannerCode(" {This is // no comment} ");

    expectToken(lbrace, 1, 2);
    expectToken(ident, 1, 3, "This");
    expectToken(ident, 1, 8, "is");
    expectToken(slash, 1, 11);
    expectToken(slash, 1, 12);
    expectToken(ident, 1, 14, "no");
    expectToken(ident, 1, 17, "comment");
    expectToken(rbrace, 1, 24);
    expectToken(eof, 1, 26);

    scanVerifyVisualize();
  }

  @Test
  public void multipleComments() {
    initScannerCode(" {/*a*/  /*b*/  /*c*/ } ");

    expectToken(lbrace, 1, 2);
    expectToken(rbrace, 1, 23);
    expectToken(eof, 1, 25);

    scanVerifyVisualize();
  }

  // index from end tests

  @Test
  public void negativeIndexFromEnd() {
    initScannerCode("~-7");

    expectToken(tilde, 1, 1);
    expectToken(minus, 1, 2);
    expectToken(number, 1, 3, 7);
    expectToken(eof, 1, 4);

    scanVerifyVisualize();
  }

  @Test
  public void singleTildeChar() {
    initScannerCode("'~'");

    expectToken(charConst, 1, 1, '~');
    expectToken(eof, 1, 4);

    scanVerifyVisualize();
  }

  @Test
  public void commentInIndexFromEnd() {
    initScannerCode("~/*comment*/7");

    expectToken(tilde, 1, 1);
    expectToken(number, 1, 13, 7);
    expectToken(eof, 1,14);

    scanVerifyVisualize();
  }

  @Test
  public void doubleTilde() {
    initScannerCode("~~");

    expectToken(tilde, 1, 1);
    expectToken(tilde, 1, 2);
    expectToken(eof, 1,3);

    scanVerifyVisualize();
  }

  @Test
  public void allTokens() {
    initScannerCode("anIdentifier 123 'c'" + LF //
            + "+ - * / % == != < <= > >= && || = += -= *= /= %= ++ -- ; , . ( ) [ ] { }" + LF //
            + "break class else final if new print program read return void while ~" + LF);

    expectToken(ident, 1, 1, "anIdentifier");
    expectToken(number, 1, 14, 123);
    expectToken(charConst, 1, 18, 'c');
    expectToken(plus, 2, 1);
    expectToken(minus, 2, 3);
    expectToken(times, 2, 5);
    expectToken(slash, 2, 7);
    expectToken(rem, 2, 9);
    expectToken(eql, 2, 11);
    expectToken(neq, 2, 14);
    expectToken(lss, 2, 17);
    expectToken(leq, 2, 19);
    expectToken(gtr, 2, 22);
    expectToken(geq, 2, 24);
    expectToken(and, 2, 27);
    expectToken(or, 2, 30);
    expectToken(assign, 2, 33);
    expectToken(plusas, 2, 35);
    expectToken(minusas, 2, 38);
    expectToken(timesas, 2, 41);
    expectToken(slashas, 2, 44);
    expectToken(remas, 2, 47);
    expectToken(pplus, 2, 50);
    expectToken(mminus, 2, 53);
    expectToken(semicolon, 2, 56);
    expectToken(comma, 2, 58);
    expectToken(period, 2, 60);
    expectToken(lpar, 2, 62);
    expectToken(rpar, 2, 64);
    expectToken(lbrack, 2, 66);
    expectToken(rbrack, 2, 68);
    expectToken(lbrace, 2, 70);
    expectToken(rbrace, 2, 72);
    expectToken(break_, 3, 1);
    expectToken(class_, 3, 7);
    expectToken(else_, 3, 13);
    expectToken(final_, 3, 18);
    expectToken(if_, 3, 24);
    expectToken(new_, 3, 27);
    expectToken(print, 3, 31);
    expectToken(program, 3, 37);
    expectToken(read, 3, 45);
    expectToken(return_, 3, 50);
    expectToken(void_, 3, 57);
    expectToken(while_, 3, 62);
    expectToken(tilde, 3, 68);
    expectToken(eof, 4, 1);

    scanVerifyVisualize();
  }
}
