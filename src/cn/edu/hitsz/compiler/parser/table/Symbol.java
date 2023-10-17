package cn.edu.hitsz.compiler.parser.table;
import cn.edu.hitsz.compiler.lexer.Token;

public class Symbol {
    private final Token token;
    private final NonTerminal nonTerminal;

    private Symbol(Token token, NonTerminal nonTerminal) {
        this.token = token;
        this.nonTerminal = nonTerminal;
    }

    /**
     * 为了方便, 用两个方法分别构造终结符和非终结符
     */
    public Symbol(Token token) {
        this(token, null);
    }

    public Symbol(NonTerminal nonTerminal) {
        this(null, nonTerminal);
    }

    public boolean isToken() {
        return this.token != null;
    }

    public boolean isNonTerminal() {
        return this.nonTerminal != null;
    }

    public Token getToken() {
        return this.token;
    }

    public NonTerminal getNonTerminal() {
        return this.nonTerminal;
    }
}