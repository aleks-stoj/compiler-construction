package ssw.mj.impl;

import javassist.expr.Expr;
import javassist.expr.FieldAccess;
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
  private final EnumSet<Token.Kind> firstProgram = EnumSet.of(final_, ident, class_);
  private final EnumSet<Token.Kind> firstMethodDecl = EnumSet.of(ident, void_);
  private final EnumSet<Token.Kind> firstType = EnumSet.of(ident, lbrack, rbrack);


  static {
    // Initialize first and follow sets.
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
    scan();
    check(ident);
    scan();

    while(firstProgram.contains(sym)) {
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
    scan();

    while(firstMethodDecl.contains(sym)) {
      MethodDecl();
    }

    check(rbrace);
    scan();
  }

  /**
   * <code>ConstDecl = "final" Type ident "=" ( number | charConst ) ";".</code>
   */
  private void ConstDecl() {
    check(final_);
    scan();
    Type();
    check(ident);
    scan();
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
    scan();
  }

  /**
   * <code>VarDecl = Type ident { "," ident } ";".</code>
   */
  private void VarDecl() {
    Type();
    check(ident);
    scan();

    while(sym == comma) {
      scan();
      check(ident);
      scan();
    }

    check(semicolon);
    scan();
  }

  /**
   * <code>ClassDecl = "class" ident "{" { VarDecl } "}".</code>
   */
  private void ClassDecl() {
    check(class_);
    scan();
    check(ident);
    scan();
    check(lbrace);

    while(firstType.contains(sym)) {
      VarDecl();
    }

    check(rbrace);
    scan();
  }

  private void MethodDecl() {
    if(firstType.contains(sym)) {
      Type();
    }
    else if(sym == void_) {
      scan();
    }
    else {
      error(INVALID_METHOD_DECL);
    }

    check(ident);
    scan();

    check(lpar);

    if(firstType.contains(sym)) {
      FormPars();
    }

    check(rpar);

    while(firstType.contains(sym)) {
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
    scan();
    while(sym == comma) {
      scan();
      Type();
      check(ident);
      scan();
    }
  }

  /**
   * <code>ident [ "[" "]" ].</code>
   */
  private void Type() {
    check(ident);
    scan(); // checks at the very least that sym is ident
    if(sym == lbrack) { // next token is lbrack
      scan();
      if (sym == rbrack) { // closing rbrack
        scan(); // successful
      } else { // no closing rbrack --> error
        error(TOKEN_EXPECTED, rbrack);
      }
    }
  }

  /**
   * <code>Block = "{" { Statement } "}".</code>
   */
  private void Block() {
    check(lbrace);
    scan();

    while(firstStatement.contains(sym)) {
      Statement();
    }

    check(rbrace);
    scan();
  }

  private void Statement() {
    if(firstDesignator.contains(sym)) {
      Designator();
      if(firstAssignOp.contains(sym)) {
        Expr();
      }
      else if(firstActPars.contains(sym)) {
        ActPars();
      }
      else if(sym == pplus) {
        scan();
      }
      else if(sym == mminus) {
        scan();
      }
      check(semicolon);
      scan();
    }
    else if(sym == if_) {
      scan();
      check(lpar);
      scan();
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
      scan();
      Condition();
      check(rpar);
      scan();
      Statement();
    }
    else if(sym == break_) {
      scan();
      check(semicolon);
      scan();
    }
    else if(sym == return_) {
      scan();
      if(firstExpr.contains(sym)) {
        Expr();
      }
      check(semicolon);
      scan();
    }
    else if(sym == read) {
      scan();
      check(lpar);
      scan();
      Designator();
      check(rpar);
      scan();
      check(semicolon);
      scan();
    }
    else if(sym == print) {
      scan();
      check(lpar);
      scan();
      Expr();
      if(sym == comma) {
        scan();
        check(number);
        scan();
      }
      check(rpar);
      scan();
      check(semicolon);
      scan();
    }
    else if(firstBlock.contains(sym)) {
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
    scan();
  }

  private void ActPars() {
    check(lpar);
    scan();

    if(firstExpr.contains(sym)) {
      Expr();
      while (sym == comma) {
        scan();
        Expr();
      }
    }

    check(rpar);
    scan();
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
    scan();
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
    if(firstDesignator.contains(sym)) {
      Designator();
      if(firstActPars.contains(sym)) {
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
      scan();
      if(sym == lbrack) {
        scan();
        Expr();
        check(rbrack);
        scan();
      }
    }
    else if(sym == lpar) {
      scan();
      Expr();
      check(rpar);
      scan();
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
    scan();
    while(sym == period || sym == lbrack) {
      if(sym == lbrack) {
        scan();
        if(sym == tilde) {
          scan();
        }
        Expr();
        scan();
        // check for "]"
      }
      else { // "." is read
        scan(); // read next token
        check(ident); // check next token is identifier
      }
    }
    scan(); // read next token
  }

  private void AddOp() {
    switch (sym) {
      case plus: scan(); break;
      case minus: scan(); break;
      default: error(INVALID_ADD_OP);
    }
    scan();
  }

  private void MulOp() {
    switch (sym) {
      case times: scan(); break;
      case slash: scan(); break;
      case rem: scan(); break;
      default: error(INVALID_MUL_OP);
    }
    scan();
  }

  // ...

  // ------------------------------------

  // TODO Exercise UE-P-3: Error recovery methods: recoverDecl, recoverMethodDecl and recoverStat (+ TODO Exercise UE-P-5: Check idents for Type kind)

  // ====================================
  // ====================================
}
