package ssw.mj.impl;

import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Scope;
import ssw.mj.symtab.Struct;

public final class Tab {

  // Universe
  public static final Struct noType = new Struct(Struct.Kind.None);
  public static final Struct intType = new Struct(Struct.Kind.Int);
  public static final Struct charType = new Struct(Struct.Kind.Char);
  public static final Struct nullType = new Struct(Struct.Kind.Class);

  public final Obj noObj, chrObj;
  public Obj ordObj, lenObj;

  /**
   * Only used for reporting errors.
   */
  private final Parser parser;
  /**
   * The current top scope.
   */
  public Scope curScope = null;
  // First scope opening (universe) will increase this to -1
  /**
   * Nesting level of current scope.
   */
  private int curLevel = -2;

  public Tab(Parser p) {
    parser = p;

    // setting up "universe" (= predefined names)
    // opening scope (curLevel goes to -1, which is the universe level)
    openScope();

    noObj = new Obj(Obj.Kind.Var, "noObj", noType);

    insert(Obj.Kind.Type, "int", intType);
    insert(Obj.Kind.Type, "char", charType);
    insert(Obj.Kind.Con, "null", nullType);

    chrObj = insert(Obj.Kind.Meth, "chr", charType);
    openScope();
    Obj iVarObj = insert(Obj.Kind.Var, "i", intType);
    iVarObj.level = 1;
    chrObj.nPars = curScope.nVars();
    chrObj.locals = curScope.locals();
    closeScope();

    // TODO Exercise UE-P-4: build "ord" universe method and store in ordObj

    // TODO Exercise UE-P-4: build "len" universe method and store in lenObj

    // still on level -1
    // now that the universe is constructed, the next node that will be added is the Program itself
    // (which will open its own scope with level 0)
  }

  // ===============================================
  // TODO Exercise UE-P-4: implementation of symbol table
  // ===============================================

  public void openScope() {
    curScope = new Scope(curScope);
    curLevel++;
  }

  public void closeScope() {
    curScope = curScope.outer();
    curLevel--;
  }

  public Obj insert(Obj.Kind kind, String name, Struct type) {
    // TODO Exercise UE-P-4
    return noObj;
  }

  /**
   * Retrieves the object with <code>name</code> from the innermost scope.
   */
  public Obj find(String name) {
    // TODO Exercise UE-P-4
    return noObj;
  }

  /**
   * Retrieves the field <code>name</code> from the fields of
   * <code>type</code>.
   */
  public Obj findField(String name, Struct type) {
    // TODO Exercise UE-P-4
    return noObj;
  }

  // ===============================================
  // ===============================================
}
