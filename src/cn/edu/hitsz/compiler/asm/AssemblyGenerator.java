package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {
    // 寄存器: t0-t6
    private static final List<String> registers = List.of("t0", "t1", "t2", "t3", "t4", "t5", "t6");

    private List<Instruction> irList = new ArrayList<>();

    private RegVarMap regvarMap = new RegVarMap();

    // 记录变量最后一次使用的行号
    private final Map<IRValue, Integer> LastUse = new HashMap<>();


    // 汇编代码, 初始定义首行为 .text
    private List<String> assemblyCode = new ArrayList<>(List.of(".text"));

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息

        for (Instruction instruction : originInstructions) {
            switch (instruction.getKind()) {
                case MOV -> {
                    irList.add(instruction);
                }
                case ADD, SUB, MUL -> {
                    // 获取LHS和RHS
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();

                    // 如果LHS和RHS都是立即数, 则直接计算结果, 新增一条MOV指令
                    if (lhs.isImmediate() && rhs.isImmediate()) {
                        // 计算结果
                        int result = 0;
                        IRImmediate lhsImmediate = (IRImmediate) lhs;
                        IRImmediate rhsImmediate = (IRImmediate) rhs;
                        switch (instruction.getKind()) {
                            case ADD -> result = lhsImmediate.getValue() + rhsImmediate.getValue();
                            case SUB -> result = lhsImmediate.getValue() - rhsImmediate.getValue();
                            case MUL -> result = lhsImmediate.getValue() * rhsImmediate.getValue();
                            default ->
                                    throw new IllegalStateException("Unexpected instruction kind: " + instruction.getKind());
                        }
                        irList.add(Instruction.createMov(instruction.getResult(), IRImmediate.of(result)));
                    } else if (!lhs.isImmediate() && rhs.isImmediate()) {
                        // 如果LHS不是立即数, RHS是立即数

                        if (instruction.getKind() == InstructionKind.MUL) {
                            // 乘法, 添加MOV指令
                            IRVariable temp = IRVariable.temp();
                            irList.add(Instruction.createMov(temp, rhs));
                            irList.add(Instruction.createMul(instruction.getResult(), lhs, temp));
                        } else if (instruction.getKind() == InstructionKind.ADD || instruction.getKind() == InstructionKind.SUB) {
                            // 其他, 直接加入irList
                            irList.add(instruction);
                        } else {
                            throw new IllegalStateException("Unexpected instruction kind: " + instruction.getKind());
                        }

                    } else if (lhs.isImmediate() && !rhs.isImmediate()) {
                        // 如果LHS是立即数, RHS不是立即数
                        switch (instruction.getKind()) {
                            // 如果是加法, 则直接将RHS移动到LHS
                            case ADD -> {
                                irList.add(Instruction.createAdd(instruction.getResult(), rhs, lhs));
                            }
                            case SUB -> {
                                // 先用一个变量temp保存LHS
                                IRVariable temp = IRVariable.temp();
                                irList.add(Instruction.createMov(temp, lhs));
                                irList.add(Instruction.createSub(instruction.getResult(), temp, rhs));
                            }
                            case MUL -> {
                                // 乘法, 添加MOV指令
                                IRVariable temp = IRVariable.temp();
                                irList.add(Instruction.createMov(temp, lhs));
                                irList.add(Instruction.createMul(instruction.getResult(), temp, rhs));
                            }
                            default ->
                                    throw new IllegalStateException("Unexpected instruction kind: " + instruction.getKind());
                        }
                    } else {
                        // 如果LHS和RHS都不是立即数, 直接加入irList
                        irList.add(instruction);
                    }

                }
                case RET -> {
                    irList.add(instruction);
                    // 直接返回
                    return;
                }
                default -> throw new IllegalStateException("Unexpected instruction kind: " + instruction.getKind());
            }
        }


        // throw new NotImplementedException();
    }


    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成

        int lineNum = 0;

        // 构造最后一次使用的行号表
        getLastUse();

        // 打印LastUse表
        // for (Map.Entry<IRValue, Integer> entry : LastUse.entrySet()) {
        //     System.out.println(entry.getKey() + " " + entry.getValue());
        // }

        // 遍历IRList, 生成汇编代码
        for (Instruction instruction : irList) {
            lineNum++;
            switch (instruction.getKind()) {
                case MOV -> {
                    // 获取变量名
                    IRVariable result = instruction.getResult();
                    IRValue from = instruction.getFrom();

                    // 判断from是否是立即数
                    if (from.isImmediate()) {
                        // 分配寄存器
                        allocateReg(result, lineNum);

                        // 获取寄存器名
                        String resultReg = regvarMap.getReg(result);

                        // 生成汇编代码
                        assemblyCode.add("\tli " + resultReg + ", " + from + "\t\t# " + instruction.toString());
                    } else {
                        // 分配寄存器
                        allocateReg(result, lineNum);
                        allocateReg(from, lineNum);

                        // 获取寄存器名
                        String resultReg = regvarMap.getReg(result);
                        String fromReg = regvarMap.getReg(from);

                        // 生成汇编代码
                        assemblyCode.add("\tmv " + resultReg + ", " + fromReg + "\t\t# " + instruction.toString());
                    }
                }
                case ADD, SUB, MUL -> {
                    // 获取变量名
                    IRVariable result = instruction.getResult();
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();

                    // 判断rhs是否是立即数
                    if (rhs.isImmediate()) {
                        // 分配寄存器
                        allocateReg(result, lineNum);
                        allocateReg(lhs, lineNum);

                        // 获取寄存器名
                        String resultReg = regvarMap.getReg(result);
                        String lhsReg = regvarMap.getReg(lhs);

                        // 生成汇编代码, 立即数运算要用到addi, subi, muli
                        assemblyCode.add("\t" + instruction.getKind().toString().toLowerCase() + "i " + resultReg + ", " + lhsReg + ", " + rhs + "\t\t# " + instruction.toString());
                    } else {
                        // 分配寄存器
                        allocateReg(result, lineNum);
                        allocateReg(lhs, lineNum);
                        allocateReg(rhs, lineNum);

                        // 获取寄存器名
                        String resultReg = regvarMap.getReg(result);
                        String lhsReg = regvarMap.getReg(lhs);
                        String rhsReg = regvarMap.getReg(rhs);

                        // 生成汇编代码
                        assemblyCode.add("\t" + instruction.getKind().toString().toLowerCase() + " " + resultReg + ", " + lhsReg + ", " + rhsReg + "\t\t# " + instruction.toString());
                    }
                }
                case RET -> {
                    // 获取变量名
                    IRValue returnValue = instruction.getReturnValue();

                    // 分配寄存器
                    allocateReg(returnValue, lineNum);

                    // 获取寄存器名
                    String returnValueReg = regvarMap.getReg(returnValue);

                    // 生成汇编代码, 结果保存在a0中
                    assemblyCode.add("\tmv a0, " + returnValueReg + "\t\t# " + instruction.toString());
                    return;
                }
                default -> throw new IllegalStateException("Unexpected instruction kind: " + instruction.getKind());
            }
        }

        // throw new NotImplementedException();
    }


    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {
        // TODO: 输出汇编代码到文件

        // 每行一条汇编代码
        FileUtils.writeLines(path, assemblyCode);
        // throw new NotImplementedException();
    }

    /**
     * 获取变量最后一次使用的行号
     */
    private void getLastUse() {
        int lineNum = 0;
        for (Instruction instruction : irList) {
            lineNum++;
            switch (instruction.getKind()) {
                case MOV -> {
                    IRValue from = instruction.getFrom();
                    if (from.isIRVariable()) {
                        LastUse.put(from, lineNum);
                    }
                }
                case ADD, SUB, MUL -> {
                    IRValue lhs = instruction.getLHS();
                    IRValue rhs = instruction.getRHS();
                    if (lhs.isIRVariable()) {
                        LastUse.put(lhs, lineNum);
                    }
                    if (rhs.isIRVariable()) {
                        LastUse.put(rhs, lineNum);
                    }
                }
                case RET -> {
                    IRValue returnValue = instruction.getReturnValue();
                    if (returnValue.isIRVariable()) {
                        LastUse.put(returnValue, lineNum);
                    }
                }
                default -> throw new IllegalStateException("Unexpected instruction kind: " + instruction.getKind());
            }
        }
    }


    /**
     * 分配寄存器
     *
     * @param irValue 变量名
     * @param lineNum 当前变量所在行号
     */
    private void allocateReg(IRValue irValue, int lineNum) {
        // 如果变量是立即数, 则不分配寄存器
        if (irValue.isImmediate()) {
            return;
        }

        // 如果变量已存在, 则不分配寄存器
        if (regvarMap.containsVar(irValue)) {
            return;
        }

        // 如果有空闲寄存器，选择空闲寄存器
        for (String reg : registers) {
            if (!regvarMap.containsReg(reg)) {
                // 更新寄存器-变量映射
                regvarMap.putRegVar(reg, irValue);
                regvarMap.putVarReg(irValue, reg);
                return;
            }
        }

        // 如果没有空闲寄存器，夺取不再使用的变量所占的寄存器
        // 根据LastUse表, 找到某一个不再使用的变量, 直接分配其寄存器
        for (Map.Entry<IRValue, Integer> entry : LastUse.entrySet()) {
            if (entry.getValue() < lineNum && !entry.getKey().equals(irValue)) {
                // 如果该变量的最后一次使用行号大于当前行号, 则不再使用
                // 分配该变量所占的寄存器
                String reg = regvarMap.getReg(entry.getKey());

                // 更新寄存器-变量映射
                regvarMap.removeRegVar(reg);
                regvarMap.removeVarReg(entry.getKey());
                regvarMap.putRegVar(reg, irValue);
                regvarMap.putVarReg(irValue, reg);

                // 更新LastUse表
                LastUse.remove(entry.getKey());

                return;
            }
        }

        // 如果还是没有空闲寄存器, 则选一个最早不再使用的变量, 根据LastUse表, 找到某一个不再使用的变量(非当前变量), 直接分配其寄存器
        int minLineNum = Integer.MAX_VALUE;
        IRValue minIRValue = null;

        for (Map.Entry<IRValue, Integer> entry : LastUse.entrySet()) {
            if (entry.getValue() < minLineNum && !entry.getKey().equals(irValue)) {
                minLineNum = entry.getValue();
                minIRValue = entry.getKey();
            }
        }

        // 分配该变量所占的寄存器
        String reg = regvarMap.getReg(minIRValue);

        // 更新寄存器-变量映射
        regvarMap.removeRegVar(reg);
        regvarMap.removeVarReg(minIRValue);
        regvarMap.putRegVar(reg, irValue);
        regvarMap.putVarReg(irValue, reg);

        // 更新LastUse表
        LastUse.remove(minIRValue);
    }

}

