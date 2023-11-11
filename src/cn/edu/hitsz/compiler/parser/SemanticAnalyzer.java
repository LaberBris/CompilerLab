package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.symtab.SymbolTableEntry;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {

    private SymbolTable symbolTable;
    private final Stack<Token> tokenStack = new Stack<>();
    private final Stack<SourceCodeType> typeStack = new Stack<>();

    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作

        // nothing to do
        System.out.println("语义分析完成");
        // throw new NotImplementedException();
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作

        switch (production.index()) {
            case 4 -> {
                // S -> D id
                Token token = tokenStack.get(tokenStack.size() - 1);
                if (symbolTable.has(token.getText())) {
                    SymbolTableEntry symbolTableEntry = symbolTable.get(token.getText());
                    symbolTableEntry.setType(typeStack.pop());
                    tokenStack.pop();
                } else {
                    // 发出异常
                    throw new RuntimeException("Undefined variable: " + token.getText());
                }
            }
            case 5 -> {
                // D -> int
                typeStack.push(SourceCodeType.Int);
            }
            default -> {
                //  占位
                typeStack.push(null);
            }
        }
        // throw new NotImplementedException();
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作

        tokenStack.push(currentToken);
        // throw new NotImplementedException();
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        symbolTable = table;

        // throw new NotImplementedException();
    }
}

