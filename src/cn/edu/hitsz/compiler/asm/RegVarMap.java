package cn.edu.hitsz.compiler.asm;
import cn.edu.hitsz.compiler.ir.IRValue;

import java.util.Map;
import java.util.HashMap;


public class RegVarMap {
    // 定义一个双向字典，用于存储变量名和寄存器的对应关系
    private final Map<String, IRValue> regvarMap;
    private final Map<IRValue, String> varregMap;

    public RegVarMap() {
        regvarMap = new HashMap<>();
        varregMap = new HashMap<>();
    }


    // 添加键值对到字典
    public void putRegVar(String reg, IRValue var) {
        regvarMap.put(reg, var);
    }

    public void putVarReg(IRValue var, String reg) {
        varregMap.put(var, reg);
    }


    // 根据键查询字典中是否存在该键
    public boolean containsReg(String reg) {
        return regvarMap.containsKey(reg);
    }

    public boolean containsVar(IRValue var) {
        return varregMap.containsKey(var);
    }

    // 根据键获取值
    public IRValue getVar(String reg) {
        return regvarMap.get(reg);
    }

    public String getReg(IRValue var) {
        return varregMap.get(var);
    }

    // 根据键删除键值对
    public void removeRegVar(String reg) {
        regvarMap.remove(reg);
    }

    public void removeVarReg(IRValue var) {
        varregMap.remove(var);
    }

}
