package com.javax0.geciapt;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class Processor extends AbstractProcessor {
    private Trees trees;

    @Override
    public void init(ProcessingEnvironment pe) {
        super.init(pe);
        this.trees = Trees.instance(pe);
    }

    private final IdentityHashMap<Tree, Object> processed = new IdentityHashMap<>();

    @Override
    public final boolean process(final Set<? extends TypeElement> annotations,
                                 final RoundEnvironment roundEnv) {
        for (final var element : roundEnv.getRootElements()) {
            final var output = element.toString().replaceAll("\\.", "/") + ".xml";

            FileObject builderFile = null;
            try {
                builderFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT,
                    ""
                    , output);
                try (final OutputStream w = builderFile.openOutputStream(); final PrintWriter out = new PrintWriter(w)) {
                    System.out.println("Processing: " + element.toString());
                    outputTree(out, 0, trees.getPath(element).getLeaf());
                    out.flush();
                    w.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    void outputTree(PrintWriter out, int tab, Tree t) {
        if (processed.containsKey(t)) {
            return;
        }
        processed.put(t, null);
        out.println(" ".repeat(tab) + "<" + t.getKind().name() + " class=\"" + t.getClass().getName() + "\">");
        final Method[] methods = t.getClass().getMethods();
        for (final var method : methods) {
            if (Tree.class.isAssignableFrom(method.getReturnType())) {
                if (method.getParameterCount() == 0) {
                    try {
                        final var subTree = (Tree) method.invoke(t);
                        if (subTree != null) {
                            outputTree(out, tab + 2, subTree);
                        }
                    } catch (Exception e) {
                        out.println(" ".repeat(tab) + "<ERROR>");
                        out.println(" ".repeat(tab) + e.toString());
                        out.println(" ".repeat(tab) + "</ERROR>");
                    }
                }
            }
            if (Name.class.isAssignableFrom(method.getReturnType()) && method.getParameterCount() == 0) {
                try {
                    final var name = (Name) method.invoke(t);
                    if (name != null) {
                        out.println(" ".repeat(tab) + "<NAME name=\"" + name + "\" from=\"" + method.getName() + "\"/>");
                    }
                } catch (Exception e) {
                    out.println(" ".repeat(tab) + "<ERROR>");
                    out.println(" ".repeat(tab) + e.toString());
                    out.println(" ".repeat(tab) + "</ERROR>");
                }
            }
            if (method.getReturnType().equals(List.class)) {
                if (method.getParameterCount() == 0) {
                    try {
                        final var subTrees = (List) method.invoke(t);
                        if (subTrees != null) {
                            for (final var subTree : subTrees) {
                                if (subTree != null && subTree instanceof Tree) {
                                    outputTree(out, tab + 2, (Tree) subTree);
                                }
                            }
                        }
                    } catch (Exception e) {
                        out.println(" ".repeat(tab) + "<ERROR>");
                        out.println(" ".repeat(tab) + e.toString());
                        out.println(" ".repeat(tab) + "</ERROR>");
                    }
                }
            }
        }
        if (t instanceof LiteralTree) {
            final var literal = (LiteralTree) t;
            final Object value = literal.getValue();
            out.println(" ".repeat(tab) + "<![CDATA[" + value.toString() + "]]>");
        }
        out.println(" ".repeat(tab) + "</" + t.getKind().name() + ">");
    }

}
