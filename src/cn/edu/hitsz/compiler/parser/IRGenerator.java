package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.parser.table.Symbol;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    private SymbolTable symbolTable;

    // 语义分析栈
    private final Stack<Symbol> symbolStack = new Stack<>();
    private final Stack<IRValue> irValueStack = new Stack<>();

    // 中间代码序列(需要返回)
    private final List<Instruction> irList = new ArrayList<>();


    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO

        symbolStack.push(new Symbol(currentToken));
        irValueStack.push(null);
        // throw new NotImplementedException();
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO

        // 根据产生式规则进行语义分析
        switch (production.index()) {
            case 6 -> {
                // S -> id = E
                symbolStack.pop();
                symbolStack.pop();
                Symbol id = symbolStack.pop();
                IRValue EValue = irValueStack.pop();
                irValueStack.pop();
                irValueStack.pop();

                // id为具体变量
                Token token = id.getToken();
                if(!symbolTable.has(token.getText())){
                    throw new RuntimeException("SymbolTable no such id");
                }
                IRVariable idValue = IRVariable.named(token.getText());

                // MOV id E
                irList.add(Instruction.createMov(idValue, EValue));

                // 填入S
                symbolStack.push(new Symbol(production.head()));
                irValueStack.push(null);
            }
            case 7 -> {
                // S -> return E
                symbolStack.pop();
                symbolStack.pop();
                IRValue EValue = irValueStack.pop();
                irValueStack.pop();

                // RET E
                irList.add(Instruction.createRet(EValue));

                // 填入S
                symbolStack.push(new Symbol(production.head()));
                irValueStack.push(null);
            }
            case 8 -> {
                // E -> E + A
                symbolStack.pop();
                symbolStack.pop();
                symbolStack.pop();
                IRValue AValue = irValueStack.pop();
                irValueStack.pop();
                IRValue EValue = irValueStack.pop();

                // ADD E E A
                IRVariable EValueTemp = IRVariable.temp();
                irList.add(Instruction.createAdd(EValueTemp, EValue, AValue));

                // 填入E
                symbolStack.push(new Symbol(production.head()));
                irValueStack.push(EValueTemp);
            }
            case 9 -> {
                // E -> E - A
                symbolStack.pop();
                symbolStack.pop();
                symbolStack.pop();
                IRValue AValue = irValueStack.pop();
                irValueStack.pop();
                IRValue EValue = irValueStack.pop();

                // SUB E E A
                IRVariable EValueTemp = IRVariable.temp();
                irList.add(Instruction.createSub(EValueTemp, EValue, AValue));

                // 填入E
                symbolStack.push(new Symbol(production.head()));
                irValueStack.push(EValueTemp);
            }
            case 10, 12 -> {
                // E -> A or A -> B
                symbolStack.pop();
                IRValue value = irValueStack.pop();

                // 填入A or B
                symbolStack.push(new Symbol(production.head()));
                irValueStack.push(value);
            }
            case 11 -> {
                // A -> A * B
                symbolStack.pop();
                symbolStack.pop();
                symbolStack.pop();
                IRValue BValue = irValueStack.pop();
                irValueStack.pop();
                IRValue AValue = irValueStack.pop();

                // MUL A A B
                IRVariable AValueTemp = IRVariable.temp();
                irList.add(Instruction.createMul(AValueTemp, AValue, BValue));

                // 填入temp
                symbolStack.push(new Symbol(production.head()));
                irValueStack.push(AValueTemp);
            }
            case 13 -> {
                // B -> ( E )
                symbolStack.pop();
                symbolStack.pop();
                symbolStack.pop();
                irValueStack.pop();
                IRValue EValue = irValueStack.pop();
                irValueStack.pop();

                // 填入B
                symbolStack.push(new Symbol(production.head()));
                IRValue BValue = EValue;
                irValueStack.push(BValue);
            }
            case 14 -> {
                // B -> id
                Symbol id = symbolStack.pop();
                IRValue idValue = irValueStack.pop();

                Token token = id.getToken();
                if(!symbolTable.has(token.getText())){
                    throw new RuntimeException("SymbolTable no such id");
                }

                // 填入B
                symbolStack.push(new Symbol(production.head()));
                IRValue BValue = IRVariable.named(token.getText());
                irValueStack.push(BValue);
            }
            case 15 -> {
                // B -> IntConst
                Symbol intConst = symbolStack.pop();
                irValueStack.pop();

                // 填入B
                symbolStack.push(new Symbol(production.head()));
                Token token = intConst.getToken();
                IRImmediate BValue = IRImmediate.of(Integer.parseInt(token.getText()));
                irValueStack.push(BValue);
            }
            default -> {
                // 弹出产生式右部
                int cnt = production.body().size();
                while (cnt > 0) {
                    symbolStack.pop();
                    irValueStack.pop();
                    --cnt;
                }

                // 占位
                symbolStack.push(new Symbol(production.head()));
                irValueStack.push(null);
            }
        }
        // throw new NotImplementedException();
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO

        // do nothing
        System.out.println("中间代码生成完成");
        // throw new NotImplementedException();
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO

        symbolTable = table;
        // throw new NotImplementedException();
    }

    public List<Instruction> getIR() {
        // TODO
        return irList;
        // throw new NotImplementedException();
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

