package ssw.mj.impl;

import ssw.mj.Errors;
import ssw.mj.Errors.Message;
import ssw.mj.scanner.Token;

import java.util.EnumSet;

import static ssw.mj.Errors.Message.*;
import static ssw.mj.scanner.Token.Kind.*;

public final class Parser {

  /**
   * Maximum number of global variables per program
   */
  private static final int MAX_GLOBALS = 32767;

  /**
   * Maximum number of fields per class
   */
  private static final int MAX_FIELDS = 32767;

  /**
   * Maximum number of local variables per method
   */
  private static final int MAX_LOCALS = 127;

  /**
   * Last recognized token;
   */
  private Token t;

  /**
   * Lookahead token (not recognized).)
   */
  private Token la;

  /**
   * Shortcut to kind attribute of lookahead token (la).
   */
  private Token.Kind sym;

  /**
   * According scanner
   */
  public final Scanner scanner;

  /**
   * According code buffer
   */
  public final Code code;

  /**
   * According symbol table
   */
  public final Tab tab;

  public Parser(Scanner scanner) {
    this.scanner = scanner;
    tab = new Tab(this);
    code = new Code(this);
    // Pseudo token to avoid crash when 1st symbol has scanner error.
    la = new Token(none, 1, 1);
  }


  /**
   * Reads ahead one symbol.
   */
  private void scan() {
    t = la;
    la = scanner.next();
    sym = la.kind;
  }

  /**
   * Verifies symbol and reads ahead.
   */
  private void check(Token.Kind expected) {
    if (sym == expected) {
      scan();
    } else {
      error(TOKEN_EXPECTED, expected);
    }
  }

  /**
   * Adds error message to the list of errors.
   */
  public void error(Message msg, Object... msgParams) {
    // TODO Exercise UE-P-3: Replace panic mode with error recovery (i.e., keep track of error distance)
    // TODO Exercise UE-P-3: Hint: Replacing panic mode also affects scan() method
    scanner.errors.error(la.line, la.col, msg, msgParams);
    throw new Errors.PanicMode();
  }

  /**
   * Starts the analysis.
   */
  public void parse() {
    scan(); // scan first symbol, initializes look-ahead
    Program(); // start analysis
    check(eof);
  }


  // ===============================================
  // TODO Exercise UE-P-2: Implementation of parser
  // TODO Exercise UE-P-3: Error recovery methods
  // TODO Exercise UE-P-4: Symbol table handling
  // TODO Exercise UE-P-5-6: Code generation
  // ===============================================

  // TODO Exercise UE-P-3: Error distance

  // TODO Exercise UE-P-2 + Exercise 3: Sets to handle certain first, follow, and recover sets
  private static final EnumSet<Token.Kind> firstDecl; // combination of First(ConstDecl), First(VarDecl) and First(ClassDecl). Decided to combine them into one, as it isn't worth it to write them in separate sets.
  private static final EnumSet<Token.Kind> firstMethodDecl;
  private static final EnumSet<Token.Kind> firstAssignOp;
  private static final EnumSet<Token.Kind> firstAddOp;
  private static final EnumSet<Token.Kind> firstMulOp;
  private static final EnumSet<Token.Kind> firstStatement;
  private static final EnumSet<Token.Kind> firstFactor;
  private static final EnumSet<Token.Kind> firstExpr;


  static {
    // Initialize first and follow sets.
    firstDecl = EnumSet.of(final_, ident, class_);
    firstMethodDecl = EnumSet.of(ident, void_);
    firstAssignOp = EnumSet.of(assign, plusas, minusas, timesas, slashas, remas);
    firstAddOp = EnumSet.of(plus, minus);
    firstMulOp = EnumSet.of(times, slash, rem);
    firstStatement = EnumSet.of(ident, if_, while_, break_, return_, read, print, lbrace, semicolon);
    firstFactor = EnumSet.of(ident, number, charConst, new_, lpar);
    firstExpr = EnumSet.of(ident, number, charConst, new_, lpar, minus);
  }

  // ---------------------------------

  // TODO Exercise UE-P-2: One top-down parsing method per production

  /**
   * Program = <br>
   * "program" ident <br>
   * { ConstDecl | VarDecl | ClassDecl } <br>
   * "{" { MethodDecl } "}" .
   */
  private void Program() {
    // TODO Exercise UE-P-2
    check(program);
    check(ident);

    while(firstDecl.contains(sym)) {
      if(sym == final_) {
        ConstDecl();
      }
      else if(sym == ident) {
        VarDecl();
      }
      else {
        ClassDecl();
      }
    }

    check(lbrace);

    while(firstMethodDecl.contains(sym)) {
      MethodDecl();
    }

    check(rbrace);
  }

  /**
   * <code>ConstDecl = "final" Type ident "=" ( number | charConst ) ";".</code>
   */
  private void ConstDecl() {
    check(final_);
    Type();
    check(ident);
    check(assign);

    if(sym == number) {
      scan();
    }
    else if(sym == charConst) {
      scan();
    }
    else {
      error(INVALID_CONST_TYPE);
    }

    check(semicolon);
  }

  /**
   * <code>VarDecl = Type ident { "," ident } ";".</code>
   */
  private void VarDecl() {
    Type();
    check(ident);

    while(sym == comma) {
      scan();
      check(ident);
    }

    check(semicolon);
  }

  /**
   * <code>ClassDecl = "class" ident "{" { VarDecl } "}".</code>
   */
  private void ClassDecl() {
    check(class_);
    check(ident);
    check(lbrace);

    while(sym == ident) {
      VarDecl();
    }

    check(rbrace);
  }

  private void MethodDecl() {
    if(sym == ident) {
      Type();
    }
    else if(sym == void_) {
      scan();
    }
    else {
      error(INVALID_METHOD_DECL);
    }

    check(ident);
    check(lpar);

    if(sym == ident) {
      FormPars();
    }

    check(rpar);

    while(sym == ident) {
      VarDecl();
    }

    Block();
  }

  /**
   * <code>FormPars = Type ident { "," Type ident }.</code>
   */
  private void FormPars() {
    Type();
    check(ident);

    while(sym == comma) {
      scan();
      Type();
      check(ident);
    }
  }

  /**
   * <code>ident [ "[" "]" ].</code>
   */
  private void Type() {
    check(ident);
    if(sym == lbrack) {
      scan();
      check(rbrack);
    }
  }

  /**
   * <code>Block = "{" { Statement } "}".</code>
   */
  private void Block() {
    check(lbrace);

    while(firstStatement.contains(sym)) {
      Statement();
    }

    check(rbrace);
  }

  private void Statement() {
    if(sym == ident) {
      Designator();
      if(firstAssignOp.contains(sym)) {
        AssignOp();
        Expr();
      }
      else if(sym == lpar) {
        ActPars();
      }
      else if(sym == pplus) {
        scan();
      }
      else if(sym == mminus) {
        scan();
      }
      else {
        error(INVALID_DESIGNATOR_STATEMENT);
      }
      check(semicolon);
    }
    else if(sym == if_) {
      scan();
      check(lpar);
      Condition();
      check(rpar);
      Statement();
      if(sym == else_) {
        scan();
        Statement();
      }
    }
    else if(sym == while_) {
      scan();
      check(lpar);
      Condition();
      check(rpar);
      Statement();
    }
    else if(sym == break_) {
      scan();
      check(semicolon);
    }
    else if(sym == return_) {
      scan();
      if(firstExpr.contains(sym)) {
        Expr();
      }
      check(semicolon);
    }
    else if(sym == read) {
      scan();
      check(lpar);
      Designator();
      check(rpar);
      check(semicolon);
    }
    else if(sym == print) {
      scan();
      check(lpar);
      Expr();
      if(sym == comma) {
        scan();
        check(number);
      }
      check(rpar);
      check(semicolon);
    }
    else if(sym == lbrace) {
      Block();
    }
    else if(sym == semicolon) {
      scan();
    }
    else {
      error(INVALID_STATEMENT);
    }
  }

  private void AssignOp() {
    switch (sym) {
      case assign: scan(); break;
      case plusas: scan(); break;
      case minusas: scan(); break;
      case timesas: scan(); break;
      case slashas: scan(); break;
      case remas: scan(); break;
      default: error(INVALID_ASSIGN_OP);
    }
  }

  private void ActPars() {
    check(lpar);

    if(firstExpr.contains(sym)) {
      Expr();
      while (sym == comma) {
        scan();
        Expr();
      }
    }

    check(rpar);
  }

  private void Condition() {
    CondTerm();

    while(sym == or) {
      scan();
      CondTerm();
    }
  }

  private void CondTerm() {
    CondFact();

    while(sym == and) {
      scan();
      CondFact();
    }
  }

  private void CondFact() {
    Expr();
    Relop();
    Expr();
  }

  private void Relop() {
    switch (sym) {
      case eql: scan(); break;
      case neq: scan(); break;
      case gtr: scan(); break;
      case geq: scan(); break;
      case lss: scan(); break;
      case leq: scan(); break;
      default: error(INVALID_REL_OP);
    }
  }

  private void Expr() {
    if(sym == minus) {
      scan();
    }
    Term();

    while(firstAddOp.contains(sym)) {
      AddOp();
      Term();
    }
  }

  private void Term() {
    Factor();

    while(firstMulOp.contains(sym)) {
      MulOp();
      Factor();
    }
  }

  private void Factor() {
    if(sym == ident) {
      Designator();
      if(sym == lpar) {
        ActPars();
      }
    }
    else if(sym == number) {
      scan();
    }
    else if(sym == charConst) {
      scan();
    }
    else if(sym == new_) {
      scan();
      check(ident);
      if(sym == lbrack) {
        scan();
        Expr();
        check(rbrack);
      }
    }
    else if(sym == lpar) {
      scan();
      Expr();
      check(rpar);
    }
    else {
      error(INVALID_FACTOR);
    }
  }

  /**
   * <code>Designator = ident { "." ident | "[" [ "~" ] Expr "]" }.</code>
   */
  private void Designator() {
    check(ident);
    while(sym == period || sym == lbrack) {
      if(sym == lbrack) {
        scan();
        if(sym == tilde) {
          scan();
        }
        Expr();
        check(rbrack);
      }
      else { // "." is read
        scan();
        check(ident);
      }
    }
  }

  private void AddOp() {
    switch (sym) {
      case plus: scan(); break;
      case minus: scan(); break;
      default: error(INVALID_ADD_OP);
    }
  }

  private void MulOp() {
    switch (sym) {
      case times: scan(); break;
      case slash: scan(); break;
      case rem: scan(); break;
      default: error(INVALID_MUL_OP);
    }
  }

  // ...

  // ------------------------------------

  // TODO Exercise UE-P-3: Error recovery methods: recoverDecl, recoverMethodDecl and recoverStat (+ TODO Exercise UE-P-5: Check idents for Type kind)

  // ====================================
  // ====================================
}
