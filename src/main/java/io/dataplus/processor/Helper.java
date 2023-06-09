package io.dataplus.processor;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

import javax.lang.model.element.Modifier;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 自定义注解处理器工具类.
 *
 * @author Lam Tong
 */
final class Helper {

    private static final Logger logger = Logger.getLogger(Helper.class.getName());


    /**
     * 判断目标类是否存在无参构造方法. 判断条件如下:
     * <ol>
     *     <li>方法名为 {@code <init>}</li>
     *     <li>方法参数个数为 0</li>
     *     <li>目标类中不存在修饰符包含 final 的属性</li>
     * </ol>
     * 三个条件缺一不可.
     *
     * @param jcClass 目标类的 JCClass 实例
     * @return true: 目标类构造方法无任何参数时返回 true, 否则返回 false
     */
    static boolean hasNoArgsConstructor(JCTree.JCClassDecl jcClass) {
        for (JCTree jcTree : jcClass.defs) {
            if (jcTree.getKind().equals(Tree.Kind.VARIABLE)) {
                JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) jcTree;
                Set<Modifier> flags = variableDecl.mods.getFlags();
                if (!flags.contains(Modifier.STATIC) && flags.contains(Modifier.FINAL)) {
                    logger.log(Level.INFO, "[" + jcClass.name.toString() +
                            "] 包含 final 属性 " + variableDecl.name.toString() + ", 无法添加无参构造方法.");
                    return true;
                }
            }
        }
        for (JCTree jcTree : jcClass.defs) {
            if (jcTree.getKind().equals(Tree.Kind.METHOD)) {
                JCTree.JCMethodDecl jcMethodDecl = (JCTree.JCMethodDecl) jcTree;
                if (Constants.CONSTRUCTOR_NAME.equals(jcMethodDecl.name.toString())) {
                    return jcMethodDecl.params.size() == 0;
                }
            }
        }
        return false;
    }

    /**
     * 判断目标类是否存在全参数构造方法. 判断条件如下:
     * <ol>
     *     <li>方法名为 {@code <init>}</li>
     *     <li>方法参数个数等于类实例属性个数, 即非 {@code static} 属性的个数</li>
     *     <li>每个属性的类型与全参构造方法某个参数的类型匹配.</li>
     * </ol>
     * 以上并不能保证全参构造方法能够实例化每个实例属性.
     *
     * @param jcClass 目标类的 JCClass 实例
     * @return true: 目标类的构造方法数量等于目标类的属性数量且每个属性均赋值, 否则返回 false
     */
    static boolean hasAllArgsConstructor(JCTree.JCClassDecl jcClass, List<JCTree.JCVariableDecl> variableDecls) {
        for (JCTree jcTree : jcClass.defs) {
            if (jcTree.getKind().equals(Tree.Kind.METHOD)) {
                JCTree.JCMethodDecl method = (JCTree.JCMethodDecl) jcTree;
                if (Constants.CONSTRUCTOR_NAME.equals(method.name.toString())
                        && variableDecls.size() == method.params.size()) {
                    for (JCTree.JCVariableDecl variableDecl : variableDecls) {
                        boolean flag = false;
                        for (int i = 0; i < method.params.size(); i++) {
                            JCTree.JCVariableDecl methodParameter = method.params.get(i);
                            if (methodParameter.vartype.type.equals(variableDecl.vartype.type)) {
                                flag = true;
                                break;
                            }
                        }
                        if (!flag) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取目标类的所有实例属性集合, 即非静态属性, 包括 final 类型的属性.
     *
     * @param jcClassDecl 目标类的 JCClass 实例
     * @return 目标类的所有属性集合.
     */
    @SuppressWarnings(value = {"AlibabaLowerCamelCaseVariableNaming"})
    static List<JCTree.JCVariableDecl> getJCVariableDecls(JCTree.JCClassDecl jcClassDecl) {
        ListBuffer<JCTree.JCVariableDecl> variableDecls = new ListBuffer<>();
        for (JCTree jcTree : jcClassDecl.defs) {
            if (isValidField(jcTree)) {
                variableDecls.append((JCTree.JCVariableDecl) jcTree);
            }
        }
        return variableDecls.toList();
    }

    /**
     * 判断目标类是否存在 hello() 实例方法. 判断条件如下:
     * <ol>
     *     <li>方法名为 hello</li>
     *     <li>方法无参</li>
     *     <li>方法返回值为 String 类型</li>
     * </ol>
     * 该方法是 {@code @Hello} 注解对应生成的实例方法. 由于 {@code Java} 中关于方法重载的要求未规定方法返回值可作为重载区分标志,
     * 因此无法通过判断方法返回值来确定是否包含对应的实例方法.
     *
     * @param jcClass 目标类的 JCClass 实例
     * @return true 或者 false
     */
    static boolean hasHelloMethod(JCTree.JCClassDecl jcClass) {
        for (JCTree jcTree : jcClass.defs) {
            if (jcTree.getKind().equals(Tree.Kind.METHOD)) {
                JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) jcTree;
                if (Constants.HELLO_METHOD.equals(methodDecl.name.toString())) {
                    if (methodDecl.params.size() == 0) {
                        String resultType = methodDecl.restype.type.toString();
                        Set<Modifier> flags = methodDecl.mods.getFlags();
                        logger.log(Level.INFO, "[" + jcClass.name.toString() +
                                "] 存在 hello() 方法, 方法返回值类型: " + resultType +
                                ", 方法修饰符: " + flags + ".");
                        return true;
                    }
                    return false;
                }
            }
        }
        return false;
    }


    /**
     * 判断目标类是否存在 toString 实例方法. 判断条件如下:
     * <ol>
     *     <li>方法名为 toString</li>
     *     <li>方法无参</li>
     *     <li>方法返回值类型为 String</li>
     * </ol>
     * 由于 toString() 实例方法是 {@code java.lang.Object} 的方法, 任何类生成 toString 方法时方法签名与返回值类型
     * 都需要与 {@code java.lang.Object#toString()} 方法保持一致. 因此此处可以不判断方法返回值类型.
     *
     * @param jcClass 目标类的 JCClass 实例
     * @return true 或者 false
     */
    static boolean hasToString(JCTree.JCClassDecl jcClass) {
        for (JCTree jcTree : jcClass.defs) {
            if (jcTree.getKind().equals(Tree.Kind.METHOD)) {
                JCTree.JCMethodDecl jcMethodDecl = (JCTree.JCMethodDecl) jcTree;
                if (Constants.TO_STRING.equals(jcMethodDecl.name.toString())) {
                    return jcMethodDecl.params.size() == 0;
                }
            }
        }
        return false;
    }

    /**
     * 克隆根据给定的目标类属性集合.
     *
     * @param treeMaker TreeMaker 实例
     * @param variables 目标类属性集合
     * @return 克隆的属性集合
     */
    static List<JCTree.JCVariableDecl> cloneFromVariables(TreeMaker treeMaker, List<JCTree.JCVariableDecl> variables) {
        ListBuffer<JCTree.JCVariableDecl> variableList = new ListBuffer<>();
        for (JCTree.JCVariableDecl variable : variables) {
            variableList.append(cloneFromVariable(treeMaker, variable));
        }
        return variableList.toList();
    }

    /**
     * 克隆给定的目标类属性.
     *
     * @param maker        TreeMaker 实例
     * @param variableDecl 目标类属性
     * @return 克隆的属性
     */
    static JCTree.JCVariableDecl cloneFromVariable(TreeMaker maker, JCTree.JCVariableDecl variableDecl) {
        return maker.VarDef(
                maker.Modifiers(Flags.PARAMETER),
                variableDecl.name,
                variableDecl.vartype,
                null
        );
    }

    /**
     * 判断给定的目标类中是否存在属性的 {@code Getter} 实例方法. 判断条件:
     * <ol>
     *     <li>方法名为 getXXX</li>
     *     <li>方法无参</li>
     *     <li>方法返回值类型与实例属性类型相等</li>
     * </ol>
     * 无法根据方法返回值来确定是否需要生成 getter 实例方法.
     *
     * @param classDecl    目标类实例
     * @param variableDecl 目标类属性实例
     * @return true 或者 false
     */
    static boolean hasGetterMethod(JCTree.JCClassDecl classDecl, JCTree.JCVariableDecl variableDecl) {
        String className = classDecl.name.toString();
        String getterMethodName = convertFieldNameToGetterMethodName(variableDecl);
        for (JCTree jcTree : classDecl.defs) {
            if (jcTree.getKind().equals(Tree.Kind.METHOD)) {
                JCTree.JCMethodDecl jcMethodDecl = (JCTree.JCMethodDecl) jcTree;
                if (getterMethodName.equals(jcMethodDecl.name.toString())
                        && jcMethodDecl.params.size() == 0) {
                    logger.log(Level.INFO, "[" + className + "] 存在实例方法 " + getterMethodName +
                            ", 预期返回值类型: " + variableDecl.vartype.type.toString() +
                            ", 实际返回值类型: " + jcMethodDecl.restype.type.toString());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断给定的目标类中是否存在属性的 {@code Setter} 实例方法, 判断条件:
     * <ol>
     *     <li>方法名为 setXXX</li>
     *     <li>方法仅有一个参数</li>
     *     <li>方法参数类型与实例属性类型相等</li>
     *     <li>方法返回值类型为 Void(非强制)</li>
     * </ol>
     *
     * @param classDecl    目标类实例
     * @param variableDecl 目标类属性实例
     * @return true 或者 false
     */
    static boolean hasSetterMethod(JCTree.JCClassDecl classDecl, JCTree.JCVariableDecl variableDecl) {
        String className = classDecl.name.toString();
        String setterMethodName = convertFieldNameToSetterMethodName(variableDecl);
        for (JCTree jcTree : classDecl.defs) {
            if (jcTree.getKind().equals(Tree.Kind.METHOD)) {
                JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) jcTree;
                if (setterMethodName.equals(methodDecl.name.toString())
                        && methodDecl.params.size() == 1
                        && methodDecl.params.get(0).vartype.type.equals(variableDecl.vartype.type)) {
                    logger.log(Level.INFO, "[" + className + "] 存在实例方法 " + setterMethodName +
                            ", 预期返回值类型: void, 实际返回值类型: " + methodDecl.restype.type.toString());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 将属性名转换为对应的 {@code Getter} 实例方法名.
     *
     * @param variableDecl 属性名
     * @return Getter 实例方法名
     */
    static String convertFieldNameToGetterMethodName(JCTree.JCVariableDecl variableDecl) {
        String fieldName = variableDecl.name.toString();
        if (isBoolean(variableDecl)) {
            return Constants.IS + StringUtils.capitalize(fieldName);
        }
        return Constants.GET + StringUtils.capitalize(fieldName);
    }

    /**
     * 将属性名转换为对应的 {@code Setter} 实例方法名.
     *
     * @param variableDecl 属性名
     * @return Setter 实例方法名
     */
    static String convertFieldNameToSetterMethodName(JCTree.JCVariableDecl variableDecl) {
        String fieldName = variableDecl.name.toString();
        if (isBoolean(variableDecl)) {
            return Constants.IS + StringUtils.capitalize(fieldName);
        }
        return Constants.SET + StringUtils.capitalize(fieldName);
    }

    /**
     * 判断目标字段是否是合法的类属性, 即实例属性.
     *
     * @param tree 目标类属性的 JCTree 实例
     * @return true: 实例属性, 否则返回 false
     */
    private static boolean isValidField(JCTree tree) {
        if (tree.getKind().equals(Tree.Kind.VARIABLE)) {
            JCTree.JCVariableDecl variableDecl = (JCTree.JCVariableDecl) tree;
            Set<Modifier> flags = variableDecl.mods.getFlags();
            return !flags.contains(Modifier.STATIC);
        }
        return false;
    }

    /**
     * 判断目标变量是否为 boolean 类型变量, 包括原始数据类型与包装数据类型.
     *
     * @param variableDecl 变量实例
     * @return true: 变量为 boolean 类型变量; 否则返回 false
     */
    private static boolean isBoolean(JCTree.JCVariableDecl variableDecl) {
        String typeString = variableDecl.vartype.type.toString();
        return "boolean".equals(typeString) || "java.lang.Boolean".equals(typeString);
    }

}
