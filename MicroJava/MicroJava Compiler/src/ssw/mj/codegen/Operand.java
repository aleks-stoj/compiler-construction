package ssw.mj.codegen;

import ssw.mj.impl.Code;
import ssw.mj.impl.Code.CompOp;
import ssw.mj.impl.Parser;
import ssw.mj.impl.Tab;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Struct;

import static ssw.mj.Errors.Message.ILLEGAL_OPERAND_KIND;

public class Operand {
  /**
   * Possible operands.
   */
  public enum Kind {
    Con(true),
    Local(false),
    Static(false),
    Stack(true),
    Fld(false),
    Elem(false),
    Meth(true),
    Cond(true),
    None(true);

    public final boolean isReadOnly;

    Kind(boolean isReadOnly) {
      this.isReadOnly = isReadOnly;
    }
  }

  /**
   * Kind of the operand.
   */
  public Kind kind;
  /**
   * The type of the operand (reference to symbol table).
   */
  public Struct type;
  /**
   * Only for Con: Value of the constant.
   */
  public int val;
  /**
   * Only for Local, Static, Fld, Meth: Offset of the element.
   */
  public int adr;
  /**
   * Only for Cond: Relational operator.
   */
  public CompOp op;
  /**
   * Only for Meth: Method object from the symbol table.
   */
  public Obj obj;
  /**
   * Only for Cond: Target for true jumps.
   */
  public Label tLabel;
  /**
   * Only for Cond: Target for false jumps.
   */
  public Label fLabel;

  /**
   * Constructor for named objects: constants, variables, methods
   */
  public Operand(Obj o, Parser parser) {
    type = o.type;
    val = o.val;
    adr = o.adr;
    switch (o.kind) {
      case Con -> kind = Kind.Con;
      case Var -> {
        if (o.level == 0) {
          kind = Kind.Static;
        } else {
          kind = Kind.Local;
        }
      }
      case Meth -> {
        kind = Kind.Meth;
        obj = o;
      }
      default -> {
        kind = Kind.None;
        parser.error(ILLEGAL_OPERAND_KIND, o.kind);
      }
    }
  }

  /**
   * Constructor for compare operations
   */
  public Operand(CompOp op, Code code) {
    this(code);
    this.kind = Kind.Cond;
    this.op = op;
  }

  public Operand(Code code) {
    tLabel = new Label(code);
    fLabel = new Label(code);
  }

  /**
   * Constructor for stack operands
   */
  public Operand(Struct type) {
    this.kind = Kind.Stack;
    this.type = type;
  }

  /**
   * Constructor for integer constants
   */
  public Operand(int x) {
    kind = Kind.Con;
    type = Tab.intType;
    val = x;
  }

  public boolean isReadOnly() {
    return kind.isReadOnly;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Op[");
    switch (kind) {
      case Con -> {
        sb.append(type).append(' ');
        sb.append(val);
      }
      case Local, Static, Fld -> {
        sb.append(kind).append(' ');
        sb.append(type).append(' ');
        sb.append(adr);
      }
      case Cond -> sb.append(op);
      case Meth -> sb.append(obj);
      case Elem, Stack -> {
        sb.append(kind).append(' ');
        sb.append(type);
      }
    }
    return sb.append(']').toString();
  }

}
