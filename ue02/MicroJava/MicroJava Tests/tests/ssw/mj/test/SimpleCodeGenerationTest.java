package ssw.mj.test;

import org.junit.jupiter.api.Test;
import ssw.mj.codegen.Operand;
import ssw.mj.symtab.Obj;
import ssw.mj.test.support.BaseCompilerTestCase;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static ssw.mj.Errors.Message.*;

public class SimpleCodeGenerationTest extends BaseCompilerTestCase {

  /**
   * Symbol table for most examples of this test class.
   */
  private void expectSymTab() {
    expectSymTabUniverse();
    expectSymTab("Program A:");
    expectSymTab("  Constant: int max = 12");
    expectSymTab("  Global Variable 0: char c");
    expectSymTab("  Global Variable 1: int i");
    expectSymTab("  Type B: class (2 fields)");
    expectSymTab("    Local Variable 0: int x");
    expectSymTab("    Local Variable 1: int y");
    expectSymTab("  Method: void main (3 locals, 0 parameters)");
    expectSymTab("    Local Variable 0: int[] iarr");
    expectSymTab("    Local Variable 1: class (2 fields) b");
    expectSymTab("    Local Variable 2: int n");
  }

  @Test
  public void undefNameMeth() {
    initCode("program Test {" + LF + //
            "  void main() {" + LF + //
            "    method();" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 11, NAME_NOT_FOUND, "method");
    parseVerifyVisualize();
  }

  @Test
  public void forwardDeclErrorMissingMethod() {
    initCode("program Test" + LF + // 1
            "{" + LF + // 2
            "  void main() { foo(); }" + LF + // 3
            "  void foo() {}" + LF + // 4
            "}" + LF // 5
    );
    expectError(3, 20, NAME_NOT_FOUND, "foo");

    parseVerifyVisualize();
  }

  @Test
  public void undefNameVar() {
    initCode("program Test {" + LF + //
            "  void main() {" + LF + //
            "    var++;" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 8, NAME_NOT_FOUND, "var");
    parseVerifyVisualize();
  }

  @Test
  public void bspEmpty() {
    initCode("program A" + LF + //
            "  final int max = 12;" + LF + //
            "  char c; int i;" + LF + //
            "  class B { int x, y; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    int[] iarr; B b; int n;" + LF + //
            "  {" + LF + //
            "  }" + LF + //
            "}");

    expectSymTab();

    parseVerifyVisualize();
  }

  @Test
  public void bsp01() {
    initCode("program A" + LF + //
            "  final int max = 12;" + LF + //
            "  char c; int i;" + LF + //
            "  class B { int x, y; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    int[] iarr; B b; int n;" + LF + //
            "  {" + LF + //
            "    n = 3;" + LF + //
            "    print(n); " + LF + //
            "  }" + LF + //
            "}");

    expectSymTab();
    addExpectedRun("3");
    parseVerifyVisualize();
  }

  @Test
  public void bsp01a() {
    initCode("program A" + LF + //
            "  final int max = 12;" + LF + //
            "  char c; int i;" + LF + //
            "  class B { int x, y; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    int[] iarr; B b; int n;" + LF + //
            "  {" + LF + //
            "    n = -1 + 2;" + LF + //
            "    print(n); " + LF + //
            "  }" + LF + //
            "}");

    expectSymTab();
    addExpectedRun("1");
    parseVerifyVisualize();
  }

  @Test
  public void bsp02() {
    initCode("program A" + LF + //
            "  final int max = 12;" + LF + //
            "  char c; int i;" + LF + //
            "  class B { int x, y; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    int[] iarr; B b; int n;" + LF + //
            "  {" + LF + //
            "    i = 10;" + LF + //
            "    print(i);" + LF + //
            "  }" + LF + //
            "}");

    expectSymTab();
    addExpectedRun("10");
    parseVerifyVisualize();
  }

  @Test
  public void bsp03() {
    initCode("program A" + LF + //
            "  final int max = 12;" + LF + //
            "  char c; int i;" + LF + //
            "  class B { int x, y; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    int[] iarr; B b; int n;" + LF + //
            "  {" + LF + //
            "    i = 1;" + LF + //
            "    n = 3 + i;" + LF + //
            "    print(n);" + LF + //
            "  }" + LF + //
            "}");

    expectSymTab();
    addExpectedRun("4");
    parseVerifyVisualize();
  }

  @Test
  public void bsp04() {
    initCode("program A" + LF + //
            "  final int max = 12;" + LF + //
            "  char c; int i;" + LF + //
            "  class B { int x, y; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    int[] iarr; B b; int n;" + LF + //
            "  {" + LF + //
            "    i = 1;" + LF + //
            "    n = 3 + i * max - n;" + LF + //
            "    print(n);" + LF + //
            "  }" + LF + //
            "}");

    expectSymTab();
    addExpectedRun("15");
    parseVerifyVisualize();
  }

  @Test
  public void bsp05() {
    initCode("program A" + LF + //
            "  final int max = 12;" + LF + //
            "  char c; int i;" + LF + //
            "  class B { int x, y; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    int[] iarr; B b; int n;" + LF + //
            "  {" + LF + //
            "    iarr = new int[10];" + LF + //
            "    iarr[5] = 10;" + LF + //
            "    print(iarr[0]);" + LF + //
            "    print(iarr[5]);" + LF + //
            "  }" + LF + //
            "}");

    expectSymTab();

    addExpectedRun("010");
    parseVerifyVisualize();
  }

  @Test
  public void bsp06() {
    initCode("program A" + LF + //
            "  final int max = 12;" + LF + //
            "  char c; int i;" + LF + //
            "  class B { int x, y; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    int[] iarr; B b; int n;" + LF + //
            "  {" + LF + //
            "    iarr = new int[10];" + LF + //
            "    iarr[5] = 10;" + LF + //
            "    b = new B;" + LF + //
            "    b.y = iarr[5] * 3;" + LF + //
            "    print(b.y);" + LF + //
            "  }" + LF + //
            "}");

    expectSymTab();
    addExpectedRun("30");

    parseVerifyVisualize();
  }

  @Test
  public void bsp07() {
    initCode("program A" + LF + //
            "  final int max = 12;" + LF + //
            "  char c; int i;" + LF + //
            "  class B { int x, y; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    int[] iarr; B b; int n;" + LF + //
            "  {" + LF + //
            "    n--;" + LF + //
            "    print(n);" + LF + //
            "  }" + LF + //
            "}");

    expectSymTab();
    addExpectedRun("-1");
    parseVerifyVisualize();
  }

  @Test
  public void bsp08() {
    initCode("program A" + LF + //
            "  final int max = 12;" + LF + //
            "  char c; int i;" + LF + //
            "  class B { int x, y; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    int[] iarr; B b; int n;" + LF + //
            "  {" + LF + //
            "    i--;" + LF + //
            "    print(i);" + LF + //
            "  }" + LF + //
            "}");

    expectSymTab();
    addExpectedRun("-1");
    parseVerifyVisualize();
  }

  @Test
  public void bsp09() {
    initCode("program A" + LF + //
            "  final int max = 12;" + LF + //
            "  char c; int i;" + LF + //
            "  class B { int x, y; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    int[] iarr; B b; int n;" + LF + //
            "  {" + LF + //
            "    b = new B;" + LF + //
            "    b.y--;" + LF + //
            "    print(b.y);" + LF + //
            "  }" + LF + //
            "}");

    expectSymTab();
    addExpectedRun("-1");
    parseVerifyVisualize();
  }

  @Test
  public void bsp10() {
    initCode("program A" + LF + //
            "  final int max = 12;" + LF + //
            "  char c; int i;" + LF + //
            "  class B { int x, y; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    int[] iarr; B b; int n;" + LF + //
            "  {" + LF + //
            "    iarr = new int[10];" + LF + //
            "    iarr[0]--;" + LF + //
            "    print(iarr[0]);" + LF + //
            "  }" + LF + //
            "}");

    expectSymTab();
    addExpectedRun("-1");
    parseVerifyVisualize();
  }

  // ---- Errors in Code.java
  @Test
  public void noVarMethod() {
    initCode("program Test {" + LF + //
            "  int method() { return 0; }" + LF + //
            "  void main() int i; {" + LF + //
            "    method = i;" + LF + //
            "  }" + LF + //
            "}");
    expectError(4, 12, CANNOT_STORE_TO_READONLY, Operand.Kind.Meth.name());
    parseVerifyVisualize();
  }

  @Test
  public void noVarIncMethod() {
    initCode("program Test {" + LF + //
            "  int method() { return 0; }" + LF + //
            "  void main() int i; {" + LF + //
            "    method++;" + LF + //
            "  }" + LF + //
            "}");
    expectError(4, 11, CANNOT_STORE_TO_READONLY, Operand.Kind.Meth.name());
    parseVerifyVisualize();
  }

  @Test
  public void noOperand() {
    initCode("program Test {" + LF + //
            "  void main() int i; {" + LF + //
            "    Test = i;" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 10, ILLEGAL_OPERAND_KIND, Obj.Kind.Prog);
    parseVerifyVisualize();
  }

  @Test
  public void noValueAssign() {
    initCode("program Test {" + LF + //
            "  char method() { return 'a'; }" + LF + //
            "  void main() char c; {" + LF + //
            "    c = method;" + LF + //
            "  }" + LF + //
            "}");
    expectError(4, 15, CANNOT_LOAD_OPERAND);
    parseVerifyVisualize();
  }

  @Test
  public void noValueCalc() {
    initCode("program Test {" + LF + //
            "  int method() { return 0; }" + LF + //
            "  void main() int i; {" + LF + //
            "    i = 5 * method;" + LF + //
            "  }" + LF + //
            "}");
    expectError(4, 19, CANNOT_LOAD_OPERAND);
    parseVerifyVisualize();
  }

  @Test
  public void noValueInc() {
    initCode("program Test {" + LF + //
            "  int method() { return 0; }" + LF + //
            "  void main() int i; {" + LF + //
            "    i += method;" + LF + //
            "  }" + LF + //
            "}");
    expectError(4, 16, CANNOT_LOAD_OPERAND);
    parseVerifyVisualize();
  }

  @Test
  public void assignPlusNoIntOp() {
    initCode("program Test {" + LF + //
            "  void main() int i; char c; {" + LF + //
            "    c += i;" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 11, INCOMPATIBLE_TYPES);
    parseVerifyVisualize();
  }

  @Test
  public void assignTimesNoIntOp() {
    initCode("program Test {" + LF + //
            "  void main() int i; char c; {" + LF + //
            "    i *= c;" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 11, INCOMPATIBLE_TYPES);
    parseVerifyVisualize();
  }

  @Test
  public void incompTypes() {
    initCode("program Test {" + LF + //
            "  void main() int i; {  " + LF + //
            "    i = null;" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 13, INCOMPATIBLE_TYPES);
    parseVerifyVisualize();
  }

  @Test
  public void incompTypesArr() {
    initCode("program Test {" + LF + //
            "  void main() int[] ia; char[] ca; {  " + LF + //
            "    ia = ca;" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 12, INCOMPATIBLE_TYPES);
    parseVerifyVisualize();
  }

  @Test
  public void incompTypesClass() {
    initCode("program Test" + LF + //
            "  class C1 { }" + LF + //
            "  class C2 { }" + LF + //
            "{" + LF + //
            "  void main() C1 c1; C2 c2; {  " + LF + //
            "    c1 = c2;" + LF + //
            "  }" + LF + //
            "}");
    expectError(6, 12, INCOMPATIBLE_TYPES);
    parseVerifyVisualize();
  }

  @Test
  public void noIntegerInc() {
    initCode("program Test {" + LF + //
            "  void main() char ch; {" + LF + //
            "    ch++;" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 7, INC_DEC_EXPECTS_INT);
    parseVerifyVisualize();
  }

  @Test
  public void noIntegerDec() {
    initCode("program Test {" + LF + //
            "  void main() int[] ia; {" + LF + //
            "    ia--;" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 7, INC_DEC_EXPECTS_INT);
    parseVerifyVisualize();
  }

  @Test
  public void wrongReadValue() {
    initCode("program Test {" + LF + //
            "  void main() int[] ia; { " + LF + //
            "    read(ia);" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 12, ILLEGAL_READ_ARGUMENT);
    parseVerifyVisualize();
  }

  @Test
  public void wrongPrintValue() {
    initCode("program Test" + LF + //
            "  class C { }" + LF + //
            "{" + LF + //
            "  void main() C obj; { " + LF + //
            "    print(obj);" + LF + //
            "  }" + LF + //
            "}");
    expectError(5, 14, ILLEGAL_PRINT_ARGUMENT);
    parseVerifyVisualize();
  }

  @Test
  public void noIntUnaryMinus() {
    initCode("program Test {" + LF + //
            "  void main() int i; char c; {" + LF + //
            "    i = -c;" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 11, UNARY_MINUS_EXPECTS_INT);
    parseVerifyVisualize();
  }

  @Test
  public void noIntOpPlus() {
    initCode("program Test {" + LF + //
            "  void main() int i; int[] ia; {" + LF + //
            "    i = i + ia;" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 15, INCOMPATIBLE_TYPES);
    parseVerifyVisualize();
  }

  @Test
  public void noIntOpTimes() {
    initCode("program Test {" + LF + //
            "  void main() int i; int[] ia; {" + LF + //
            "    i = ia * i;" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 15, INCOMPATIBLE_TYPES);
    parseVerifyVisualize();
  }

  @Test
  public void procAsFunc() {
    initCode("program Test {" + LF + //
            "  void main() int x; {" + LF + //
            "    x = main();" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 13, VOID_CALL_IN_EXPRESSION);
    parseVerifyVisualize();
  }

  @Test
  public void noTypeNew() {
    initCode("program Test {" + LF + //
            "  void main() int i; {" + LF + //
            "    i = new main;" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 17, TYPE_EXPECTED);
    parseVerifyVisualize();
  }

  @Test
  public void wrongArraySize() {
    initCode("program Test {" + LF + //
            "  void main() int[] ia; {" + LF + //
            "    ia = new int[ia];" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 20, ARRAY_SIZE_EXPECTS_INT);
    parseVerifyVisualize();
  }

  @Test
  public void noClass() {
    initCode("program Test {" + LF + //
            "  void main() int i; {" + LF + //
            "    i = i.i;" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 10, FIELD_ACCESS_TO_NON_CLASS);
    parseVerifyVisualize();
  }

  @Test
  public void noArrayIndex() {
    initCode("program Test {" + LF + //
            "  void main() int[] ia; {" + LF + //
            "    ia[ia] = 1;" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 10, ARRAY_INDEX_EXPECTS_INT);
    parseVerifyVisualize();
  }

  @Test
  public void noArray() {
    initCode("program Test {" + LF + //
            "  void main() char c; int i; {" + LF + //
            "    print(c[i]);" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 12, INDEXED_ACCESS_TO_NON_ARRAY);
    expectError(3, 15, CANNOT_LOAD_OPERAND);
    parseVerifyVisualize();
  }

  @Test
  public void noTypeNewArray() {
    initCode("program A {" + LF + //
            "  void main () {" + LF + //
            "    print(len(new null[10]));" + LF + //
            "  }" + LF + //
            "}");
    expectError(3, 23, TYPE_EXPECTED);
    parseVerifyVisualize();
  }

  @Test
  public void testPrint() {
    initCode("program A {" + LF + //
            "  void main () {" + LF + //
            "	 print('a');" + LF + //
            "    print('b',1);" + LF + //
            "    print('c',2);" + LF + //
            "    print('d',3);" + LF + //
            "    print('e',4);" + LF + //
            "  }" + LF + //
            "}");
    addExpectedRun("ab c  d   e");
    parseVerifyVisualize();
  }

  @Test
  public void testDesignator() {
    initCode("program A" + LF + //
            "  class A { int x; }" + LF + //
            "  class B { A a; }" + LF + //
            "  class C { B b; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    A a; B b; C c;" + LF + //
            "  {" + LF + //
            "    c = new C;" + LF + //
            "    c.b = new B;" + LF + //
            "    c.b.a = new A;" + LF + //
            "    c.b.a.x++;" + LF + //
            "    print(c.b.a.x);" + LF + //
            "  }" + LF + //
            "}");

    addExpectedRun("1");
    parseVerifyVisualize();
  }

  @Test
  public void testArrayAndDesignator() {
    initCode("program A" + LF + //
            "  class A { int[] x; }" + LF + //
            "  class B { A a; }" + LF + //
            "  class C { B b; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    A a; B b; C[] c;" + LF + //
            "  {" + LF + //
            "    c = new C[5];" + LF + //
            "    c[0] = new C;" + LF + //
            "    c[0].b = new B;" + LF + //
            "    c[0].b.a = new A;" + LF + //
            "    c[0].b.a.x = new int[10];" + LF + //
            "    c[3] = new C;" + LF + //
            "    c[3].b = new B;" + LF + //
            "    c[3].b.a = new A;" + LF + //
            "    c[3].b.a.x = new int[30];" + LF + //
            "    c[0].b.a.x[0]--;" + LF + //
            "    c[0].b.a.x[8]++;" + LF + //
            "    c[3].b.a.x[2]++;" + LF + //
            "    c[3].b.a.x[2]*=3;" + LF + //
            "    c[0].b.a.x[8]+=50 + c[3].b.a.x[2] * c[3].b.a.x[2] * c[0].b.a.x[0];" + LF + //
            "    print(c[0].b.a.x[8]);" + LF + //
            "  }" + LF + //
            "}");

    addExpectedRun("42");
    parseVerifyVisualize();
  }

  @Test
  public void testArrayAndDesignatorAndAssign() {
    initCode("program A" + LF + //
            "  class A { int[] x; }" + LF + //
            "  class B { A a; }" + LF + //
            "  class C { B b; }" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    A a; B b; C[] c;" + LF + //
            "  {" + LF + //
            "    c = new C[5];" + LF + //
            "    c[0] = new C;" + LF + //
            "    c[0].b = new B;" + LF + //
            "    c[0].b.a = new A;" + LF + //
            "    c[0].b.a.x = new int[2];" + LF + //
            "    c[3] = new C;" + LF + //
            "    c[3].b = new B;" + LF + //
            "    c[3].b.a = new A;" + LF + //
            "    c[3].b.a.x = new int[3];" + LF + //
            "    c[0].b.a.x[1]++;" + LF + //
            "    c[0].b.a.x[1]*=256;" + LF + //
            "    c[0].b.a.x[1]/=2;" + LF + //
            "    c[0].b.a.x[1]--;" + LF + //
            "    c[0].b.a.x[1]%=64;" + LF + //
            "    c[3].b.a.x[2]++;" + LF + //
            "    c[3].b.a.x[2]*=21;" + LF + //
            "    c[0].b.a.x[1]-=c[3].b.a.x[2];" + LF + //
            "    print(c[0].b.a.x[1]);" + LF + //
            "  }" + LF + //
            "}");

    addExpectedRun("42");
    parseVerifyVisualize();
  }

  @Test
  public void testArrayIndexExpression() {
    initCode("program A" + LF + // 1
            "{" + LF + // 2
            "  void main()" + LF + // 3
            "    int[] arr;" + LF + // 4
            "  {" + LF + // 5
            "    arr = new int[10];" + LF + // 6
            "    arr[ ( 1 + 2 ) * 3 ] = 4;" + LF + // 7
            "    arr[ 4 - 2 * 2 ] = 2;" + LF + // 8
            "    print(arr[ 90 / 10 ]);" + LF + // 9
            "    print(arr[ 6 - 3 * 2 ]);" + LF + // 10
            "  }" + LF + // 11
            "}");
    addExpectedRun("42");
    parseVerifyVisualize();
  }

  @Test
  public void testReadAndPrint() {
    initCode("program A" + LF + // 1
            "{" + LF + // 2
            "  void main()" + LF + // 3
            "    int n;" + LF + // 4
            "  {" + LF + // 5
            "    n = 0;" + LF + // 6
            "    read(n);" + LF + // 7
            "    print(n);" + LF + // 7
            "  }" + LF + // 9
            "}");
    addExpectedRun("2", "2");
    parseVerifyVisualize();
  }

  @Test
  public void testFields() {
    initCode("program A" + LF + //
            "  class A { int x; }" + LF + //
            "  class B { A a; }" + LF + //
            "{" + LF + //
            "  void main()" + LF + //
            "    A a;" + LF + //
            "    B b;" + LF + //
            "  {" + LF + //
            "    a = new A;" + LF + //
            "    b = new B;" + LF + //
            "    a.x = 20;" + LF + //
            "    a.x++;" + LF + //
            "    a.x /= 7;" + LF + //
            "    a.x *= a.x;" + LF + //
            "    a.x %= a.x - 5;" + LF + //
            "    b.a = new A;" + LF + //
            "    b.a.x = -12;" + LF + //
            "    b.a.x -= a.x;" + LF + //
            "    b.a.x *= -a.x;" + LF + //
            "    b.a.x %= 5;" + LF + //
            "    b.a.x *= a.x + 2 * 3;" + LF + //
            "    print(b.a.x);" + LF + //
            "  }" + LF + //
            "}");
    addExpectedRun("21");
    parseVerifyVisualize();
  }

  @Test
  public void twoMethods() {
    initCode("program A" + LF + // 1
            "{" + LF + // 2
            "  void deadMethodToMoveMainPcFrom0()" + LF + // 3
            "    int n;" + LF + // 4
            "  {" + LF + // 5
            "    n = 0;" + LF + // 6
            "  }" + LF + // 7
            "  void main()" + LF + // 8
            "  {" + LF + // 9
            "    print(2);" + LF + // 10
            "  }" + LF + // 11
            "}");
    addExpectedRun("2");
    parseVerifyVisualize();
    assertTrue(
            parser.code.mainpc > 0,
            "In this example mainpc must be > 0, most likely it should be 7, but it is: " + parser.code.mainpc);
  }

  @Test
  public void noMain() {
    initCode("program Test {" + LF + //
            "  void main_() { }" + LF + //
            "}");
    expectError(3, 2, MAIN_NOT_FOUND);
    parseVerifyVisualize();
  }

  @Test
  public void noValueAssignopMethod() {
    initCode("program Test {" + LF + //
            "  int method() { return 0; }" + LF + //
            "  void main() int i; {" + LF + //
            "    method += i;" + LF + //
            "  }" + LF + //
            "}");
    expectError(4, 12, CANNOT_STORE_TO_READONLY, Operand.Kind.Meth.name());
    parseVerifyVisualize();
  }

  @Test
  public void testMulops() {
    initCode("program Mulops" + LF + //
            "{" + LF + //
            "  void main ()" + LF + //
            "    int a; int b;" + LF + //
            "  {" + LF + //
            "    a = 42;" + LF + //
            "    b = 3;" + LF + //
            "    a = a / b;" + LF + //
            "    a = a % ( b * b );" + LF + //
            "    print(a);" + LF + //
            "  }" + LF + //
            "}");
    addExpectedRun("5");
    parseVerifyVisualize();
  }

  @Test
  public void testLocalVarsIncDec() {
    initCode("program LocalVars" + LF + //
            "{" + LF + //
            "  void main()" + LF + //
            "    int a;" + LF + //
            "    int b;" + LF + //
            "  {" + LF + //
            "    a = 2;" + LF + //
            "    b = 5;" + LF + //
            "    a++;" + LF + //
            "    b--;" + LF + //
            "    print(a+b);" + LF + //
            "  }" + LF + //
            "}");
    addExpectedRun("7");
    parseVerifyVisualize();
  }

  @Test
  public void testConstDecl() {
    initCode("program ConstDecl" + LF + //
            " final int a = 100;" + LF + //
            " final char b = 'A';" + LF + //
            "{" + LF + //
            "  void main()" + LF + //
            "  {" + LF + //
            "    print(a);" + LF + //
            "    print(b);" + LF + //
            "  }" + LF + //
            "}");
    addExpectedRun("100A");
    parseVerifyVisualize();
  }

  @Test
  public void testMethodAsOperand() {
    initCode("program ConstDecl" + LF + //
            "{" + LF + //
            "  int foo() {}" + LF + //
            "  void main()" + LF + //
            "  {" + LF + //
            "    foo++;" + LF + //
            "  }" + LF + //
            "}");
    expectError(6, 8, CANNOT_STORE_TO_READONLY, Operand.Kind.Meth.name());
    parseVerifyVisualize();
  }

  @Test
  public void testTypeAsOperand() {
    initCode("program ConstDecl" + LF + //
            "class Foo {}" + LF + //
            "{" + LF + //
            "  void main()" + LF + //
            "  {" + LF + //
            "    Foo++;" + LF + //
            "  }" + LF + //
            "}");
    expectError(6, 8, ILLEGAL_OPERAND_KIND, Obj.Kind.Type);
    parseVerifyVisualize();
  }

  @Test
  public void writeConstant() {
    initCode("program Test" + LF + //
            "  final int max = 42;" + LF + //
            "{" + LF + //
            "  void main() {" + LF + //
            "    max = 68;" + LF + //
            "  }" + LF + //
            "}");
    expectError(5, 9, CANNOT_STORE_TO_READONLY, Operand.Kind.Con.name());
    parseVerifyVisualize();
  }

  @Test
  public void negativeArrayIndex() {
    initCode("""
            program Test {
              void main() int[] a; int i; {
                a = new int[1];
                i = a[-1];
                print(i);
              }
            }""");
    addFailingRun("index out of bounds");
    parseVerifyVisualize();
  }

  // index from end tests

  @Test
  public void constantArrayAccessFromEnd() {
    initCode("""
            program Test {
              void main() int[] a; int i; {
                a = new int[3];
                a[~1] = 3;
                a[~2] = 2;
                a[~3] = 1;
                i = a[~1];
                print(i);
              }
            }""");
    addExpectedRun("3");
    parseVerifyVisualize();
  }

  @Test
  public void computedArrayAccessFromEnd() {
    initCode("""
            program Test {
              void main() int[] a; int i; {
                a = new int[3];
                a[~(2 * 6 - 11)] = 3;
                a[~(9 - 11 + 4)] = 2;
                a[~(-(-3))] = 1;
                i = a[~(a[0] + 2 * 6 - 11)];
                print(i);
              }
            }""");
    addExpectedRun("2");
    parseVerifyVisualize();
  }

  @Test
  public void dynamicArrayAccessFromEnd() {
    initCode("""
            program Test {
              void main() int[] a; int i; {
                read(i);
                a = new int[i];
                a[~i] = 1;
                a[~(i - 1)] = 2;
                a[~(i - 2)] = 3;
                i = a[~i];
                print(i);
              }
            }""");
    addExpectedRun("3", "1");
    parseVerifyVisualize();
  }

  @Test
  public void globalArrayAccessFromEnd() {
    initCode("""
            program Test
              final int len = 3;
            {
              void main() int[] a; int i; {
                a = new int[len];
                a[~len] = 1;
                a[~(len - 1)] = 2;
                a[~(len - 2)] = 3;
                i = a[~len];
                print(i);
              }
            }""");
    addExpectedRun("1");
    parseVerifyVisualize();
  }

  @Test
  public void arrayAccessFromZeroEnd() {
    initCode("""
            program Test
            final int len = 3;
            {
              void main() int[] a; int i; {
                a = new int[len];
                i = a[~0];
                print(i);
              }
            }""");
    addFailingRun("index out of bounds");
    parseVerifyVisualize();
  }

  @Test
  public void negativeOutOfBoundsArrayAccessFromEnd() {
    initCode("""
            program Test
            final int len = 3;
            {
              void main() int[] a; int i; {
                a = new int[len];
                i = a[~(-20)];
                print(i);
              }
            }""");
    addFailingRun("index out of bounds");
    parseVerifyVisualize();
  }

  @Test
  public void positiveOutOfBoundsArrayAccessFromEnd() {
    initCode("""
            program Test
            final int len = 3;
            {
              void main() int[] a; int i; {
                a = new int[len];
                i = a[~1000];
                print(i);
              }
            }""");
    addFailingRun("index out of bounds");
    parseVerifyVisualize();
  }

  @Test
  public void arrayFromEndIncrement() {
    initCode("""
            program Test
            final int len = 1;
            {
              void main() int[] a; {
                a = new int[len];
                a[~1]++;
                print(a[0]);
              }
            }""");
    addExpectedRun("1");
    parseVerifyVisualize();
  }

  @Test
  public void arrayFromEndCompoundAssignment() {
    initCode("""
            program Test
            final int len = 2;
            {
              void main() int[] a; {
                a = new int[len];
                a[0] = 1;
                a[1] = 1;
                a[~1]++;
                a[~2]--;
                print(a[0]);
                print(a[1]);
              }
            }""");
    addExpectedRun("02");
    parseVerifyVisualize();
  }
}
