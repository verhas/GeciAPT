package com.javax0.geciapt;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
            final String output;
            if (element.getKind() == ElementKind.MODULE) {
                output = "module-info.xml";
            } else {
                output = element.toString().replaceAll("\\.", "/") + ".xml";
            }

            try {
                final var fo = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", output);
                try (final OutputStream w = fo.openOutputStream(); final PrintWriter out = new PrintWriter(w)) {
                    System.out.println("Processing: " + element.toString());
                    final var klass = toTag(trees.getPath(element).getLeaf());
                    if (element.getKind() == ElementKind.CLASS) {
                        klass.attribute("package", element.getEnclosingElement());
                        klass.attribute("module", element.getEnclosingElement().getEnclosingElement());
                    }
                    out.print(klass.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    Tag toTag(Tree t) {
        if (processed.containsKey(t)) {
            return null;
        }
        processed.put(t, null);
        final var tagName = t.getKind().name().toLowerCase();
        final Tag tag;
        if (t instanceof LiteralTree) {
            final var literal = (LiteralTree) t;
            final Object value = literal.getValue();
            tag = Tag.xml(tagName, value == null ? null : value.toString());
        } else {
            tag = Tag.xml(tagName);
        }
        addName(tag, t);
        handleSpecialTrees(t, tag);
        final Method[] methods = t.getKind().asInterface().getMethods();
        for (final var method : methods) {
            addEnumTag(tag, method, t);
            addSubTags(tag, method, t);
            addTagList(tag, method, t);
        }
        return tag;
    }

    private void addTagList(Tag tag, Method method, Tree t) {
        if (List.class.isAssignableFrom(method.getReturnType())) {
            if (hasNoArguments(method)) {
                final Tag listTag = Tag.xml(ungetterize(method.getName()));
                try {
                    final var subTrees = (List<?>) method.invoke(t);
                    if (subTrees != null) {
                        for (final var subTree : subTrees) {
                            if (subTree instanceof Tree) {
                                listTag.subtag(toTag((Tree) subTree));
                            }
                        }
                    }
                } catch (Exception e) {
                    addErrorTag(tag, e);
                }
                if (listTag.hasSubTags()) {
                    tag.subtag(listTag);
                }
            }
        }
    }

    private void addSubTags(Tag tag, Method method, Tree t) {
        if (returnsSomeTreeType(method)) {
            if (hasNoArguments(method)) {
                try {
                    final var subTree = (Tree) method.invoke(t);
                    if (subTree != null) {
                        final var subtag = toTag(subTree);
                        subtag.attribute("source", ungetterize(method.getName()));
                        tag.subtag(subtag);
                    }
                } catch (Exception e) {
                    addErrorTag(tag, e);
                }
            }
        }
    }

    private void addEnumTag(Tag tag, Method method, Tree t) {
        if (!method.getName().equals("getKind") && Enum.class.isAssignableFrom(method.getReturnType()) && hasNoArguments(method)) {
            try {
                final var anEnum = method.invoke(t);
                if (anEnum != null) {
                    final var subtag = Tag.xml(ungetterize(method.getName()));
                    subtag.attribute("value", anEnum.toString().toLowerCase());
                    tag.subtag(subtag);
                }
            } catch (Exception e) {
                addErrorTag(tag, e);
            }
        }
    }

    private void handleSpecialTrees(Tree t, Tag tag) {
        switch (t.getKind()) {
            case MODIFIERS:
                tag.attribute("values", ((ModifiersTree) t).getFlags().stream().map(m -> m.toString().toLowerCase()).collect(Collectors.joining(", ")));
                break;
            case PRIMITIVE_TYPE:
                tag.attribute("type", ((PrimitiveTypeTree) t).getPrimitiveTypeKind().toString().toLowerCase());
                break;
            case MEMBER_SELECT:
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                }
                break;
        }
    }

    private void addErrorTag(Tag tag, Exception e) {
        final var sw = new StringWriter();
        final var pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        try {
            sw.close();
        } catch (IOException ex) {
        }
        tag.subtag(Tag.xml("ERROR", sw.toString()));
    }

    private static boolean hasNoArguments(Method method) {
        return method.getParameterCount() == 0;
    }

    private boolean returnsSomeTreeType(Method method) {
        return Tree.class.isAssignableFrom(method.getReturnType());
    }

    private static void addName(Tag tag, Tree t) {
        final Method[] methods = t.getKind().asInterface().getMethods();
        for (final var method : methods) {
            if (returnsName(method) && hasNoArguments(method)) {
                try {
                    final var name = (Name) method.invoke(t);
                    tag.attribute("name", name);
                    tag.attribute("nameSource", ungetterize(method.getName()));
                } catch (Exception e) {
                }
            }
        }
    }

    private static String ungetterize(String getterName) {
        if (getterName.startsWith("get")) {
            getterName = getterName.substring(3);
        }
        if (getterName.length() > 0) {
            getterName = Character.toLowerCase(getterName.charAt(0)) + getterName.substring(1);
        }
        return getterName;
    }

    private static boolean returnsName(Method method) {
        return Name.class.isAssignableFrom(method.getReturnType());
    }

}
