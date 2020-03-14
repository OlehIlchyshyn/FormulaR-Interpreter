package com.t1ne.formular;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.Tree;

import java.util.ArrayList;

public class TreeView implements Expr.Visitor<TreeNode>, Stmt.Visitor<TreeNode> {

    String print(Stmt stmt) {
        return stmt.accept(this).toString();
    }

    @Override
    public TreeNode visitAssignExpr(Expr.Assign expr) {
        ArrayList<TreeNode> childs = new ArrayList<>();
        childs.add(new TreeNode(expr.name.lexeme, new ArrayList<>()));
        childs.add(expr.value.accept(this));
        return new TreeNode("=", childs);
    }

    @Override
    public TreeNode visitBinaryExpr(Expr.Binary expr) {
        ArrayList<TreeNode> childs = new ArrayList<>();
        childs.add(expr.right.accept(this));
        childs.add(expr.left.accept(this));
        return new TreeNode(expr.operator.lexeme, childs);
    }

    @Override
    public TreeNode visitCallExpr(Expr.Call expr) {
        ArrayList<TreeNode> childs = new ArrayList<>();
        childs.add(expr.callee.accept(this));
        ArrayList<TreeNode> args = new ArrayList<>();
        for (Expr arg : expr.arguments) {
            args.add(arg.accept(this));
        }
        childs.add(new TreeNode("args", args));
        return new TreeNode("call", childs);
    }

    @Override
    public TreeNode visitGroupingExpr(Expr.Grouping expr) {
        ArrayList<TreeNode> childs = new ArrayList<>();
        childs.add(expr.expression.accept(this));
        return new TreeNode("group", childs);
    }

    @Override
    public TreeNode visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return new TreeNode("nil", new ArrayList<>());
        return new TreeNode(expr.value.toString(), new ArrayList<>());
    }

    @Override
    public TreeNode visitUnaryExpr(Expr.Unary expr) {
        ArrayList<TreeNode> childs = new ArrayList<>();
        childs.add(expr.right.accept(this));
        return new TreeNode(expr.operator.lexeme, childs);
    }

    @Override
    public TreeNode visitVariableExpr(Expr.Variable expr) {
        return new TreeNode(expr.name.lexeme, new ArrayList<>());
    }

    @Override
    public TreeNode visitBlockStmt(Stmt.Block stmt) {
        ArrayList<TreeNode> childs = new ArrayList<>();
        for (Stmt statement : stmt.statements) {
            childs.add(statement.accept(this));
        }
        return new TreeNode("block", childs);
    }

    @Override
    public TreeNode visitExpressionStmt(Stmt.Expression stmt) {
        ArrayList<TreeNode> childs = new ArrayList<>();
        childs.add(stmt.expression.accept(this));
        return new TreeNode(";", childs);
    }

    @Override
    public TreeNode visitFunctionStmt(Stmt.Function stmt) {
        ArrayList<TreeNode> params = new ArrayList<>();
        for (Token param : stmt.params) {
            params.add(new TreeNode(param.lexeme, new ArrayList<>()));
        }
        ArrayList<TreeNode> body = new ArrayList<>();
        for (Stmt bodyStmt : stmt.body) {
            body.add(bodyStmt.accept(this));
        }
        ArrayList<TreeNode> childs = new ArrayList<>();
        childs.add(new TreeNode("params", params));
        childs.add(new TreeNode("body", body));
        return new TreeNode("func "+stmt.name.lexeme, childs);
    }

    @Override
    public TreeNode visitIfStmt(Stmt.If stmt) {
        ArrayList<TreeNode> childs = new ArrayList<>();
        childs.add(stmt.condition.accept(this));
        childs.add(stmt.thenBranch.accept(this));
        if (stmt.elseBranch != null) {
            childs.add(stmt.elseBranch.accept(this));
            return new TreeNode("if-else", childs);
        }
        return new TreeNode("if", childs);
    }

    @Override
    public TreeNode visitPrintStmt(Stmt.Print stmt) {
        ArrayList<TreeNode> childs = new ArrayList<>();
        childs.add(stmt.expression.accept(this));
        return new TreeNode("print", childs);
    }

    @Override
    public TreeNode visitReturnStmt(Stmt.Return stmt) {
        if (stmt.value == null) return new TreeNode("return", new ArrayList<>());
        ArrayList<TreeNode> childs = new ArrayList<>();
        childs.add(stmt.value.accept(this));
        return new TreeNode("return", childs);
    }

    @Override
    public TreeNode visitVarStmt(Stmt.Var stmt) {
        ArrayList<TreeNode> childs = new ArrayList<>();
        if (stmt.initializer == null) {
            childs.add(new TreeNode(stmt.name.lexeme, new ArrayList<>()));
        }
        else {
            ArrayList<TreeNode> childs2 = new ArrayList<>();
            childs2.add(new TreeNode(stmt.name.lexeme, new ArrayList<>()));
            childs2.add(stmt.initializer.accept(this));
            childs.add(new TreeNode("=", childs2));
        }
        return new TreeNode("var", childs);
    }

    @Override
    public TreeNode visitWhileStmt(Stmt.While stmt) {
        ArrayList<TreeNode> childs = new ArrayList<>();
        childs.add(stmt.condition.accept(this));
        childs.add(stmt.body.accept(this));
        return new TreeNode("while", childs);
    }
}
