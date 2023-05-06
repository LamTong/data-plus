package io.dataplus.processor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import io.dataplus.annotation.NoArgsConstructor;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 自定义注解 {@code @NoArgsConstructor} 处理器.
 *
 * @author Lam Tong
 */
@SupportedAnnotationTypes(value = {"io.dataplus.annotation.NoArgsConstructor"})
@SupportedSourceVersion(value = SourceVersion.RELEASE_8)
public class NoArgsConstructorProcessor extends BaseAnnotationProcessor {

    private static final Logger logger = Logger.getLogger(NoArgsConstructorProcessor.class.getName());

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(NoArgsConstructor.class);
        for (Element element : elements) {
            JCTree jcTree = super.trees.getTree(element);
            jcTree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Starts process @NoArgsConstructor with class [" + jcClassDecl.name.toString() + "].");
                    if (!Helper.hasNoArgsConstructor(jcClassDecl)) {
                        jcClassDecl.defs = jcClassDecl.defs.append(buildNoArgsConstructor());
                    }
                    messager.printMessage(Diagnostic.Kind.NOTE, "Ends process @NoArgsConstructor with class [" + jcClassDecl.name.toString() + "].");
                }
            });
        }
        return true;
    }

    private JCTree.JCMethodDecl buildNoArgsConstructor() {
        logger.log(Level.INFO, "构建无参构造方法.");
        JCTree.JCBlock block = super.treeMaker.Block(0, List.nil());
        return super.treeMaker.MethodDef(super.treeMaker.Modifiers(Flags.PUBLIC),
                super.names.fromString(Constants.CONSTRUCTOR_NAME),
                super.treeMaker.TypeIdent(TypeTag.VOID),
                List.nil(),
                List.nil(),
                List.nil(),
                block,
                null);
    }

}