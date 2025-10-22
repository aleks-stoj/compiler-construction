package ssw.mj.test.support;

import ssw.mj.impl.Tab;
import ssw.mj.symtab.Obj;
import ssw.mj.symtab.Scope;
import ssw.mj.symtab.Struct;

import java.util.Collection;

public class SymTabDumper {
  public static String dump(Tab tab) {
    StringBuilder sb = new StringBuilder();
    if (tab.curScope != null) {
      dump(tab.curScope, sb);
    }
    return sb.toString();
  }

  private static void dump(Scope scope, StringBuilder sb) {
    sb.append("-- begin scope (").append(scope.nVars()).append(" variables) --\n");
    if (!scope.locals().isEmpty()) {
      dump(scope.locals().values(), sb, "");
    }
    if (scope.outer() != null) {
      sb.append("\n");
      dump(scope.outer(), sb);
    }
  }

  private static void dump(Collection<Obj> objects, StringBuilder sb, String indent) {
    for (Obj obj : objects) {
      dump(obj, sb, indent);
    }
  }

  private static void dump(Obj obj, StringBuilder sb, String indent) {
    sb.append(indent);

    switch (obj.kind) {
      case Con -> dumpCon(obj, sb, indent);
      case Var -> dumpVar(obj, sb, indent);
      case Type -> dumpType(obj, sb, indent);
      case Meth -> dumpMethod(obj, sb, indent);
      case Prog -> dumpProgram(obj, sb);
    }

    if (obj.locals != null) {
      sb.append("\n");
      dump(obj.locals.values(), sb, indent + "  ");
    }
    sb.append("\n");
  }

  private static void dumpCon(Obj obj, StringBuilder sb, String indent) {
    sb.append("Constant: ");
    if (obj.type != null) {
      dump(obj.type, sb, indent, false);
    }
    sb.append(" ").append(obj.name).append(" = ");
    if (obj.type == Tab.charType) {
      sb.append("'").append((char) obj.val).append("'");
    } else {
      sb.append(obj.val);
    }
  }

  private static void dumpVar(Obj obj, StringBuilder sb, String indent) {
    if (obj.level == 0) {
      sb.append("Global Variable ");
    } else {
      sb.append("Local Variable ");
    }
    sb.append(obj.adr).append(": ");
    if (obj.type != null) {
      dump(obj.type, sb, indent, false);
    }
    sb.append(" ").append(obj.name);
  }

  private static void dumpType(Obj type, StringBuilder sb, String indent) {
    sb.append("Type ").append(type.name).append(": ");
    if (type.type != null) {
      dump(type.type, sb, indent + "  ", true);
    }
  }

  private static void dumpMethod(Obj meth, StringBuilder sb, String indent) {
    sb.append("Method: ");
    if (meth.type != null) {
      dump(meth.type, sb, indent, false);
    }
    sb.append(" ").append(meth.name).append(" (").append(meth.locals.size()).append(" locals, ").append(meth.nPars).append(" parameters").append(")");
  }

  private static void dumpProgram(Obj obj, StringBuilder sb) {
    sb.append("Program ").append(obj.name).append(":");
  }

  private static void dump(Struct struct, StringBuilder sb, String indent, boolean dumpFields) {
    switch (struct.kind) {
      case None -> sb.append("void");
      case Int -> sb.append("int");
      case Char -> sb.append("char");
      case Arr -> {
        if (struct.elemType != null) {
          dump(struct.elemType, sb, indent, dumpFields);
        }
        sb.append("[]");
      }
      case Class -> {
        sb.append("class (").append(struct.nrFields()).append(" fields)");
        if (dumpFields && struct.fields != null) {
          sb.append("\n");
          dump(struct.fields.values(), sb, indent);
        }
      }
    }
  }
}
