package fr.adrienbrault.idea.symfony2plugin.dic.intention;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInspection.IntentionAndQuickFixAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SlowOperations;
import com.intellij.util.ThrowableRunnable;
import com.jetbrains.php.lang.psi.elements.FieldReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import fr.adrienbrault.idea.symfony2plugin.completion.IncompletePropertyServiceInjectionContributor;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import icons.SymfonyIcons;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;

public class PhpPropertyArgumentIntention extends IntentionAndQuickFixAction implements Iconable, HighPriorityAction {
    @Override
    public @IntentionName @NotNull String getName() {
        return "Symfony: Add Property Service";
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "Symfony";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @Nullable Editor editor, PsiFile file) {
        if (editor == null) {
            return false;
        }

        FieldReference fieldReference = getElement(editor, file);
        if (fieldReference != null) {
            PhpExpression classReference1 = fieldReference.getClassReference();
            if (classReference1 != null && "this".equals(classReference1.getName())) {
                PhpClass phpClassScope = PsiTreeUtil.getParentOfType(fieldReference, PhpClass.class);
                if (phpClassScope != null && phpClassScope.findFieldByName(((FieldReference) fieldReference).getName(), true) == null) {
                    return ServiceUtil.isPhpClassAService(phpClassScope);
                }
            }
        }

        return false;
    }

    @Override
    public void applyFix(@NotNull Project project, PsiFile file, @Nullable Editor editor) {
        if (editor == null) {
            return;
        }

        FieldReference fieldReference = getElement(editor, file);
        if (fieldReference == null) {
            return;
        }

        String name = fieldReference.getName();
        if (name == null) {
            return;
        }

        List<String> injectionService = IncompletePropertyServiceInjectionContributor.getInjectionService(project, name)
            .stream()
            .map(s -> StringUtils.stripStart(s, "\\"))
            .collect(Collectors.toList());

        JBPopupFactory.getInstance().createPopupChooserBuilder(injectionService)
            .setTitle("Symfony: Property Service Suggestions")
            .setItemChosenCallback(s -> {
                try {
                    SlowOperations.allowSlowOperations((ThrowableRunnable<Throwable>) () -> {
                        PhpClass phpClassScope = PsiTreeUtil.getParentOfType(fieldReference, PhpClass.class);
                        if (phpClassScope == null || !ServiceUtil.isPhpClassAService(phpClassScope)) {
                            return;
                        }

                        try {
                            WriteCommandAction.writeCommandAction(project)
                                .withName("Symfony: Add Property Service")
                                .run((ThrowableRunnable<Throwable>) () -> IncompletePropertyServiceInjectionContributor.buildInject(phpClassScope, fieldReference.getName(), s));
                        } catch (Throwable ignored) {
                        }
                    });
                } catch (Throwable ignored) {
                }
            })
            .createPopup()
            .showInBestPositionFor(editor);
    }

    @Override
    public Icon getIcon(int flags) {
        return SymfonyIcons.Symfony;
    }

    @Nullable
    private static FieldReference getElement(@NotNull Editor editor, @NotNull PsiFile file) {
        CaretModel caretModel = editor.getCaretModel();

        int position = caretModel.getOffset();
        PsiElement elementAt = file.findElementAt(position);
        if (elementAt == null) {
            return null;
        }

        PsiElement parent = elementAt.getParent();
        return parent instanceof FieldReference ? (FieldReference) parent : null;
    }
}
