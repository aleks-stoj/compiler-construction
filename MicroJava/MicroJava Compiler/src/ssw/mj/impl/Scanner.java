package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.scanner.Token;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import static ssw.mj.scanner.Token.Kind.*;

public class Scanner {

  // Scanner Skeleton - do not rename fields / methods !
  private static final char EOF = (char) -1;
  private static final char LF = '\n';

  /**
   * Input data to read from.
   */
  private final Reader in;

  /**
   * Lookahead character. (= next (unhandled) character in the input stream)
   */
  private char ch;

  /**
   * Current line in input stream.
   */
  private int line;

  /**
   * Current column in input stream.
   */
  private int col;

  /**
   * According errors object.
   */
  public final Errors errors;

  public Scanner(Reader r) {
    // store reader
    in = r;

    // initialize error handling support
    errors = new Errors();

    line = 1;
    col = 0;
    nextCh(); // read 1st char into ch, incr col to 1
  }

  /**
   * Adds error message to the list of errors.
   */
  public final void error(Token t, Errors.Message msg, Object... msgParams) {
    errors.error(t.line, t.col, msg, msgParams);

    // reset token content (consistent JUnit tests)
    t.numVal = 0;
    t.val = null;
  }


  // ================================================
  // TODO Exercise UE-P-1: Implement Scanner (next() + private helper methods)
  // ================================================

  // TODO Exercise UE-P-1: Keywords
  /**
   * Mapping from keyword names to appropriate token codes.
   */
  private static final Map<String, Token.Kind> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("", none);
    keywords.put("break", break_);
    keywords.put("class", class_);
    keywords.put("if", if_);
    keywords.put("else", else_);
    keywords.put("new", new_);
    keywords.put("final", final_);
    keywords.put("program", program);
    keywords.put("print", print);
    keywords.put("read", read);
    keywords.put("return", return_);
    keywords.put("void", void_);
    keywords.put("while", while_);
    keywords.put("eof", eof);
  }

  /**
   * Returns next token. To be used by parser.
   */
  public Token next() {
    // TODO Exercise UE-P-1: implementation of next method
    while(Character.isWhitespace(ch)) {
      nextCh(); // read next character if current one is space
    }

    Token t = new Token(none, line, col);
    // TODO: add behaviour for simple character '' and its error handling
    switch(ch) {
      // identifier or keyword
      case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j': case 'k': case 'l': case 'm': case 'n': case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u': case 'v': case 'w': case 'x': case 'y': case 'z':
      case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I': case 'J': case 'K': case 'L': case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R': case 'S': case 'T': case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
      case '_':
        readName(t);
        break;

      // number
      case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
        readNumber(t);
        break;

      // character
      case '\'':
        readCharConst(t);
        break;

      // simple tokens
      case ';':
        t.kind = semicolon;
        nextCh();
        break;
      case ',':
        t.kind = comma;
        nextCh();
        break;
      case '.':
        t.kind = period;
        nextCh();
        break;
      case '(':
        t.kind = lpar;
        nextCh();
        break;
      case ')':
        t.kind = rpar;
        nextCh();
        break;
      case '[':
        t.kind = lbrack;
        nextCh();
        break;
      case ']':
        t.kind = rbrack;
        nextCh();
        break;
      case '{':
        t.kind = lbrace;
        nextCh();
        break;
      case '}':
        t.kind = rbrace;
        nextCh();
        break;
      case '~':
        t.kind = tilde;
        nextCh();
        break;
      case EOF: // end of file, read no new character
        t.kind = eof;
        break;

      // compound tokens
      // equals
      case '=':
        nextCh();
        if(ch == '=') {
          t.kind = eql;
        }
        else {
          t.kind = assign;
        }
        nextCh();
        break;
      // plus
      case '+':
        nextCh();
        if(ch == '+') {
          t.kind = pplus;
          nextCh();
        }
        else if(ch == '=') {
          t.kind = plusas;
          nextCh();
        }
        else {
          t.kind = plus;
        }
        break;
      // minus
      case '-':
        nextCh();
        if(ch == '-') {
          t.kind = mminus;
          nextCh();
        }
        else if(ch == '=') {
          t.kind = minusas;
          nextCh();
        }
        else {
          t.kind = minus;
        }
        break;
      // times
      case '*':
        nextCh();
        if(ch == '=') {
          t.kind = timesas;
          nextCh();
        }
        else {
          t.kind = times;
        }
        break;
      // divide/comment
      case '/':
        nextCh();
        if(ch == '=') {
          t.kind = slashas;
          nextCh();
        }
        else if(ch == '*') {
          skipComment(t);
          t = next();
        }
        else {
          t.kind = slash;
        }
        break;
      // remainder/modulo
      case '%':
        nextCh();
        if(ch == '=') {
          t.kind = remas;
          nextCh();
        }
        else {
          t.kind = rem;
        }
        break;
      // negation
      case '!':
        nextCh();
        if(ch == '=') {
          t.kind = neq;
          nextCh();
        }
        else {
          error(t, Errors.Message.INVALID_CHAR, '!'); // no negation symbol
        }
        break;
      // less (equals)
      case '<':
        nextCh();
        if(ch == '=') {
          t.kind = leq;
          nextCh();
        }
        else {
          t.kind = lss;
        }
        break;
      // greater (equals)
      case '>':
        nextCh();
        if(ch == '=') {
          t.kind = geq;
          nextCh();
        }
        else {
          t.kind = gtr;
        }
        break;
      // logical and
      case '&':
        nextCh();
        if(ch == '&') {
          t.kind = and;
          nextCh();
        }
        else {
          error(t, Errors.Message.INVALID_CHAR, '&'); // no second and
        }
        break;
      // logical or
      case '|':
        nextCh();
        if(ch == '|') {
          t.kind = or;
          nextCh();
        }
        else {
          error(t, Errors.Message.INVALID_CHAR, '|'); // no second or
        }
        break;
      default:
        error(t, Errors.Message.INVALID_CHAR, ch);
        nextCh();
        t.kind = none;
        break;
    }

    return t;
  }

  private void nextCh() {
    // TODO Exercise UE-P-1: implementation of nextCh method and other private helper methods
    try {
      ch = (char) in.read(); // read next character in stream
      col++; // advance one character
      if(ch == LF) { // character is line feed
        line++; // advance one line
        col = 0; // go to first character of new line
      }
    }
    catch (IOException e) {
      ch = EOF; // eof reached in character stream
    }

  }

  // TODO Exercise UE-P-1: private helper methods used by next(), as discussed in the exercise
  private void readName(Token t) {
    t.kind = ident; // declare as identifier
    StringBuilder nameBuilder = new StringBuilder();
    nameBuilder.append(ch); // add first character
    nextCh(); // move to next character

    while(isLetter(ch) || isDigit(ch) || ch == '_') { // go through the character stream while valid naming is valid (i.e. letter or digit or underscore)
      nameBuilder.append(ch);

      if(keywords.containsKey(nameBuilder.toString())) { // if string in sb matches a keyword, get its kind from the hashmap and quit loop
        t.kind = keywords.get(nameBuilder.toString());
        nextCh();
        return;
      }

      nextCh(); // read next character
    }
    t.val = nameBuilder.toString(); // save value
  }

  private void readNumber(Token t) {
    t.kind = number; // declare as number
    StringBuilder numberBuilder = new StringBuilder();
    numberBuilder.append(ch);
    nextCh();

    // Parse to Double in order to parse > MAX INT
    while(isDigit(ch)) {
      numberBuilder.append(ch);
      nextCh();
    }

    t.val = numberBuilder.toString();

    try { // catch numberformatexception and execute error(BIG_NUM) instead. This is inelegant, however I don't know how else to efficiently solve it within the loop without causing a NumberFormatException
      t.numVal = Integer.parseInt(numberBuilder.toString());
    }
    catch (NumberFormatException e) {
       error(t, Errors.Message.BIG_NUM, t.val);
    }
  }

  private void readCharConst(Token t) {
    t.kind = charConst;
    StringBuilder charBuilder = new StringBuilder();
    nextCh(); // char after opening '

    if(ch == '\'') {
      error(t, Errors.Message.EMPTY_CHARCONST);
      t.numVal = 0;
    }
    if(ch == LF || ch == '\r') {
      error(t, Errors.Message.ILLEGAL_LINE_END);
      t.numVal = 0;
      t.val = String.valueOf((char) t.numVal);
      return;
    }
    if(ch == EOF) {
      error(t, Errors.Message.EOF_IN_CHAR);
      t.numVal = 0;
      t.val = String.valueOf((char) t.numVal);
      return;
    }

    if(ch == '\\') {
      charBuilder.append(ch);
      nextCh();
      if(ch == 'n' || ch == 'r' || ch == '\'' || ch == '\\') {
        charBuilder.append(ch);
        switch (ch) {
          case 'n': t.numVal = '\n'; break;
          case 'r': t.numVal = '\r'; break;
          case '\\': t.numVal = '\\'; break;
          case '\'': t.numVal = '\''; break;
        }
        nextCh();
        if(ch != '\'') {
          error(t, Errors.Message.MISSING_QUOTE);
          t.val = String.valueOf((char) t.numVal);
          return;
        }
      }
      else {
        error(t, Errors.Message.UNDEFINED_ESCAPE, ch);
        nextCh();
        if(ch != '\'') {
          error(t, Errors.Message.MISSING_QUOTE);
        }
        t.numVal = 0;
      }
    }
    else if(ch != '\'') { // normal char read
      charBuilder.append(ch);
      nextCh();
      if(ch != '\'') {
        error(t, Errors.Message.MISSING_QUOTE);
        t.numVal = 0;
        t.val = String.valueOf((char) t.numVal);
        return;
      }
      else {
        t.numVal = charBuilder.toString().charAt(0);
      }
    }
    t.val = String.valueOf((char) t.numVal);
    nextCh();
  }

  private void skipComment(Token t) {
    int count = 1;
    nextCh(); // go to first char after opening comment (after *)
    char prev = ch;

    while(count != 0) {
      nextCh();
      if(ch == EOF) {
        error(t, Errors.Message.EOF_IN_COMMENT);
        return;
      }
      if(ch == '*' && prev == '/') { // increase count if "/*" found
        count++;
        ch = 0; // to prevent from */ being read, thus effectively nullifying count++;
      }
      else if(ch == '/' && prev == '*') { // decrease count if "*/" found
        count--; // to prevent from /* being read, thus effectively nullifying count++;
        ch = 0;
      }
      prev = ch;
    }
    nextCh();
  }

  // -----------------------------------------------

  private boolean isLetter(char c) {
    return 'a' <= c && c <= 'z' || 'A' <= c && c <= 'Z';
  }

  private boolean isDigit(char c) {
    return '0' <= c && c <= '9';
  }

  // ================================================
  // ================================================
}
