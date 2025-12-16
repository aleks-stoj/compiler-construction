package ssw.mj.impl;

import ssw.mj.Errors.Message;
import ssw.mj.codegen.Operand;
import ssw.mj.scanner.Token;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Struct;

import java.util.EnumSet;
import java.util.Objects;

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

    // check if main exists
    if(code.mainpc == -1) {
      error(MAIN_NOT_FOUND);
    }

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

    code.dataSize = tab.curScope.nVars();
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

    code.put(Code.OpCode.enter);
    code.put(methObj.nPars); // enter first argument

    if(methName.equals("main")) {
      if(type != Tab.noType) { // main return type not void
        error(MAIN_NOT_VOID);
      }
      if(methParams > 0) { // main has parameters
        error(MAIN_WITH_PARAMS);
      }
      code.mainpc = methObj.adr;
    }

    while(sym == ident) {
      VarDecl();
    }

    code.put(tab.curScope.locals().size()); // enter second argument

    if(tab.curScope.locals().size() > MAX_LOCALS) {
      error(TOO_MANY_LOCALS);
    }

    Block();

    methObj.nPars = methParams;
    methObj.locals = tab.curScope.locals();
    tab.closeScope();

    if(type == Tab.noType) { // void methods
      code.put(Code.OpCode.exit);
      code.put(Code.OpCode.return_);
    }
    else { // non-void methods
      code.put(Code.OpCode.trap);
      code.put(1);
    }
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
    Operand x;
    if(sym == ident) {
      x = Designator();
      if(firstAssignOp.contains(sym)) {
        if(x.isReadOnly()) { // designator is read-only; attempting to write to it causes an error
          error(CANNOT_STORE_TO_READONLY, x.kind);
        }
        Code.OpCode assignmentOp = AssignOp();

        if(assignmentOp != Code.OpCode.nop) { // prepare LHS (if Field or Array Element) before continuing with RHS of statement
          code.prepareLhsOfCompoundAssignment(x);
        }

        Operand y = Expr();

        if(!y.type.assignableTo(x.type)) {
          error(INCOMPATIBLE_TYPES);
        }

        if(assignmentOp != Code.OpCode.nop) { // compound assign
          code.load(y);
          code.put(assignmentOp);
          code.assign(x, new Operand(x.type)); // get calculated value from stack and assign to LHS
        }
        else { // regular assign (=)
          code.assign(x, y);
        }
      }
      else if(sym == lpar) {
        ActPars();
      }
      else if(sym == pplus) {
        if(x.type != Tab.intType) {
          error(INC_DEC_EXPECTS_INT);
        }
        if(x.isReadOnly()) { // designator is read-only; attempting to write to it causes an error
          error(CANNOT_STORE_TO_READONLY, x.kind);
        }
        scan();
        code.inc(x, 1);
      }
      else if(sym == mminus) {
        if(x.type != Tab.intType) {
          error(INC_DEC_EXPECTS_INT);
        }
        if(x.isReadOnly()) { // designator is read-only; attempting to write to it causes an error
          error(CANNOT_STORE_TO_READONLY, x.kind);
        }
        scan();
        code.inc(x, -1);
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
      x = Designator();
      // x = int => read
      // x = char => bread
      if(x.type != Tab.intType && x.type != Tab.charType) {
        error(ILLEGAL_READ_ARGUMENT);
      }
      if(x.type == Tab.intType) {
        code.put(Code.OpCode.read);
      }
      else if(x.type == Tab.charType) {
        code.put(Code.OpCode.bread);
      }

      code.assign(x, new Operand(x.type)); // get value from stack

      check(rpar);
      check(semicolon);
    }
    else if(sym == print) {
      Operand width = new Operand(0); // optional width
      scan();
      check(lpar);
      x = Expr();

      code.load(x);

      if(x.type != Tab.intType && x.type != Tab.charType) {
        error(ILLEGAL_PRINT_ARGUMENT);
      }

      if(sym == comma) {
        scan();
        check(number);
        width = new Operand(t.numVal); // optional width
      }

      code.load(width);

      if(x.type == Tab.intType) {
        code.put(Code.OpCode.print);
      }
      else if(x.type == Tab.charType) {
        code.put(Code.OpCode.bprint);
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

  private Code.OpCode AssignOp() {
    Code.OpCode op = Code.OpCode.nop;
    switch (sym) {
      case assign: scan(); break;
      case plusas: scan(); op = Code.OpCode.add; break;
      case minusas: scan(); op = Code.OpCode.sub; break;
      case timesas: scan(); op = Code.OpCode.mul; break;
      case slashas: scan(); op = Code.OpCode.div; break;
      case remas: scan(); op = Code.OpCode.rem; break;
      default: error(INVALID_ASSIGN_OP);
    }
    return op;
  }

  private void ActPars() {
    check(lpar);

    if(firstExpr.contains(sym)) {
      Operand x = Expr();
      code.load(x); // load onto estack
      while (sym == comma) {
        scan();
        x = Expr();
        code.load(x); // load onto estack
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
    Operand x = Expr();
    Relop();
    Operand y = Expr();
    if(!y.type.compatibleWith(x.type)) {
      error(INCOMPATIBLE_TYPES);
    }
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

  private Operand Expr() {
    boolean isNeg = false; // expr is pre-appended with "-"
    if(sym == minus) {
      scan();
      isNeg = true;
    }

    Operand x = Term();

    if(isNeg) {
      if(x.type != Tab.intType) {
        error(UNARY_MINUS_EXPECTS_INT);
      }

      if(x.kind == Operand.Kind.Con) {
        x.val = x.val * -1;
      }
      else {
        code.load(x);
        code.put(Code.OpCode.neg);
        x = new Operand(x.type); // retrieve negative value from stack
      }
    }

    while(firstAddOp.contains(sym)) {
      Code.OpCode op = AddOp();
      code.load(x);
      Operand y = Term();
      if(x.type != Tab.intType || y.type != Tab.intType) {
        error(INCOMPATIBLE_TYPES);
      }
      code.load(y);
      code.put(op); // perform addition/subtraction of x and y
      x = new Operand(x.type); // get addop value from stack
    }

    return x;
  }

  private Operand Term() {
    Operand x = Factor();

    while(firstMulOp.contains(sym)) {
      Code.OpCode op = MulOp();
      code.load(x);
      Operand y = Factor();
      if(x.type != Tab.intType || y.type != Tab.intType) {
        error(INCOMPATIBLE_TYPES);
      }
      code.load(y);
      code.put(op);
      x = new Operand(x.type); // get mulop value from stack
    }

    return x;
  }

  private Operand Factor() {
    Operand x;
    if(sym == ident) {
      x = Designator();
      if(sym == lpar) {
        if(x.kind == Operand.Kind.Meth && x.type == Tab.noType) {
          error(VOID_CALL_IN_EXPRESSION);
        }
        ActPars();
        return new Operand(x.type); // return operand with return type of method
      }
      return x;
    }
    else if(sym == number) {
      scan();
      x = new Operand(t.numVal);
      return x;
    }
    else if(sym == charConst) {
      scan();
      x = new Operand(t.numVal);
      x.type = Tab.charType; // explicitly override type to char to get bprint/bread
      return x;
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
        Operand y = Expr();

        // array size not of type int
        if(y.type != Tab.intType) {
          error(ARRAY_SIZE_EXPECTS_INT);
        }

        check(rbrack);

        code.load(y);
        code.put(Code.OpCode.newarray);

        // Differentiate n elems as required per MicroJava spec
        if(factorNewObj.type == Tab.charType) { // byte size for char
          code.put(0);
        }
        else { // word size for everything else
          code.put(1);
        }

        // exit to allow instance of array of int/char
        return new Operand(new Struct(factorNewObj.type));
      }

      if(factorNewObj.type.kind != Struct.Kind.Class) { // if factor is not an array, then it must be class
        error(CLASS_TYPE_EXPECTED);
      }

      // instantiate class
      code.put(Code.OpCode.new_);
      code.put2(factorNewObj.type.fields.size());

      return new Operand(factorNewObj.type);
    }
    else if(sym == lpar) {
      scan();
      x = Expr();

      // method call in expr, but method returns void
      if(x.kind == Operand.Kind.Meth && x.type == Tab.noType) {
        error(VOID_CALL_IN_EXPRESSION);
      }

      check(rpar);
      return x;
    }
    else {
      error(INVALID_FACTOR);
    }
    return new Operand(Tab.intType); // return operand of type int to prevent further errors
  }

  /**
   * <code>Designator = ident { "." ident | "[" [ "~" ] Expr "]" }.</code>
   */
  private Operand Designator() {
    check(ident);
    Obj designatorObj = tab.find(t.val);
    Operand x = new Operand(designatorObj, this);
    while(sym == period || sym == lbrack) {
      if(sym == lbrack) {
        // in case operand is not array
        // change kind and type to prevent further errors
        if(!Objects.equals(x.type.kind, Struct.Kind.Arr)) {
          error(INDEXED_ACCESS_TO_NON_ARRAY);
          x.kind = Operand.Kind.None;
          x.type = Tab.noType;
        }
        scan();
        boolean indexFromEnd = false;

        code.load(x);

        // index-from-end
        if(sym == tilde) {
          scan();
          indexFromEnd = true;
          code.put(Code.OpCode.dup);
          code.put(Code.OpCode.arraylength);
          //code.load(new Operand(t.numVal));
        }

        Operand y = Expr();

        if(y.type != Tab.intType) {
          error(ARRAY_INDEX_EXPECTS_INT);
        }

        code.load(y);


        //Expr();

        /*if(x.kind != Operand.Kind.Elem) { // attempting to access a non-array via an index

        } */

        check(rbrack);

        if(indexFromEnd) code.put(Code.OpCode.sub); // a[i - n]

        x.kind = Operand.Kind.Elem;
        x.type = x.type.elemType;

        // access to type of array elements
        designatorObj = new Obj(Obj.Kind.Var, "", designatorObj.type.elemType);
      }
      else { // "." is read
        if(x.type.kind != Struct.Kind.Class) {
          error(FIELD_ACCESS_TO_NON_CLASS);
         }

        code.load(x); // load variable before field access
        scan();
        check(ident);

        Obj obj = tab.findField(t.val, x.type);
        x.kind = Operand.Kind.Fld;
        x.type = obj.type;
        x.adr = obj.adr;
      }
    }
    return x;
  }

  private Code.OpCode AddOp() {
    Code.OpCode op = Code.OpCode.nop; // nop in case sym is no add operation. prevents accidentally doing something wrong
    switch (sym) {
      case plus: scan(); op = Code.OpCode.add; break;
      case minus: scan(); op = Code.OpCode.sub; break;
      default: error(INVALID_ADD_OP);
    }
    return op;
  }

  private Code.OpCode MulOp() {
    Code.OpCode op = Code.OpCode.nop; // nop in case sym is no mul operation. prevents accidentally doing something wrong
    switch (sym) {
      case times: scan(); op = Code.OpCode.mul; break;
      case slash: scan(); op = Code.OpCode.div; break;
      case rem: scan(); op = Code.OpCode.rem; break;
      default: error(INVALID_MUL_OP);
    }
    return op;
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
