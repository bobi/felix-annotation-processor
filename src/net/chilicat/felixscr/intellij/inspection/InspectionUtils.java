package net.chilicat.felixscr.intellij.inspection;

import com.intellij.openapi.util.Comparing;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dkuffner
 */
public class InspectionUtils {
    public final static String GROUP_NAME = "Felix SCR Annotations";

    @NotNull
    public static Map<String, PsiNameValuePair> toAttributeMap(@NotNull PsiAnnotationParameterList list) {
        final Map<String, PsiNameValuePair> map = new HashMap<String, PsiNameValuePair>();
        for (PsiNameValuePair p : list.getAttributes()) {
            map.put(p.getName(), p);
        }
        return map;
    }

    public static boolean isLookupStrategy(@NotNull Map<String, PsiNameValuePair> map) {
        PsiNameValuePair strategy = map.get("strategy");
        boolean lookup = false;
        if (strategy != null) {
            PsiAnnotationMemberValue value = strategy.getValue();
            if (value != null) {
                String text = value.getText();
                lookup = text.endsWith("LOOKUP");
            }
        }
        return lookup;
    }

    public static boolean doesMethodExist(@NotNull PsiAnnotation annotation, @NotNull PsiNameValuePair p) {
        final PsiClass containingClass = PsiTreeUtil.getParentOfType(annotation, PsiClass.class);
        if (containingClass != null) {
            String value = getStringValue(p);
            if (value != null) {
                final PsiMethod[] method = containingClass.findMethodsByName(value, true);
                return method.length > 0;
            }
        }
        return false;
    }

    /**
     * A String values in a annotation contains the quotes. This method return the value without quotes.
     *
     * @param p the pair.
     * @return the value.
     */
    @Nullable
    public static String getStringValue(@NotNull PsiNameValuePair p) {
        final PsiAnnotationMemberValue value = p.getValue();
        if (value != null) {
            String text = value.getText();
            return text.substring(1, text.length() - 1);
        }
        return null;
    }

    /*
    public static String getStringValue(Map<String, PsiNameValuePair> map, String key) {
        PsiNameValuePair psiNameValuePair = map.get(key);
        if(psiNameValuePair != null) {
            return getStringValue(psiNameValuePair);
        }
        return null;
    } */

    public static boolean isReference(@NotNull PsiAnnotation annotation) {
        final String qualifiedName = annotation.getQualifiedName();
        return Comparing.strEqual(qualifiedName, "org.apache.felix.scr.annotations.Reference");
    }

    public static boolean isService(@NotNull PsiAnnotation annotation) {
        final String qfn = annotation.getQualifiedName();
        return Comparing.strEqual(qfn, "org.apache.felix.scr.annotations.Service");
    }

    /**
     * Resolves all classes stored in the pair.
     * Returns only classes which can be resolved
     *
     * @param pair the pair.
     * @return a list.
     */
    @NotNull
    public static List<PsiClass> getClasses(@NotNull PsiNameValuePair pair) {
        List<PsiClass> classes = new ArrayList<PsiClass>();
        PsiAnnotationMemberValue value = pair.getValue();
        if (value != null) {
            if (value instanceof PsiClassObjectAccessExpression) {

                resolveClassAndAddToList(classes, (PsiClassObjectAccessExpression) value);

            } else {

                PsiElement[] children = value.getChildren();
                for (PsiElement child : children) {
                    if (child instanceof PsiClassObjectAccessExpression) {
                        PsiClassObjectAccessExpression exp = (PsiClassObjectAccessExpression) child;
                        resolveClassAndAddToList(classes, exp);
                    }
                }

            }
        }
        return classes;
    }

    private static void resolveClassAndAddToList(List<PsiClass> classes, PsiClassObjectAccessExpression value) {
        PsiType type = value.getOperand().getType();
        PsiClass psiClass = PsiTypesUtil.getPsiClass(type);

        // If fqn is not available than the class is not fully resolved.
        if (psiClass != null && psiClass.getQualifiedName() != null) {
            classes.add(psiClass);
        }
    }

    @Nullable
    public static PsiClass findClass(@NotNull PsiElement el) {
        return findParent(PsiClass.class, el);
    }

    @Nullable
    public static <T> T findParent(@NotNull Class<T> cls, @NotNull PsiElement el) {
        PsiElement parent = el;
        while ((parent = parent.getParent()) != null) {
            if (cls.isAssignableFrom(parent.getClass())) {
                return cls.cast(parent);
            }
        }
        return null;
    }
}
