package com.t1ne.formular;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.t1ne.formular.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() throws IOException {
        List<Stmt> statements = new ArrayList<>();
        Map <String, Stmt> varStmts = new HashMap<>();
        TreeView astPrinter = new TreeView();
        PrintWriter astFile = new PrintWriter("ast.txt", StandardCharsets.UTF_8);
        while (!isAtEnd()) {
            Stmt stmt = declaration();
            String name;
            if (stmt instanceof Stmt.Var) {
                Stmt.Var var = (Stmt.Var) stmt;
                name = var.name.lexeme;
                varStmts.put(name, stmt);
            } else if (stmt instanceof Stmt.Expression) {
                Stmt.Expression expr = (Stmt.Expression) stmt;
                if (expr.expression instanceof Expr.Assign) {
                    Expr.Assign assign = (Expr.Assign) expr.expression;
                    name = assign.name.lexeme;
                    varStmts.put(name, stmt);
                }
            }
            astFile.println(astPrinter.print(stmt));
            statements.add(stmt);
            for (String key : varStmts.keySet()) {
                statements.add(varStmts.get(key));
            }
        }
        astFile.close();
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Stmt declaration() {
        try {
            if (match(FUNC)) return function();
            if (match(VAR)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Очікується '(' після 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Очікується ')' наприкінці умови умовного оператора.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Очікується ';' наприкінці виразу.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Очікується ';' після виразу повернення.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Очікується ім'я змінної.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Очікується ';' після оголошення змінної.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Очікується '(' після 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Очікується ')' після умови циклу.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Відсутній символ ';' наприкінці виразу.");
        return new Stmt.Expression(expr);
    }

    private Stmt.Function function() {
        Token name = consume(IDENTIFIER, "Очікується ім'я функції.");
        consume(LEFT_PAREN, "Очікується '(' після імені фукнції.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                parameters.add(consume(IDENTIFIER, "Очікується ім'я параметру."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Очікується ')' в кінці списку параметрів.");
        consume(LEFT_BRACE, "Очікується '{' перед тілом функції.");
        List<Stmt> body = block();
        name.lexeme += "_" + parameters.size();
        return new Stmt.Function(name, parameters, body);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Відстутня '}' в кінці блоку.");
        return statements;
    }

    private Expr assignment() {
        Expr expr = equality();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Неможливо виконати присвоєння.");
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(EXCL_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        FormulaR.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case FUNC:
                case VAR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }

    private Expr comparison() {
        Expr expr = addition();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = addition();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr addition() {
        Expr expr = multiplication();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = multiplication();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr multiplication() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(EXCL, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (match(LEFT_PAREN)) {
                expr = finishCall(expr);
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Очікується ')' після виразу.");
        Expr.Variable caller = (Expr.Variable)callee;
        caller.name.lexeme += "_" + arguments.size();
        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Очікується ')' після виразу.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Очікується вираз.");
    }
}