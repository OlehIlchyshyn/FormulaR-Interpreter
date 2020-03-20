package com.t1ne.formular;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.pow;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment = globals;

    Interpreter() {
        //core library funcs
        // _* must be added to name, where * is arguments number due to function overloading strategy
        globals.define("sin_1", new FCallable() {
            @Override
            public int argsNum() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return Math.sin((double)arguments.get(0));
            }

            @Override
            public String toString() { return "<вбудована функція sin>"; }
        });

        globals.define("cos_1", new FCallable() {
            @Override
            public int argsNum() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return Math.cos((double)arguments.get(0));
            }

            @Override
            public String toString() { return "<вбудована функція cos>"; }
        });

        globals.define("pow_2", new FCallable() {
            @Override
            public int argsNum() { return 2; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return pow((double)arguments.get(0), (double)arguments.get(1));
            }

            @Override
            public String toString() { return "<вбудована функція pow>"; }
        });

        globals.define("sqrt_1", new FCallable() {
            @Override
            public int argsNum() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return Math.sqrt((double)arguments.get(0));
            }

            @Override
            public String toString() { return "<вбудована функція sqrt>"; }
        });

        globals.define("kvadrat_1", new FCallable() {
            @Override
            public int argsNum() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return pow((double)arguments.get(0),2);
            }

            @Override
            public String toString() { return "<вбудована функція kvadrat>"; }
        });

        globals.define("diffLn_1", new FCallable() {
            @Override
            public int argsNum() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return 1/Math.log((double)arguments.get(0));
            }

            @Override
            public String toString() { return "<вбудована функція differentiatePolynomial>"; }
        });

        globals.define("diffLog_2", new FCallable() {
            @Override
            public int argsNum() { return 2; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return 1/Math.log((double)arguments.get(0));
            }

            @Override
            public String toString() { return "<вбудована функція differentiatePolynomial>"; }
        });

        globals.define("diffPolynom_2", new FCallable() {
            @Override
            public int argsNum() { return 2; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                double answer = 0;
                String nextChar = "+";
                String formula = (String)arguments.get(0);
                String[] parts = formula.split(" ");
                for (String part : parts) {
                    if (!part.equals("+") && !part.equals("-")) {
                        StringBuilder coeffStr = new StringBuilder();
                        int i;
                        for (i = 0; part.charAt(i) != 'x'; i++)
                            coeffStr.append(part.charAt(i));
                        int coeff = Integer.parseInt(coeffStr.toString());
                        StringBuilder powStr = new StringBuilder();
                        for (i = i + 2; i != part.length(); i++)
                            powStr.append(part.charAt(i));
                        int pow = Integer.parseInt(powStr.toString());
                        if (nextChar.equals("+"))
                            answer += coeff * pow * pow((double) arguments.get(1), pow - 1);
                        else
                            answer -= coeff * pow * pow((double) arguments.get(1), pow - 1);
                    }
                    if (part.equals("+")) {
                        nextChar = "+";
                    } else if (part.equals("-")) {
                        nextChar = "-";
                    }
                }
                return answer;
            }

            @Override
            public String toString() { return "<вбудована функція differentiatePolynomial>"; }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            FormulaR.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case EXCL:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Операнд повинен бути числом.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Операнди повинні бути числами.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        // nil is only equal to nil.
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        // Hack. Work around Java adding ".0" to integer-valued doubles.
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        FFunction function = new FFunction(stmt);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case EXCL_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }

                throw new RuntimeError(expr.operator, "Операндами можуть бути лише два числа або два символьних рядки.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof FCallable)) {
            throw new RuntimeError(expr.paren,"Викликати можливо лише функції.");
        }

        FCallable function = (FCallable)callee;

        if (arguments.size() != function.argsNum()) {
            throw new RuntimeError(expr.paren, "Очікується " +
                    function.argsNum() + " аргументів але отримано " +
                    arguments.size() + ".");
        }
        return function.call(this, arguments);
    }

    public List<Stmt> optimize(List<Stmt> statements) throws IOException {
        List<Stmt> optList = new ArrayList<>();
        TreeView astPrinter = new TreeView();
        PrintWriter astFile = new PrintWriter("optimization.txt", StandardCharsets.UTF_8);
        for (Stmt stmt: statements) {
            if (stmt instanceof Stmt.While) {
                Stmt.While temp = (Stmt.While)stmt;
                if (temp.condition instanceof Expr.Literal) {
                    if (!(boolean) temp.condition.accept(this)) {
                        astFile.println("\nBefore: ");
                        astFile.println(astPrinter.print(stmt));
                        astFile.println("\nAfter: ");
                        astFile.println("Statement was removed (loop body will never execute)\n");
                        astFile.println("/**************************************************************************/");
                        continue;
                    }
                }
                else if (temp.body instanceof Stmt.Block) {
                    astFile.println("\nBefore: ");
                    astFile.println(astPrinter.print(stmt));
                    astFile.println("\nAfter: ");
                    Stmt.Block tempBlock = (Stmt.Block) temp.body;
                    if (tempBlock.statements.size() == 0) {
                        astFile.println("Statement was removed (loop body is empty)\n");
                        astFile.println("/**************************************************************************/");
                    } else if (tempBlock.statements.size() == 1) {
                        Stmt.While newLoop = new Stmt.While(temp.condition, tempBlock.statements.get(0));
                        optList.add(newLoop);
                        astFile.println(astPrinter.print(newLoop));
                        astFile.println("/**************************************************************************/");
                    } else {
                        optList.add(stmt);
                    }
                }
            }
            else if (stmt instanceof Stmt.If) {
                Stmt.If temp = (Stmt.If)stmt;
                astFile.println("\nBefore: ");
                astFile.println(astPrinter.print(stmt));
                astFile.println("\nAfter: ");
                if ((boolean)temp.condition.accept(this)) {
                    optList.add(temp.thenBranch);
                    astFile.println(astPrinter.print(temp.thenBranch));
                }
                else if (temp.elseBranch != null) {
                    optList.add(temp.elseBranch);
                    astFile.println(astPrinter.print(temp.elseBranch));
                }
                else {
                    astFile.println("Statement was removed (condition is false and no else block provided)\n");
                }
                astFile.println("/**************************************************************************/");
            }
            else {
                optList.add(stmt);
            }
        }
        astFile.close();
        return optList;
    }
}
