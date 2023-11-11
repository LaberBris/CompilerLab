package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.*;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {
    private final SymbolTable symbolTable;

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        sourceCode = FileUtils.readFile(path);

        // System.out.println(sourceCode);
        // throw new NotImplementedException();
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        int sLength = sourceCode.length();

        for (int i = 0; i < sLength; i++) {
            int currentState = 0;
            int endPointer = i;

            while (!tStates.contains(currentState) && endPointer < sLength) {
                char ch = sourceCode.charAt(endPointer);
                // System.out.println(ch);

                boolean isBlank = blankList.contains(ch);
                boolean isLetter = Character.isLetter(ch);
                boolean isDigit = Character.isDigit(ch);
                boolean isSemicolon = ch == ';';

                // 确定下一个状态
                int nextState = switch (currentState) {
                    case 0-> switch (ch) {
                        case '*' -> 18;
                        case '=' -> 21;
                        case '+' -> 29;
                        case '-' -> 30;
                        case ',' -> 32;
                        case '(' -> 26;
                        case ')' -> 27;
                        case '/' -> 31;
                        case ';' -> 28;
                        default -> {
                            if (isBlank) {
                                yield 0;
                            } else if (isLetter) {
                               yield 14;
                            } else if (isDigit) {
                                yield 16;
                            } else {
                                throw new RuntimeException("Illegal Identifier");
                            }
                        }
                    };
                    case 14 -> {
                        if (isLetter || isDigit) {
                            yield 14;
                        } else {
                            yield 15;
                        }
                    }
                    case 16 -> {
                        if (isDigit) {
                            yield 16;
                        } else {
                            yield 17;
                        }
                    }
                    case 18 -> {
                        if (ch == '*') {
                            yield 19;
                        } else {
                            yield 20;
                        }
                    }
                    case 21 -> {
                        if (ch == '=') {
                            yield 22;
                        } else {
                            yield 23;
                        }
                    }
                    default -> {
                        if (tStates.contains(currentState)) {
                            yield 0;
                        } else {
                            throw new RuntimeException("Illegal Identifier");
                        }
                    }
                };

                if (tStates.contains(nextState)) {
                    tokens.add(switch (nextState){
                        case 15 -> {
                            if ("int".equals(sourceCode.substring(i, endPointer))) {
                                yield Token.simple("int");
                            } else if ("return".equals(sourceCode.substring(i, endPointer))) {
                                yield Token.simple("return");
                            } else {
                                if (!symbolTable.has(sourceCode.substring(i, endPointer))) {
                                    symbolTable.add(sourceCode.substring(i, endPointer));
                                }
                                yield Token.normal("id", sourceCode.substring(i, endPointer));
                            }
                        }
                        case 17 -> Token.normal("IntConst", sourceCode.substring(i, endPointer));
                        case 20 -> Token.simple("*");
                        case 23 -> Token.simple("=");
                        case 26 -> Token.simple("(");
                        case 27 -> Token.simple(")");
                        case 28 -> Token.simple("Semicolon");
                        case 29 -> Token.simple("+");
                        case 30 -> Token.simple("-");
                        case 31 -> Token.simple("/");
                        case 32 -> Token.simple(",");
                        default -> throw new IllegalStateException("Unexpected value: " + nextState);
                    });
                }

                // 处理无意义字符
                switch (currentState) {
                    case 0 -> {
                        if (isBlank) {
                            i += 1;
                        }
                    }
                    case 14, 16 -> {
                        if (isSemicolon) {
                            tokens.add(Token.simple("Semicolon"));
                        }
                    }
                    default -> {}
                }

                // System.out.println("现在的下标是"+ endPointer +", 现在读到的字符是" + ch + ", 当前状态是" + currentState + ", 下一个状态是" + nextState);
                endPointer += 1;
                currentState = nextState;
            }

            i = endPointer - 1;
            // System.out.println("现在的下标是"+ i);
        }

        tokens.add(Token.eof());
        // throw new NotImplementedException();
    }


    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可

        return tokens;
        // throw new NotImplementedException();
    }

    /**
     * 将词法分析的结果输出到文件
     *
     * @param path 输出文件的路径
     */
    public void dumpTokens(String path) {
        FileUtils.writeLines(
            path,
            StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }

    /**
     * 代表源代码的字符串
     */
    private static String sourceCode;

    /**
     * 词法分析的结果
     */
    private static List<Token> tokens = new ArrayList<>();

    /**
     * 存放无意义字符的列表(集合)
     */
    private static final List<Character> blankList = Arrays.asList(' ', '\t', '\n');

    /**
     * 存放终态的列表(集合)
     */
    private static final Set<Integer> tStates = new HashSet<>(Arrays.asList(15, 17, 20, 23, 26, 27, 28, 29, 30, 31, 32));

}