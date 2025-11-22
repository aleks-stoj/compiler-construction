package ssw.mj.impl;

import ssw.mj.Errors.Message;
import ssw.mj.scanner.Token;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Struct;

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
    errorDist++;
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
    if(errorDist >= MIN_ERROR_DIST) {
      scanner.errors.error(la.line, la.col, msg, msgParams);
    }
    errorDist = 0;
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
  private int errorDist = MIN_ERROR_DIST;
  private static final int MIN_ERROR_DIST = 3;

  // TODO Exercise UE-P-2 + Exercise 3: Sets to handle certain first, follow, and recover sets
  private static final EnumSet<Token.Kind> firstDecl; // combination of First(ConstDecl), First(VarDecl) and First(ClassDecl). Decided to combine them into one, as it isn't worth it to write them in separate sets.
  private static final EnumSet<Token.Kind> firstMethodDecl;
  private static final EnumSet<Token.Kind> firstAssignOp;
  private static final EnumSet<Token.Kind> firstAddOp;
  private static final EnumSet<Token.Kind> firstMulOp;
  private static final EnumSet<Token.Kind> firstStatement;
  private static final EnumSet<Token.Kind> firstExpr;
  private static final EnumSet<Token.Kind> followStatement;
  private static final EnumSet<Token.Kind> recoverDecl;
  private static final EnumSet<Token.Kind> recoverMethodDecl;
  private static final EnumSet<Token.Kind> recoverStatement;


  static {
    // Initialize first and follow sets.
    firstDecl = EnumSet.of(final_, ident, class_);
    firstMethodDecl = EnumSet.of(ident, void_);
    firstAssignOp = EnumSet.of(assign, plusas, minusas, timesas, slashas, remas);
    firstAddOp = EnumSet.of(plus, minus);
    firstMulOp = EnumSet.of(times, slash, rem);
    firstStatement = EnumSet.of(ident, if_, while_, break_, return_, read, print, lbrace, semicolon);
    firstExpr = EnumSet.of(ident, number, charConst, new_, lpar, minus);
    followStatement = EnumSet.of(rbrace, else_, eof);
    recoverDecl = EnumSet.of(final_, ident, class_, eof);
    recoverMethodDecl = EnumSet.of(ident, void_, eof);
    recoverStatement = EnumSet.of(if_, while_, break_, return_, read, print, semicolon, eof);
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

    Obj progObj = tab.insert(Obj.Kind.Prog, t.val, Tab.noType);

    tab.openScope();

    while(true) {
      if(sym == final_) {
        ConstDecl();
      }
      else if(sym == ident) {
        VarDecl();
      }
      else if(sym == class_) {
        ClassDecl();
      }
      else if(sym == lbrace || sym == eof) {
        break;
      }
      else {
        recoverDecl();
      }
    }

    // number of variables in this scope exceeds limit
    // at this position (before openScope()) as this is the global scope
    if(tab.curScope.nVars() > MAX_GLOBALS) {
      error(TOO_MANY_GLOBALS);
    }

    check(lbrace);

    while(true) {
      if(firstMethodDecl.contains(sym)) {
        MethodDecl();
      }
      else if(sym == rbrace || sym == eof) {
        break;
      }
      else {
        recoverMethodDecl();
      }
    }
    check(rbrace);

    progObj.locals = tab.curScope.locals(); // properly set program's local to the locals of the current scope before closing
    tab.closeScope();
  }

  /**
   * <code>ConstDecl = "final" Type ident "=" ( number | charConst ) ";".</code>
   */
  private void ConstDecl() {
    check(final_);
    Struct type = Type();
    check(ident);

    Obj con = tab.insert(Obj.Kind.Con, t.val, type); // new constant in table

    check(assign);

    if(sym == number) {
      if(type != Tab.intType) { // assignment is of type int, but const is not
        error(INCOMPATIBLE_TYPES);
      }
      scan();
    }
    else if(sym == charConst) {
      if(type != Tab.charType) { // assignment is of type char, but const is not
        error(INCOMPATIBLE_TYPES);
      }
      scan();
    }
    else {
      error(INVALID_CONST_TYPE);
    }

    con.val = t.numVal; // assign value to const

    check(semicolon);
  }

  /**
   * <code>VarDecl = Type ident { "," ident } ";".</code>
   */
  private void VarDecl() {
    Struct type = Type();
    check(ident);

    tab.insert(Obj.Kind.Var, t.val, type);

    while(sym == comma) {
      scan();
      check(ident);
      tab.insert(Obj.Kind.Var, t.val, type);
    }

    check(semicolon);
  }

  /**
   * <code>ClassDecl = "class" ident "{" { VarDecl } "}".</code>
   */
  private void ClassDecl() {
    check(class_);
    check(ident);
    Obj classObj = tab.insert(Obj.Kind.Type, t.val, new Struct(Struct.Kind.Class)); // insert program into symtab
    check(lbrace);

    tab.openScope();

    while(sym == ident) {
      VarDecl();
    }

    if(tab.curScope.nVars() > MAX_FIELDS) {
      error(TOO_MANY_FIELDS);
    }

    classObj.type.fields = tab.curScope.locals(); // assign scope locals as class fields

    check(rbrace);

    tab.closeScope();
  }

  /**
   * <code>( Type | "void" ) ident "(" [ FormPars ] ")"
   * { VarDecl } Block.</code>
   */
  private void MethodDecl() {
    Struct type = Tab.noType; // assume that initial type is void
    if(sym == ident) {
      type = Type();
    }
    else if(sym == void_) {
      scan();
    }
    else {
      error(INVALID_METHOD_DECL);
    }

    check(ident);

    String methName = t.val;
    Obj methObj = tab.insert(Obj.Kind.Meth, methName, type); // add method to symtab
    methObj.adr = code.pc;

    tab.openScope();

    check(lpar);

    if(sym == ident) {
      FormPars();
    }

    int methParams = tab.curScope.locals().size(); // get number of method parameters

    check(rpar);

    if(methName.equals("main")) {
      if(type != Tab.noType) { // main return type not void
        error(MAIN_NOT_VOID);
      }
      if(methParams > 0) { // main has parameters
        error(MAIN_WITH_PARAMS);
      }
    }

    while(sym == ident) {
      VarDecl();
    }

    if(tab.curScope.locals().size() > MAX_LOCALS) {
      error(TOO_MANY_LOCALS);
    }

    Block();

    methObj.nPars = methParams;
    methObj.locals = tab.curScope.locals();
    tab.closeScope();
  }

  /**
   * <code>FormPars = Type ident { "," Type ident }.</code>
   */
  private void FormPars() {
    Struct type = Type();
    check(ident);

    tab.insert(Obj.Kind.Var, t.val, type);

    while(sym == comma) {
      scan();
      type = Type();
      check(ident);
      tab.insert(Obj.Kind.Var, t.val, type);
    }
  }

  /**
   * <code>ident [ "[" "]" ].</code>
   */
  private Struct Type() {
    check(ident);
    Obj o = tab.find(t.val);
    if(o.kind != Obj.Kind.Type) {
      error(TYPE_EXPECTED);
    }
    Struct type = o.type;
    if(sym == lbrack) {
      scan();
      check(rbrack);
      type = new Struct(type); // create new array
    }
    return type;
  }

  /**
   * <code>Block = "{" { Statement } "}".</code>
   */
  private void Block() {
    check(lbrace);

    while(true) {
      if(firstStatement.contains(sym)) {
        Statement();
      }
      else if(followStatement.contains(sym)) {
        break;
      }
      else {
        recoverStat();
      }
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

      Obj factorNewObj = tab.find(t.val);

      if(factorNewObj.kind != Obj.Kind.Type) { // ident after "new" is not a type
        error(TYPE_EXPECTED);
      }

      if(sym == lbrack) {
        scan();
        Expr();
        check(rbrack);

        return; // exit to allow instance of array of int/char
      }

      if(factorNewObj.type.kind != Struct.Kind.Class) { // if factor is not an array, then it must be class
        error(CLASS_TYPE_EXPECTED);
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
    Obj designatorObj = tab.find(t.val);
    while(sym == period || sym == lbrack) {
      if(sym == lbrack) {
        scan();
        if(sym == tilde) {
          scan();
        }
        Expr();
        check(rbrack);

        // access to type of array elements
        designatorObj = new Obj(Obj.Kind.Var, "", designatorObj.type.elemType);
      }
      else { // "." is read
        scan();
        check(ident);
        tab.findField(t.val, designatorObj.type);
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
  private void recoverDecl() {
    error(DECLARATION_RECOVERY);
    do {
      scan();
    } while(!recoverDecl.contains(sym));
    errorDist = 0;
  }
  private void recoverMethodDecl() {
    error(METHOD_DECL_RECOVERY);
    do {
      scan();
    } while(!recoverMethodDecl.contains(sym));
    errorDist = 0;
  }
  private void recoverStat() {
    error(STATEMENT_RECOVERY);
    do {
      scan();
    } while(!recoverStatement.contains(sym));
    errorDist = 0;
  }

  // ====================================
  // ====================================
}
